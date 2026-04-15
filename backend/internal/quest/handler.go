package quest

import (
	"net/http"

	"github.com/go-chi/chi/v5"
	"github.com/rajvirsingh2/ascend-backend/internal/middleware"
	"github.com/rajvirsingh2/ascend-backend/internal/models"
	"github.com/rajvirsingh2/ascend-backend/internal/store"
	"github.com/rajvirsingh2/ascend-backend/pkg/response"
)

type Handler struct{ store store.QuestStore }

func NewHandler(s store.QuestStore) *Handler { return &Handler{store: s} }

func (h *Handler) ListActive(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	quests, err := h.store.ListActive(r.Context(), userID)
	if err != nil {
		response.Error(w, http.StatusInternalServerError, "failed to fetch quests")
		return
	}
	if quests == nil {
		quests = []*models.Quest{}
	}
	response.JSON(w, http.StatusOK, quests)
}

func (h *Handler) Complete(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	id := chi.URLParam(r, "id")

	result, err := h.store.Complete(r.Context(), id, userID)
	if err != nil {
		response.Error(w, http.StatusInternalServerError, "failed to complete quest")
		return
	}
	response.JSON(w, http.StatusOK, map[string]any{
		"xp_awarded":  result.XPAwarded,
		"xp_after":    result.XPAfter,
		"level_after": result.LevelAfter,
		"leveled_up":  result.LeveledUp,
	})
}

func (h *Handler) Skip(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	id := chi.URLParam(r, "id")
	if err := h.store.Skip(r.Context(), id, userID); err != nil {
		response.Error(w, http.StatusInternalServerError, "failed to skip quest")
		return
	}
	response.NoContent(w)
}
