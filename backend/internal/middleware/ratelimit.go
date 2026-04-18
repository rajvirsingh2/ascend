package middleware

import (
	"context"
	"fmt"
	"net/http"
	"time"

	"ascend-backend/pkg/response"
	"github.com/redis/go-redis/v9"
)

// RateLimit returns middleware that limits requests per IP using a Redis
// sliding window counter. maxRequests allowed per window duration.
func RateLimit(rdb *redis.Client, maxRequests int, window time.Duration) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			ip := r.RemoteAddr
			key := fmt.Sprintf("rate_limit:%s", ip)
			ctx := context.Background()

			count, err := rdb.Incr(ctx, key).Result()
			if err != nil {
				next.ServeHTTP(w, r)
				return
			}

			if count == 1 {
				rdb.Expire(ctx, key, window)
			}

			if count > int64(maxRequests) {
				response.Error(w, http.StatusTooManyRequests, "too many requests")
				return
			}

			next.ServeHTTP(w, r)
		})
	}
}
