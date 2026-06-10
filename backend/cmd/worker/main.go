package main

import (
	"context"
	"log/slog"
	"os"
	"os/signal"
	"syscall"

	"ascend-backend/internal/notifications"
	"ascend-backend/internal/store/postgres"
	redisstore "ascend-backend/internal/store/redis"
	"ascend-backend/internal/workers"

	"ascend-backend/pkg/config"
	"ascend-backend/pkg/logger"

	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/redis/go-redis/v9"
)

func main() {
	cfg, err := config.Load()
	if err != nil {
		slog.Error("config load failed", "error", err)
		os.Exit(1)
	}
	logger.Init(cfg.AppEnv)

	ctx, cancel := signal.NotifyContext(
		context.Background(), syscall.SIGINT, syscall.SIGTERM,
	)
	defer cancel()

	db, err := postgres.NewPool(ctx, cfg.DatabaseURL)
	if err != nil {
		slog.Error("db connect failed", "error", err)
		os.Exit(1)
	}
	defer db.Close()

	rdb, err := redisstore.NewClient(cfg.RedisURL)
	if err != nil {
		slog.Error("redis connect failed", "error", err)
		os.Exit(1)
	}
	defer rdb.Close()

	workerType := os.Getenv("WORKER_TYPE")
	slog.Info("worker starting", "type", workerType)

	switch workerType {
	case "xp":
		runXPWorker(ctx, cfg, rdb, db)
	default:
		slog.Error("unknown WORKER_TYPE", "value", workerType)
		os.Exit(1)
	}
}

func runXPWorker(ctx context.Context, cfg *config.Config, rdb *redis.Client, db *pgxpool.Pool) {
	notifier, err := notifications.NewFCMNotifier(ctx, cfg, db)
	if err != nil {
		slog.Warn("FCM notifier unavailable in worker, continuing without push", "error", err)
		notifier = nil
	}

	// Blocks until ctx is cancelled. The same queue may also be drained by the
	// in-process worker inside the API server; BLPOP guarantees each event is
	// consumed exactly once regardless of how many consumers run.
	workers.RunXPWorker(ctx, workers.XPWorkerConfig{
		Redis:    rdb,
		DB:       db,
		Notifier: notifier,
		Config:   cfg,
	})
}
