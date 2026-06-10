package realtime

import (
	"net/http"
	"time"
)

// ServeWS returns the handler for GET /api/v1/ws.
//
// userIDFromRequest extracts the authenticated user ID (the route is mounted
// behind the JWT middleware; middleware.GetUserID is passed in to keep this
// package dependency-free and unit-testable).
func ServeWS(hub *Hub, userIDFromRequest func(*http.Request) string) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		userID := userIDFromRequest(r)
		if userID == "" {
			http.Error(w, "unauthorized", http.StatusUnauthorized)
			return
		}

		conn, err := Upgrade(w, r)
		if err != nil {
			return // Upgrade already wrote the error response
		}

		hub.add(userID, conn)
		defer func() {
			hub.remove(userID, conn)
			conn.Close()
		}()

		// Server-side keepalive: detect dead peers and keep intermediary
		// proxies (nginx) from idling the connection out.
		stop := make(chan struct{})
		defer close(stop)
		go func() {
			ticker := time.NewTicker(pingInterval)
			defer ticker.Stop()
			for {
				select {
				case <-stop:
					return
				case <-ticker.C:
					if err := conn.Ping(); err != nil {
						conn.Close() // unblocks ReadLoop
						return
					}
				}
			}
		}()

		conn.ReadLoop() // blocks until close/error
	}
}
