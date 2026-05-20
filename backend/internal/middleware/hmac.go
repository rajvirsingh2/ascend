package middleware

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"net/http"
	"strconv"
	"time"

	"ascend-backend/pkg/response"
)

const signatureWindow = 5 * time.Minute

// HMACVerify validates the X-Timestamp and X-Signature headers.
// Only applied to state-mutating endpoints (POST, PATCH, DELETE).
func HMACVerify(secret string) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			// only verify mutating methods
			if r.Method == http.MethodGet || r.Method == http.MethodOptions {
				next.ServeHTTP(w, r)
				return
			}

			tsHeader := r.Header.Get("X-Timestamp")
			sigHeader := r.Header.Get("X-Signature")

			if tsHeader == "" || sigHeader == "" {
				response.Error(w, http.StatusUnauthorized, "missing request signature")
				return
			}

			ts, err := strconv.ParseInt(tsHeader, 10, 64)
			if err != nil {
				response.Error(w, http.StatusUnauthorized, "invalid timestamp")
				return
			}

			// reject requests older than 5 minutes
			age := time.Since(time.Unix(ts, 0))
			if age > signatureWindow || age < -30*time.Second {
				response.Error(w, http.StatusUnauthorized, "request timestamp expired")
				return
			}

			// reconstruct expected signature
			msg := fmt.Sprintf("%d:%s:%s", ts, r.Method, r.URL.Path)
			mac := hmac.New(sha256.New, []byte(secret))
			mac.Write([]byte(msg))
			expected := hex.EncodeToString(mac.Sum(nil))

			if !hmac.Equal([]byte(expected), []byte(sigHeader)) {
				response.Error(w, http.StatusUnauthorized, "invalid request signature")
				return
			}

			next.ServeHTTP(w, r)
		})
	}
}
