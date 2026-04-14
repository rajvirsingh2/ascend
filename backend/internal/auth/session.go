package auth

import (
	"context"
	"crypto/sha256"
	"fmt"
	"time"

	"github.com/redis/go-redis/v9"
)

const refreshPrefix = "refresh:"

func hashToken(token string) string {
	sum := sha256.Sum256([]byte(token))
	return fmt.Sprintf("%x", sum)
}

// StoreRefreshToken saves a refresh token → userID mapping in Redis.
func StoreRefreshToken(ctx context.Context, rdb *redis.Client, token, userID string, expiry time.Duration) error {
	key := refreshPrefix + hashToken(token)
	return rdb.Set(ctx, key, userID, expiry).Err()
}

// ValidateRefreshToken returns the userID for the token or an error.
func ValidateRefreshToken(ctx context.Context, rdb *redis.Client, token string) (string, error) {
	key := refreshPrefix + hashToken(token)
	userID, err := rdb.Get(ctx, key).Result()
	if err == redis.Nil {
		return "", fmt.Errorf("refresh token not found or expired")
	}
	return userID, err
}

// RevokeRefreshToken deletes the token from Redis (logout / rotation).
func RevokeRefreshToken(ctx context.Context, rdb *redis.Client, token string) error {
	key := refreshPrefix + hashToken(token)
	return rdb.Del(ctx, key).Err()
}
