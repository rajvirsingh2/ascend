package notifications

import (
	"encoding/json"
	"net/http"

	"ascend-backend/internal/middleware"
	"ascend-backend/internal/models"
	"ascend-backend/internal/store/postgres"

	"github.com/go-chi/chi/v5"
)

type Handler struct {
	store *postgres.NotificationStore
}

func NewHandler(store *postgres.NotificationStore) *Handler {
	return &Handler{store: store}
}

func (h *Handler) Routes() chi.Router {
	r := chi.NewRouter()
	r.Get("/", h.GetNotifications)
	r.Put("/{id}/read", h.MarkRead)
	r.Put("/read-all", h.MarkAllRead)
	r.Delete("/{id}", h.DeleteNotification)
	r.Delete("/clear-all", h.ClearAll)
	return r
}

func (h *Handler) GetNotifications(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)

	notifs, err := h.store.GetByUser(r.Context(), userID)
	if err != nil {
		http.Error(w, "Failed to fetch notifications", http.StatusInternalServerError)
		return
	}
	if notifs == nil {
		notifs = []*models.Notification{}
	}

	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(map[string]interface{}{"data": notifs})
}

func (h *Handler) MarkRead(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	id := chi.URLParam(r, "id")

	if err := h.store.MarkRead(r.Context(), id, userID); err != nil {
		http.Error(w, "Failed to mark read", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusOK)
}

func (h *Handler) MarkAllRead(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)

	if err := h.store.MarkAllRead(r.Context(), userID); err != nil {
		http.Error(w, "Failed to mark all read", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusOK)
}

func (h *Handler) DeleteNotification(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	id := chi.URLParam(r, "id")

	if err := h.store.Delete(r.Context(), id, userID); err != nil {
		http.Error(w, "Failed to delete notification", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusOK)
}

func (h *Handler) ClearAll(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)

	if err := h.store.DeleteAll(r.Context(), userID); err != nil {
		http.Error(w, "Failed to clear notifications", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusOK)
}
