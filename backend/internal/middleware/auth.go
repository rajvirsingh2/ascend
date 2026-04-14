package middleware

import (
	"context"
	"net/http"
	"strings"

	"github.com/rajvirsingh2/ascend-backend/internal/auth"
	"github.com/rajvirsingh2/ascend-backend/pkg/response"
)

type contextKey string

const UserIDKey contextKey = "userID"

func JWTGuard(jwtSecret string) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			header := r.Header.Get("Authorization")
			if header == "" || !strings.HasPrefix(header, "Bearer ") {
				response.Error(w, http.StatusUnauthorized, "missing or malformed token")
				return
			}

			token := strings.TrimPrefix(header, "Bearer ")
			userID, err := auth.ValidateAccessToken(token, jwtSecret)
			if err != nil {
				response.Error(w, http.StatusUnauthorized, "invalid or expired token")
				return
			}

			ctx := context.WithValue(r.Context(), UserIDKey, userID)
			next.ServeHTTP(w, r.WithContext(ctx))
		})
	}
}

func GetUserID(r *http.Request) string {
	id, _ := r.Context().Value(UserIDKey).(string)
	return id
}
