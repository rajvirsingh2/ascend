package main

import (
	"context"
	"log"
	"log/slog"
	"net/http"

	"ascend-backend/internal/interests"
	"ascend-backend/internal/mlservice"
	"ascend-backend/internal/notifications"
	"ascend-backend/internal/quest"
	"ascend-backend/internal/server"
	"ascend-backend/internal/store/postgres"
	pgstore "ascend-backend/internal/store/postgres"
	redisstore "ascend-backend/internal/store/redis"
	"ascend-backend/internal/user"
	"ascend-backend/internal/workers"
	"ascend-backend/pkg/config"
	logger "ascend-backend/pkg/logger"
)

func main() {
	cfg, err := config.Load()
	if err != nil {
		log.Fatalf("loading config: %v", err)
	}

	logger.Init(cfg.AppEnv)
	slog.Info("ascend backend starting",
		"env", cfg.AppEnv,
		"port", cfg.AppPort,
	)

	ctx := context.Background()

	db, err := postgres.NewPool(ctx, cfg.DatabaseURL)
	if err != nil {
		log.Fatalf("connecting to postgres: %v", err)
	}
	defer db.Close()

	rdb, err := redisstore.NewClient(cfg.RedisURL)
	if err != nil {
		log.Fatalf("connecting to redis: %v", err)
	}
	defer rdb.Close()

	if err := postgres.RunMigrations(cfg.DatabaseURL); err != nil {
		log.Fatalf("running database migrations: %v", err)
	}

	srv := server.New(cfg, db, rdb)

	fcmNotifier, err := notifications.NewFCMNotifier(ctx, cfg, db)
	if err != nil {
		log.Printf("failed to initialize FCM notifier: %v", err)
	}

	// start background workers
	questStore := pgstore.NewQuestStore(db, rdb)
	go quest.StartExpiryWorker(ctx, questStore)
	go user.PurgeScheduled(ctx, db)

	// Quest Generation Worker
	if cfg.MLServiceURL != "" {
		workerMLClient := mlservice.NewClient(mlservice.Config{
			SpaceURL: cfg.MLServiceURL,
			Redis:    rdb,
			HFToken:  cfg.HFToken,
		})
		interestsStore := interests.NewStore(db)
		go workers.StartQuestGenerationWorker(ctx, workers.QuestGenerationWorkerConfig{
			DB:             db,
			MLClient:       workerMLClient,
			InterestsStore: interestsStore,
		})
	}
	go notifications.RunDailyReminder(ctx, db, fcmNotifier)
	go workers.RunXPWorker(ctx, workers.XPWorkerConfig{
		Redis:    rdb,
		DB:       db,
		Notifier: fcmNotifier,
		Config:   cfg,
	})
	go workers.StartQuestReminderWorker(ctx, workers.QuestReminderWorkerConfig{
		DB:       db,
		Notifier: fcmNotifier,
	})

	log.Printf("starting ascend backend on %s [%s]", srv.Addr(), cfg.AppEnv)
	if err := http.ListenAndServe(srv.Addr(), srv.Routes()); err != nil {
		log.Fatalf("server error: %v", err)
	}
}
