package config

import (
	"fmt"
	"os"
	"strconv"

	"github.com/joho/godotenv"
)

type Config struct {
	AppEnv  string
	AppPort string

	DatabaseURL string
	RedisURL    string

	OpenAIKey string
	GeminiKey string

	JWTSecret         string
	JWTExpiryMinutes  int
	RefreshExpiryDays int

	AllowedOrigins []string

	MLServiceURL string
	HFToken      string

	MasterEncryptionKey string
	SMTPHost            string
	SMTPPort            string
	SMTPUser            string
	SMTPPassword        string
	EmailFrom           string

	CloudinaryCloudName string
	CloudinaryAPIKey    string
	CloudinaryAPISecret string
	FCMServerKey        string
	FCMCredentialsJSON  string
	FCMProjectID        string
	ResendAPIKey        string
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
		AppEnv:              getEnv("APP_ENV", "development"),
		AppPort:             getEnv("APP_PORT", "8080"),
		DatabaseURL:         requireEnv("DATABASE_URL"),
		RedisURL:            requireEnv("REDIS_URL"),
		OpenAIKey:           getEnv("OPENAI_API_KEY", ""),
		GeminiKey:           getEnv("GEMINI_API_KEY", ""),
		JWTSecret:           requireEnv("JWT_SECRET"),
		JWTExpiryMinutes:    jwtExpiry,
		RefreshExpiryDays:   refreshExpiry,
		AllowedOrigins:      []string{getEnv("ALLOWED_ORIGINS", "http://localhost:3000")},
		MLServiceURL:        getEnv("ML_SERVICE_URL", ""),
		HFToken:             getEnv("HF_TOKEN", ""),
		MasterEncryptionKey: getEnv("MASTER_ENCRYPTION_KEY", ""),
		SMTPHost:            getEnv("SMTP_HOST", ""),
		SMTPPort:            getEnv("SMTP_PORT", "587"),
		SMTPUser:            getEnv("SMTP_USER", ""),
		SMTPPassword:        getEnv("SMTP_PASSWORD", ""),
		EmailFrom:           getEnv("EMAIL_FROM", "noreply@ascend.app"),
		CloudinaryCloudName: getEnv("CLOUDINARY_CLOUD_NAME", ""),
		CloudinaryAPIKey:    getEnv("CLOUDINARY_API_KEY", ""),
		CloudinaryAPISecret: getEnv("CLOUDINARY_API_SECRET", ""),
		FCMServerKey:        getEnv("FCM_SERVER_KEY", ""),
		FCMCredentialsJSON:  getEnv("FCM_CREDENTIALS_JSON", ""),
		FCMProjectID:        getEnv("FCM_PROJECT_ID", ""),
		ResendAPIKey:        getEnv("RESEND_API_KEY", ""),
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
