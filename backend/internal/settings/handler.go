package settings

import (
	"encoding/json"
	"net/http"

	"github.com/rajvirsingh2/ascend-backend/internal/keyvault"
	"github.com/rajvirsingh2/ascend-backend/internal/middleware"
	"github.com/rajvirsingh2/ascend-backend/pkg/response"
)

type Handler struct {
	vault *keyvault.Vault
}

func NewHandler(vault *keyvault.Vault) *Handler {
	return &Handler{vault: vault}
}

type saveKeyRequest struct {
	Provider string `json:"provider"` // "openai" | "claude" | "gemini"
	APIKey   string `json:"api_key"`
	Model    string `json:"model"` // optional override e.g. "gpt-4o-mini"
}

func (h *Handler) SaveAPIKey(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)

	var req saveKeyRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		response.Error(w, http.StatusBadRequest, "invalid request body")
		return
	}

	validProviders := map[string]bool{
		"openai": true, "claude": true,
		"gemini": true, "anthropic": true,
	}
	if !validProviders[req.Provider] {
		response.Error(w, http.StatusBadRequest,
			"provider must be one of: openai, claude, gemini")
		return
	}
	if len(req.APIKey) < 20 {
		response.Error(w, http.StatusBadRequest, "api_key appears invalid")
		return
	}

	if err := h.vault.Store(r.Context(), userID, req.Provider, req.Model, req.APIKey); err != nil {
		response.Error(w, http.StatusInternalServerError, "failed to store key securely")
		return
	}

	response.JSON(w, http.StatusOK, map[string]string{
		"message":  "API key stored securely",
		"provider": req.Provider,
	})
}

func (h *Handler) GetKeyStatus(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	hasKey := h.vault.HasKey(r.Context(), userID)
	response.JSON(w, http.StatusOK, map[string]bool{"has_key": hasKey})
}

func (h *Handler) DeleteAPIKey(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	if err := h.vault.Delete(r.Context(), userID); err != nil {
		response.Error(w, http.StatusInternalServerError, "failed to delete key")
		return
	}
	response.NoContent(w)
}
