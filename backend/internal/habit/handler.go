package habit

import (
	"encoding/json"
	"net/http"

	"github.com/go-chi/chi/v5"
	"github.com/rajvirsingh2/ascend-backend/internal/middleware"
	"github.com/rajvirsingh2/ascend-backend/internal/models"
	"github.com/rajvirsingh2/ascend-backend/internal/store"
	"github.com/rajvirsingh2/ascend-backend/pkg/response"
)

type Handler struct{ store store.HabitStore }

func NewHandler(s store.HabitStore) *Handler { return &Handler{store: s} }

func (h *Handler) Create(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	var habit models.Habit
	if err := json.NewDecoder(r.Body).Decode(&habit); err != nil {
		response.Error(w, http.StatusBadRequest, "invalid request body")
		return
	}
	if habit.Title == "" {
		response.Error(w, http.StatusBadRequest, "title is required")
		return
	}
	if habit.Frequency == "" {
		habit.Frequency = "daily"
	}
	if habit.XPReward == 0 {
		habit.XPReward = 10
	}
	habit.UserID = userID
	if err := h.store.Create(r.Context(), &habit); err != nil {
		response.Error(w, http.StatusInternalServerError, "failed to create habit")
		return
	}
	response.JSON(w, http.StatusCreated, habit)
}

func (h *Handler) List(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	habits, err := h.store.ListByUser(r.Context(), userID)
	if err != nil {
		response.Error(w, http.StatusInternalServerError, "failed to fetch habits")
		return
	}
	if habits == nil {
		habits = []*models.Habit{}
	}
	response.JSON(w, http.StatusOK, habits)
}

func (h *Handler) Complete(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	id := chi.URLParam(r, "id")

	result, err := h.store.Complete(r.Context(), id, userID)
	if err != nil {
		response.Error(w, http.StatusInternalServerError, "failed to complete habit")
		return
	}
	if result == nil {
		// already completed today — idempotent 200
		response.JSON(w, http.StatusOK, map[string]string{
			"message": "already completed today",
		})
		return
	}
	response.JSON(w, http.StatusOK, map[string]any{
		"xp_awarded":  result.XPAwarded,
		"xp_after":    result.XPAfter,
		"level_after": result.LevelAfter,
		"leveled_up":  result.LeveledUp,
	})
}
