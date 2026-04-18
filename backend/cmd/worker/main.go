package main

import (
	"context"
	"log/slog"
	"os"
	"os/signal"
	"syscall"

	"github.com/rajvirsingh2/ascend-backend/internal/events"
	"github.com/rajvirsingh2/ascend-backend/internal/store/postgres"
	redisstore "github.com/rajvirsingh2/ascend-backend/internal/store/redis"
	"github.com/rajvirsingh2/ascend-backend/pkg/config"
	"github.com/rajvirsingh2/ascend-backend/pkg/logger"
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

	publisher := events.NewPublisher(rdb)
	workerType := os.Getenv("WORKER_TYPE")
	slog.Info("worker starting", "type", workerType)

	switch workerType {
	case "xp":
		runXPWorker(ctx, rdb, db, publisher)
	default:
		slog.Error("unknown WORKER_TYPE", "value", workerType)
		os.Exit(1)
	}
}

func runXPWorker(ctx context.Context, rdb interface { /* redis.Client */
}, db interface{}, publisher *events.Publisher) {
	// implemented in Step 1.7 below
}
