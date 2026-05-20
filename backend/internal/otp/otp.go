package otp

import (
	"context"
	"crypto/rand"
	"fmt"
	"math/big"
	"strconv"
	"time"

	"github.com/redis/go-redis/v9"
)

const (
	otpTTL            = 10 * time.Minute
	rateLimitTTL      = 15 * time.Minute
	maxSendsPerWindow = 3

	otpKeyPrefix    = "ascend:otp:"      // ascend:otp:<email>      → code
	rateLimitPrefix = "ascend:otp:rate:" // ascend:otp:rate:<email> → count
)

type Service struct {
	rdb *redis.Client
}

func NewService(rdb *redis.Client) *Service {
	return &Service{rdb: rdb}
}

// Generate creates a new 6-digit OTP, stores it in Redis, and returns it.
// Returns ErrRateLimited if the user has requested too many codes in the window.
func (s *Service) Generate(ctx context.Context, email string) (string, error) {
	// Rate limit: max maxSendsPerWindow sends per rateLimitTTL window.
	rateKey := rateLimitPrefix + email
	count, err := s.rdb.Incr(ctx, rateKey).Result()
	if err != nil {
		return "", fmt.Errorf("redis incr: %w", err)
	}
	if count == 1 {
		// First send in the window — set TTL.
		s.rdb.Expire(ctx, rateKey, rateLimitTTL)
	}
	if count > maxSendsPerWindow {
		ttl, _ := s.rdb.TTL(ctx, rateKey).Result()
		return "", fmt.Errorf("rate limited: too many OTP requests, try again in %d minutes",
			int(ttl.Minutes())+1)
	}

	// Generate random 6-digit code.
	code, err := generateCode()
	if err != nil {
		return "", fmt.Errorf("generate code: %w", err)
	}

	// Store in Redis with TTL.
	otpKey := otpKeyPrefix + email
	if err := s.rdb.Set(ctx, otpKey, code, otpTTL).Err(); err != nil {
		return "", fmt.Errorf("redis set: %w", err)
	}

	return code, nil
}

// Verify checks the OTP for an email. Returns nil on success.
// The code is deleted immediately after a successful verification (single use).
func (s *Service) Verify(ctx context.Context, email, code string) error {
	otpKey := otpKeyPrefix + email
	stored, err := s.rdb.Get(ctx, otpKey).Result()
	if err == redis.Nil {
		return fmt.Errorf("OTP expired or not found — request a new code")
	}
	if err != nil {
		return fmt.Errorf("redis get: %w", err)
	}

	if stored != code {
		return fmt.Errorf("incorrect OTP")
	}

	// Delete immediately — single use.
	s.rdb.Del(ctx, otpKey)

	// Also clear the rate limit on success.
	s.rdb.Del(ctx, rateLimitPrefix+email)

	return nil
}

// ReadForDev returns the current OTP stored for an email.
// Only use this in development (when SMTP/Resend is not configured).
// Returns "" if not found.
func (s *Service) ReadForDev(ctx context.Context, email string) string {
	val, _ := s.rdb.Get(ctx, otpKeyPrefix+email).Result()
	return val
}

// TTLRemaining returns how many seconds the OTP is still valid for.
func (s *Service) TTLRemaining(ctx context.Context, email string) int {
	ttl, err := s.rdb.TTL(ctx, otpKeyPrefix+email).Result()
	if err != nil || ttl < 0 {
		return 0
	}
	return int(ttl.Seconds())
}

func generateCode() (string, error) {
	max := big.NewInt(1_000_000)
	n, err := rand.Int(rand.Reader, max)
	if err != nil {
		return "", err
	}
	return fmt.Sprintf("%06s", strconv.Itoa(int(n.Int64()))), nil
}
