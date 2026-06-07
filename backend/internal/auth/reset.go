package auth

import (
	"context"
	"crypto/rand"
	"fmt"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/redis/go-redis/v9"
)

const resetPrefix = "pwd_reset:"
const resetTTL = 15 * time.Minute

func GenerateResetOTP(ctx context.Context, rdb *redis.Client, email string) (string, error) {
	ratioKey := "reset_rate:" + email
	count, _ := rdb.Incr(ctx, ratioKey).Result()
	if count == 1 {
		rdb.Expire(ctx, ratioKey, time.Hour)
	}
	if count > 3 {
		return "", fmt.Errorf("too many reset requests — try again in an hour")
	}

	// generate 6-digit OTP
	b := make([]byte, 3)
	_, _ = rand.Read(b)
	code := int(b[0])<<16 | int(b[1])<<8 | int(b[2])
	codeStr := fmt.Sprintf("%06d", code)

	rdb.Set(ctx, resetPrefix+email, codeStr, resetTTL)
	return codeStr, nil
}

func VerifyResetOTP(ctx context.Context, rdb *redis.Client, email, code string) error {
	stored, err := rdb.Get(ctx, resetPrefix+email).Result()
	if err == redis.Nil {
		return fmt.Errorf("code expired or not found")
	}
	if err != nil || stored != code {
		return fmt.Errorf("invalid code")
	}
	// don't delete yet — needed for the reset step
	return nil
}

func ResetPassword(
	ctx context.Context,
	db *pgxpool.Pool, rdb *redis.Client,
	email, code, newPassword string,
) error {
	if err := VerifyResetOTP(ctx, rdb, email, code); err != nil {
		return err
	}
	if len(newPassword) < 8 {
		return fmt.Errorf("password must be at least 8 characters")
	}

	hash, err := HashPassword(newPassword)
	if err != nil {
		return fmt.Errorf("hashing password: %w", err)
	}

	result, err := db.Exec(ctx,
		`UPDATE users SET password_hash=$1 WHERE email=$2 AND is_active=true`,
		hash, email,
	)
	if err != nil || result.RowsAffected() == 0 {
		return fmt.Errorf("user not found")
	}

	// invalidate OTP and all active sessions
	rdb.Del(ctx, resetPrefix+email)
	rdb.Del(ctx, "reset_rate:"+email)

	return nil
}
