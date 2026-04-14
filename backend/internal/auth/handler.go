package auth

import (
	"context"
	"encoding/json"
	"net/http"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/rajvirsingh2/ascend-backend/pkg/response"
	"github.com/redis/go-redis/v9"
)

type Handler struct {
	db                *pgxpool.Pool
	rdb               *redis.Client
	jwtSecret         string
	jwtExpiryMinutes  int
	refreshExpiryDays int
}

func NewHandler(db *pgxpool.Pool, rdb *redis.Client, jwtSecret string, jwtExpiry, refreshExpiry int) *Handler {
	return &Handler{
		db:                db,
		rdb:               rdb,
		jwtSecret:         jwtSecret,
		jwtExpiryMinutes:  jwtExpiry,
		refreshExpiryDays: refreshExpiry,
	}
}

// --- Register ---

type registerRequest struct {
	Email    string `json:"email"`
	Password string `json:"password"`
	Username string `json:"username"`
}

func (h *Handler) Register(w http.ResponseWriter, r *http.Request) {
	var req registerRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		response.Error(w, http.StatusBadRequest, "invalid request body")
		return
	}

	if len(req.Password) < 8 {
		response.Error(w, http.StatusBadRequest, "password must be at least 8 characters")
		return
	}

	hash, err := HashPassword(req.Password)
	if err != nil {
		response.Error(w, http.StatusInternalServerError, "failed to process password")
		return
	}

	userID := uuid.NewString()
	_, err = h.db.Exec(context.Background(),
		`INSERT INTO users (id, email, password_hash, username)
		 VALUES ($1, $2, $3, $4)`,
		userID, req.Email, hash, req.Username,
	)
	if err != nil {
		response.Error(w, http.StatusConflict, "email already registered")
		return
	}

	response.JSON(w, http.StatusCreated, map[string]string{"user_id": userID})
}

// --- Login ---

type loginRequest struct {
	Email    string `json:"email"`
	Password string `json:"password"`
}

func (h *Handler) Login(w http.ResponseWriter, r *http.Request) {
	var req loginRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		response.Error(w, http.StatusBadRequest, "invalid request body")
		return
	}

	var userID, hash string
	err := h.db.QueryRow(context.Background(),
		`SELECT id, password_hash FROM users WHERE email = $1 AND is_active = true`,
		req.Email,
	).Scan(&userID, &hash)
	if err != nil || !CheckPassword(req.Password, hash) {
		response.Error(w, http.StatusUnauthorized, "invalid credentials")
		return
	}

	accessToken, err := GenerateAccessToken(userID, h.jwtSecret, h.jwtExpiryMinutes)
	if err != nil {
		response.Error(w, http.StatusInternalServerError, "failed to generate token")
		return
	}

	refreshToken := GenerateRefreshToken()
	expiry := time.Duration(h.refreshExpiryDays) * 24 * time.Hour
	if err := StoreRefreshToken(r.Context(), h.rdb, refreshToken, userID, expiry); err != nil {
		response.Error(w, http.StatusInternalServerError, "failed to create session")
		return
	}

	http.SetCookie(w, &http.Cookie{
		Name:     "refresh_token",
		Value:    refreshToken,
		HttpOnly: true,
		Secure:   true,
		SameSite: http.SameSiteStrictMode,
		Path:     "/api/v1/auth",
		MaxAge:   h.refreshExpiryDays * 24 * 60 * 60,
	})

	response.JSON(w, http.StatusOK, map[string]string{
		"access_token": accessToken,
		"token_type":   "Bearer",
	})
}

// --- Refresh ---

func (h *Handler) Refresh(w http.ResponseWriter, r *http.Request) {
	cookie, err := r.Cookie("refresh_token")
	if err != nil {
		response.Error(w, http.StatusUnauthorized, "missing refresh token")
		return
	}

	userID, err := ValidateRefreshToken(r.Context(), h.rdb, cookie.Value)
	if err != nil {
		response.Error(w, http.StatusUnauthorized, "invalid or expired refresh token")
		return
	}

	// rotate: revoke old, issue new
	_ = RevokeRefreshToken(r.Context(), h.rdb, cookie.Value)

	accessToken, _ := GenerateAccessToken(userID, h.jwtSecret, h.jwtExpiryMinutes)
	newRefresh := GenerateRefreshToken()
	expiry := time.Duration(h.refreshExpiryDays) * 24 * time.Hour
	_ = StoreRefreshToken(r.Context(), h.rdb, newRefresh, userID, expiry)

	http.SetCookie(w, &http.Cookie{
		Name:     "refresh_token",
		Value:    newRefresh,
		HttpOnly: true,
		Secure:   true,
		SameSite: http.SameSiteStrictMode,
		Path:     "/api/v1/auth",
		MaxAge:   h.refreshExpiryDays * 24 * 60 * 60,
	})

	response.JSON(w, http.StatusOK, map[string]string{
		"access_token": accessToken,
		"token_type":   "Bearer",
	})
}

// --- Logout ---

func (h *Handler) Logout(w http.ResponseWriter, r *http.Request) {
	cookie, err := r.Cookie("refresh_token")
	if err == nil {
		_ = RevokeRefreshToken(r.Context(), h.rdb, cookie.Value)
	}

	http.SetCookie(w, &http.Cookie{
		Name:     "refresh_token",
		Value:    "",
		HttpOnly: true,
		Secure:   true,
		SameSite: http.SameSiteStrictMode,
		Path:     "/api/v1/auth",
		MaxAge:   -1,
	})

	response.NoContent(w)
}
