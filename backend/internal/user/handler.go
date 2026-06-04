package user

import (
	"context"
	"encoding/json"
	"log"
	"net/http"
	"time"

	"ascend-backend/internal/middleware"
	"ascend-backend/pkg/response"

	"github.com/jackc/pgx/v5/pgxpool"
)

type Handler struct {
	db *pgxpool.Pool
}

func NewHandler(db *pgxpool.Pool) *Handler {
	return &Handler{db: db}
}

func (h *Handler) RequestDeletion(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)

	_, err := h.db.Exec(r.Context(),
		`UPDATE users SET deleted_at = NOW() + INTERVAL '30 days' WHERE id = $1`,
		userID,
	)
	if err != nil {
		response.Error(w, http.StatusInternalServerError, "failed to schedule deletion")
		return
	}
	response.JSON(w, http.StatusOK, map[string]string{"message": "account scheduled for deletion in 30 days"})
}

func (h *Handler) CancelDeletion(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)

	_, err := h.db.Exec(r.Context(),
		`UPDATE users SET deleted_at = NULL WHERE id = $1`,
		userID,
	)
	if err != nil {
		response.Error(w, http.StatusInternalServerError, "failed to cancel deletion")
		return
	}
	response.JSON(w, http.StatusOK, map[string]string{"message": "account deletion cancelled"})
}

func (h *Handler) RegisterFCMToken(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	var req struct {
		Token string `json:"token"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		response.Error(w, http.StatusBadRequest, "invalid request body")
		return
	}
	if req.Token == "" {
		response.Error(w, http.StatusBadRequest, "token required")
		return
	}

	_, err := h.db.Exec(r.Context(),
		`INSERT INTO fcm_tokens (user_id, token, updated_at) VALUES ($1, $2, NOW()) ON CONFLICT (token) DO UPDATE SET user_id = EXCLUDED.user_id, updated_at = NOW()`,
		userID, req.Token,
	)
	if err != nil {
		response.Error(w, http.StatusInternalServerError, "failed to register FCM token")
		return
	}
	response.JSON(w, http.StatusOK, map[string]string{"message": "token registered"})
}

func PurgeScheduled(ctx context.Context, db *pgxpool.Pool) {
	ticker := time.NewTicker(24 * time.Hour)
	defer ticker.Stop()

	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
			res, err := db.Exec(ctx, `DELETE FROM users WHERE deleted_at IS NOT NULL AND deleted_at <= NOW()`)
			if err != nil {
				log.Printf("failed to purge scheduled users: %v", err)
			} else if res.RowsAffected() > 0 {
				log.Printf("purged %d scheduled users", res.RowsAffected())
			}
		}
	}
}
