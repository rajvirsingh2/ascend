package interests

import (
	"encoding/json"
	"net/http"

	"ascend-backend/internal/middleware"
	"ascend-backend/pkg/response"
)

// Handler handles interest-related API endpoints.
type Handler struct {
	store *Store
}

func NewHandler(store *Store) *Handler {
	return &Handler{store: store}
}

// GetCategories returns the full category taxonomy.
// GET /interests/categories  (public — no auth needed)
func (h *Handler) GetCategories(w http.ResponseWriter, r *http.Request) {
	response.JSON(w, http.StatusOK, map[string]interface{}{
		"categories": AllCategories,
	})
}

// SaveInterests saves the user's selected interests.
// POST /interests
func (h *Handler) SaveInterests(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	if userID == "" {
		response.Error(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	type interestInput struct {
		Category    string `json:"category"`
		Subcategory string `json:"subcategory"`
		CustomGoal  string `json:"custom_goal"`
		Priority    int    `json:"priority"`
		Proficiency string `json:"proficiency"`
	}

	var req struct {
		Interests []interestInput `json:"interests"`
	}

	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		response.Error(w, http.StatusBadRequest, "invalid request body")
		return
	}

	if len(req.Interests) == 0 || len(req.Interests) > 15 {
		response.Error(w, http.StatusBadRequest, "interests count must be between 1 and 15")
		return
	}

	// Validate all category IDs.
	validCats := map[string]bool{}
	validSubs := map[string]bool{}
	for _, cat := range AllCategories {
		validCats[cat.ID] = true
		for _, sub := range cat.Subcategories {
			validSubs[cat.ID+"/"+sub.ID] = true
		}
	}

	var interests []UserInterest
	for i, inp := range req.Interests {
		if !validCats[inp.Category] {
			response.Error(w, http.StatusBadRequest, "unknown category: "+inp.Category)
			return
		}
		if inp.Subcategory != "" && !validSubs[inp.Category+"/"+inp.Subcategory] {
			response.Error(w, http.StatusBadRequest, "unknown subcategory: "+inp.Category+"/"+inp.Subcategory)
			return
		}
		if inp.Category == "" {
			response.Error(w, http.StatusBadRequest, "category is required")
			return
		}

		priority := inp.Priority
		if priority < 1 || priority > 3 {
			priority = i + 1
			if priority > 3 {
				priority = 3
			}
		}
		proficiency := inp.Proficiency
		if proficiency == "" {
			proficiency = "Beginner"
		}

		interests = append(interests, UserInterest{
			UserID:      userID,
			Category:    inp.Category,
			Subcategory: inp.Subcategory,
			CustomGoal:  inp.CustomGoal,
			Priority:    priority,
			Proficiency: proficiency,
		})
	}

	if err := h.store.Save(r.Context(), userID, interests); err != nil {
		response.Error(w, http.StatusInternalServerError, "failed to save interests")
		return
	}

	response.JSON(w, http.StatusOK, map[string]interface{}{
		"configured": true,
		"message":    "interests saved",
		"count":      len(interests),
		"interests":  interests,
	})
}

// GetMyInterests returns the current user's configured interests.
// GET /interests
func (h *Handler) GetMyInterests(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	if userID == "" {
		response.Error(w, http.StatusUnauthorized, "unauthorized")
		return
	}

	interests, err := h.store.GetByUser(r.Context(), userID)
	if err != nil {
		response.Error(w, http.StatusInternalServerError, "failed to load interests")
		return
	}

	configured, _ := h.store.IsConfigured(r.Context(), userID)

	response.JSON(w, http.StatusOK, map[string]interface{}{
		"configured": configured,
		"interests":  interests,
	})
}
