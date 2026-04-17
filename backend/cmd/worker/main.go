package main

import (
	"context"
	"log/slog"
	"os"
	"os/signal"
	"syscall"

	"github.com/rajvirsingh2/ascend-backend/internal/kafka"
	"github.com/rajvirsingh2/ascend-backend/internal/store/postgres"
	"github.com/rajvirsingh2/ascend-backend/internal/workers"
	"github.com/rajvirsingh2/ascend-backend/pkg/config"
	"github.com/rajvirsingh2/ascend-backend/pkg/logger"
)

func main() {
	cfg, _ := config.Load()
	logger.Init(cfg.AppEnv)

	ctx, cancel := signal.NotifyContext(
		context.Background(), syscall.SIGINT, syscall.SIGTERM,
	)
	defer cancel()

	db, _ := postgres.NewPool(ctx, cfg.DatabaseURL)
	defer db.Close()

	producer := kafka.NewProducer(cfg.KafkaBrokers)
	defer producer.Close()

	workerType := os.Getenv("WORKER_TYPE")
	slog.Info("starting worker", "type", workerType)

	switch workerType {
	case "xp":
		w := workers.NewXPWorker(cfg.KafkaBrokers, db, producer)
		w.Run(ctx)
	case "outbox":
		kafka.OutboxReplayer(ctx, db, producer)
	default:
		slog.Error("unknown WORKER_TYPE", "type", workerType)
		os.Exit(1)
	}
}
