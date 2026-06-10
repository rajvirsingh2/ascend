package middleware

import (
	"context"
	"fmt"
	"net"
	"net/http"
	"strings"
	"time"

	"ascend-backend/pkg/response"

	"github.com/redis/go-redis/v9"
)

// RateLimit returns middleware that limits requests per IP using a Redis
// sliding window counter. maxRequests allowed per window duration.
func RateLimit(rdb *redis.Client, maxRequests int, window time.Duration) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			ip := r.Header.Get("X-Real-IP")
			if ip == "" {
				ip = r.Header.Get("X-Forwarded-For")
			}
			if ip == "" {
				ip = r.RemoteAddr
				if strings.Contains(ip, ":") {
					// Split host and port safely
					if host, _, err := net.SplitHostPort(ip); err == nil {
						ip = host
					}
				}
			} else {
				// X-Forwarded-For can contain multiple IPs, take the first one
				ips := strings.Split(ip, ",")
				ip = strings.TrimSpace(ips[0])
			}

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

// UserRateLimit returns middleware that limits requests per authenticated user
func UserRateLimit(rdb *redis.Client, maxRequests int, window time.Duration, errMsg string) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			userID := GetUserID(r)
			if userID == "" {
				next.ServeHTTP(w, r)
				return
			}

			key := fmt.Sprintf("rate_limit:user:%s:%s", r.URL.Path, userID)
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
				response.Error(w, http.StatusTooManyRequests, errMsg)
				return
			}

			next.ServeHTTP(w, r)
		})
	}
}
