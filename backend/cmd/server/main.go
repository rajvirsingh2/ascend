package main

import (
	"context"
	"log"
	"net/http"

	"log/slog"

	"ascend-backend/internal/quest"
	"ascend-backend/internal/server"
	"ascend-backend/internal/store/postgres"
	pgstore "ascend-backend/internal/store/postgres"
	redisstore "ascend-backend/internal/store/redis"

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

	// start background workers
	questStore := pgstore.NewQuestStore(db, rdb)
	go quest.StartExpiryWorker(ctx, questStore)
	log.Printf("starting ascend backend on %s [%s]", srv.Addr(), cfg.AppEnv)
	if err := http.ListenAndServe(srv.Addr(), srv.Routes()); err != nil {
		log.Fatalf("server error: %v", err)
	}
}
