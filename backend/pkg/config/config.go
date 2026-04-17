package config

import (
	"fmt"
	"os"
	"strconv"
	"strings"

	"github.com/joho/godotenv"
)

type Config struct {
	AppEnv  string
	AppPort string

	DatabaseURL string

	RedisURL string

	KafkaBrokers []string

	JWTSecret         string
	JWTExpiryMinutes  int
	RefreshExpiryDays int

	AllowedOrigins []string

	RAGServiceURL string
}

func Load() (*Config, error) {
	// only load .env file in development — in production env vars are injected
	if os.Getenv("APP_ENV") != "production" {
		_ = godotenv.Load("../.env")
	}

	jwtExpiry, err := strconv.Atoi(getEnv("JWT_EXPIRY_MINUTES", "15"))
	if err != nil {
		return nil, fmt.Errorf("invalid JWT_EXPIRY_MINUTES: %w", err)
	}

	refreshExpiry, err := strconv.Atoi(getEnv("REFRESH_TOKEN_EXPIRY_DAYS", "7"))
	if err != nil {
		return nil, fmt.Errorf("invalid REFRESH_TOKEN_EXPIRY_DAYS: %w", err)
	}

	cfg := &Config{
		AppEnv:            getEnv("APP_ENV", "development"),
		AppPort:           getEnv("APP_PORT", "8080"),
		DatabaseURL:       requireEnv("DATABASE_URL"),
		RedisURL:          requireEnv("REDIS_URL"),
		KafkaBrokers:      strings.Split(getEnv("KAFKA_BROKERS", "localhost:9092"), ","),
		JWTSecret:         requireEnv("JWT_SECRET"),
		JWTExpiryMinutes:  jwtExpiry,
		RefreshExpiryDays: refreshExpiry,
		AllowedOrigins:    []string{getEnv("ALLOWED_ORIGINS", "http://localhost:3000")},
		RAGServiceURL:     getEnv("RAG_SERVICE_URL", "http://localhost:8001"),
	}

	return cfg, nil
}

func requireEnv(key string) string {
	v := os.Getenv(key)
	if v == "" {
		panic(fmt.Sprintf("required environment variable %s is not set", key))
	}
	return v
}

func getEnv(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}
