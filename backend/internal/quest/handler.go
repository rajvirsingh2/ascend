package quest

import (
	"errors"
	"log"
	"net/http"

	"ascend-backend/internal/middleware"
	"ascend-backend/internal/models"
	"ascend-backend/internal/store"

	"ascend-backend/pkg/response"

	chi "github.com/go-chi/chi/v5"
	"github.com/jackc/pgx/v5"
)

type Handler struct {
	store   store.QuestStore
	tracker *InteractionTracker
}

func NewHandler(s store.QuestStore, t *InteractionTracker) *Handler {
	return &Handler{store: s, tracker: t}
}

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

func (h *Handler) ListHistory(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	quests, err := h.store.ListHistory(r.Context(), userID)
	if err != nil {
		response.Error(w, http.StatusInternalServerError, "failed to fetch quest history")
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
		log.Printf("[quests] failed to complete quest (id=%s, user=%s): %v", id, userID, err)
		if errors.Is(err, pgx.ErrNoRows) {
			response.Error(w, http.StatusNotFound, "quest not found")
			return
		}
		response.Error(w, http.StatusInternalServerError, "failed to complete quest")
		return
	}

	// log outcome for DPO tracking
	if h.tracker != nil {
		_ = h.tracker.LogOutcome(r.Context(), id, "completed")
	}

	response.JSON(w, http.StatusOK, map[string]any{
		"xp_awarded":  result.XPAwarded,
		"xp_after":    result.XPAfter,
		"level_after": result.LevelAfter,
		"leveled_up":  result.LeveledUp,
		"stat_deltas": result.StatDeltas,
	})
}

func (h *Handler) Skip(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	id := chi.URLParam(r, "id")

	result, err := h.store.Skip(r.Context(), id, userID)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			response.Error(w, http.StatusNotFound, "quest not found")
			return
		}
		response.Error(w, http.StatusInternalServerError, "failed to skip quest")
		return
	}

	// log outcome for DPO tracking
	if h.tracker != nil {
		_ = h.tracker.LogOutcome(r.Context(), id, "skipped")
	}

	response.JSON(w, http.StatusOK, map[string]any{
		"hp_damage":   result.HPDamage,
		"hp_after":    result.HPAfter,
		"skips_used":  result.SkipsUsed,
		"died":        result.Died,
		"stat_deltas": result.StatDeltas,
	})
}

func (h *Handler) GetHeatmap(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	heatmap, err := h.store.GetHeatmap(r.Context(), userID)
	if err != nil {
		response.Error(w, http.StatusInternalServerError, "failed to fetch heatmap")
		return
	}
	response.JSON(w, http.StatusOK, heatmap)
}
