package middleware

import (
	"net/http"
	"time"

	"ascend-backend/internal/metrics"

	"github.com/go-chi/chi/v5"
	chimiddleware "github.com/go-chi/chi/v5/middleware"
)

// Metrics records request count and latency per chi route pattern, so
// /quests/{id}/complete is one series rather than one per quest ID.
func Metrics(m *metrics.HTTPMetrics) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			ww := chimiddleware.NewWrapResponseWriter(w, r.ProtoMajor)
			start := time.Now()
			next.ServeHTTP(ww, r)

			pattern := ""
			if rctx := chi.RouteContext(r.Context()); rctx != nil {
				pattern = rctx.RoutePattern()
			}
			m.Observe(r.Method, pattern, ww.Status(), time.Since(start).Seconds())
		})
	}
}
