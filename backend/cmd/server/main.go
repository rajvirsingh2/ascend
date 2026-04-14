package main

import (
	"context"
	"log"
	"net/http"

	"github.com/rajvirsingh2/ascend-backend/internal/server"
	"github.com/rajvirsingh2/ascend-backend/internal/store/postgres"
	redisstore "github.com/rajvirsingh2/ascend-backend/internal/store/redis"
	"github.com/rajvirsingh2/ascend-backend/pkg/config"
)

func main() {
	cfg, err := config.Load()
	if err != nil {
		log.Fatalf("loading config: %v", err)
	}

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

	srv := server.New(cfg, db, rdb)

	log.Printf("starting ascend backend on %s [%s]", srv.Addr(), cfg.AppEnv)
	if err := http.ListenAndServe(srv.Addr(), srv.Routes()); err != nil {
		log.Fatalf("server error: %v", err)
	}
}
