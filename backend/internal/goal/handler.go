package goal

import (
	"encoding/json"
	"net/http"

	"github.com/go-chi/chi/v5"
	"github.com/rajvirsingh2/ascend-backend/internal/middleware"
	"github.com/rajvirsingh2/ascend-backend/internal/models"
	"github.com/rajvirsingh2/ascend-backend/internal/store"
	"github.com/rajvirsingh2/ascend-backend/pkg/response"
)

type Handler struct{ store store.GoalStore }

func NewHandler(s store.GoalStore) *Handler { return &Handler{store: s} }

func (h *Handler) Create(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	var g models.Goal
	if err := json.NewDecoder(r.Body).Decode(&g); err != nil {
		response.Error(w, http.StatusBadRequest, "invalid request body")
		return
	}
	if g.Title == "" || g.SkillArea == "" {
		response.Error(w, http.StatusBadRequest, "title and skill_area are required")
		return
	}
	g.UserID = userID
	if err := h.store.Create(r.Context(), &g); err != nil {
		response.Error(w, http.StatusInternalServerError, "failed to create goal")
		return
	}
	response.JSON(w, http.StatusCreated, g)
}

func (h *Handler) List(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	goals, err := h.store.ListByUser(r.Context(), userID)
	if err != nil {
		response.Error(w, http.StatusInternalServerError, "failed to fetch goals")
		return
	}
	if goals == nil {
		goals = []*models.Goal{}
	}
	response.JSON(w, http.StatusOK, goals)
}

func (h *Handler) Update(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	id := chi.URLParam(r, "id")

	existing, err := h.store.GetByID(r.Context(), id, userID)
	if err != nil {
		response.Error(w, http.StatusNotFound, "goal not found")
		return
	}

	if err := json.NewDecoder(r.Body).Decode(existing); err != nil {
		response.Error(w, http.StatusBadRequest, "invalid request body")
		return
	}
	existing.ID = id
	existing.UserID = userID

	if err := h.store.Update(r.Context(), existing); err != nil {
		response.Error(w, http.StatusInternalServerError, "failed to update goal")
		return
	}
	response.JSON(w, http.StatusOK, existing)
}

func (h *Handler) Delete(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	id := chi.URLParam(r, "id")
	if err := h.store.Delete(r.Context(), id, userID); err != nil {
		response.Error(w, http.StatusInternalServerError, "failed to delete goal")
		return
	}
	response.NoContent(w)
}
