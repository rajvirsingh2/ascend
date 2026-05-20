package auth

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"time"

	"ascend-backend/internal/email"
	"ascend-backend/internal/otp"
	"ascend-backend/pkg/response"
	"ascend-backend/pkg/validator"
	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/redis/go-redis/v9"
)

type Handler struct {
	db                *pgxpool.Pool
	rdb               *redis.Client
	jwtSecret         string
	jwtExpiryMinutes  int
	refreshExpiryDays int
	emailSender       *email.Sender
	otpService        *otp.Service
}

func NewHandler(db *pgxpool.Pool, rdb *redis.Client, jwtSecret string, jwtExpiry, refreshExpiry int, emailSender *email.Sender) *Handler {
	return &Handler{
		db:                db,
		rdb:               rdb,
		jwtSecret:         jwtSecret,
		jwtExpiryMinutes:  jwtExpiry,
		refreshExpiryDays: refreshExpiry,
		emailSender:       emailSender,
		otpService:        otp.NewService(rdb),
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

	if err := validator.Email(req.Email); err != nil {
		response.Error(w, http.StatusBadRequest, err.Error())
		return
	}
	if err := validator.Username(req.Username); err != nil {
		response.Error(w, http.StatusBadRequest, err.Error())
		return
	}
	if err := validator.Password(req.Password); err != nil {
		response.Error(w, http.StatusBadRequest, err.Error())
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

	// Generate and send OTP (Non-fatal if OTP fails, user is still created)
	code, err := h.otpService.Generate(r.Context(), req.Email)
	if err != nil {
		log.Printf("[register] OTP generate error: %v", err)
	} else if h.emailSender != nil {
		go h.emailSender.SendOTP(context.Background(), req.Email, req.Username, code)
	}

	response.JSON(w, http.StatusCreated, map[string]string{
		"message": "verification code sent to your email",
		"user_id": userID,
	})
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
	var emailVerified bool
	err := h.db.QueryRow(context.Background(),
		`SELECT id, password_hash, email_verified FROM users WHERE email = $1 AND is_active = true AND deleted_at IS NULL`,
		req.Email,
	).Scan(&userID, &hash, &emailVerified)

	lockKey := "lock:" + req.Email
	locked, _ := h.rdb.Get(r.Context(), lockKey).Result()
	if locked == "1" {
		response.Error(w, http.StatusTooManyRequests, "account temporarily locked - try again in 30 minutes")
		return
	}

	failKey := "fails:" + req.Email
	if err != nil || !CheckPassword(req.Password, hash) {
		count, _ := h.rdb.Incr(r.Context(), failKey).Result()
		h.rdb.Expire(r.Context(), failKey, 15*time.Minute)
		if count >= 5 {
			h.rdb.Set(r.Context(), lockKey, "1", 30*time.Minute)
			h.rdb.Del(r.Context(), failKey)
			response.Error(w, http.StatusTooManyRequests,
				"too many failed attempts — account locked for 30 minutes")
			return
		}
		remaining := 5 - count
		response.Error(w, http.StatusUnauthorized,
			fmt.Sprintf("invalid credentials — %d attempts remaining", remaining))
		return
	}
	h.rdb.Del(r.Context(), failKey)

	// Block unverified accounts
	if !emailVerified {
		response.Error(w, http.StatusForbidden, "email not verified — check your inbox for the OTP")
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

// --- Verification & OTP ---

func (h *Handler) VerifyEmail(w http.ResponseWriter, r *http.Request) {
	var req struct {
		Email string `json:"email"`
		OTP   string `json:"otp"` // Renamed from Code to match your initial request body structure
	}

	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		response.Error(w, http.StatusBadRequest, "invalid request body")
		return
	}

	if err := h.otpService.Verify(r.Context(), req.Email, req.OTP); err != nil {
		response.Error(w, http.StatusUnauthorized, "invalid or expired OTP")
		return
	}

	var username string
	// Mark email as verified and return the username for the welcome email
	err := h.db.QueryRow(r.Context(),
		`UPDATE users SET email_verified=true, email_verified_at=NOW()
         WHERE email=$1 RETURNING username`,
		req.Email,
	).Scan(&username)

	if err != nil {
		response.Error(w, http.StatusInternalServerError, "verification update failed")
		return
	}

	// Send welcome email (non-blocking)
	if h.emailSender != nil {
		go func() {
			_ = h.emailSender.SendWelcome(r.Context(), req.Email, username)
		}()
	}

	response.JSON(w, http.StatusOK, map[string]string{
		"message": "email verified — you may now log in",
	})
}

func (h *Handler) SendOTP(w http.ResponseWriter, r *http.Request) {
	var req struct {
		Email string `json:"email"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		response.Error(w, http.StatusBadRequest, "invalid request body")
		return
	}

	var username string
	err := h.db.QueryRow(r.Context(),
		`SELECT username FROM users WHERE email=$1 AND email_verified=false`,
		req.Email,
	).Scan(&username)
	if err != nil {
		// Don't reveal whether the email exists or is already verified.
		response.JSON(w, http.StatusOK, map[string]string{
			"message": "if that email exists and is unverified, a new code has been sent",
		})
		return
	}

	code, err := h.otpService.Generate(r.Context(), req.Email)
	if err != nil {
		response.Error(w, http.StatusTooManyRequests, err.Error())
		return
	}

	if h.emailSender != nil {
		go func() {
			if err := h.emailSender.SendOTP(context.Background(), req.Email, username, code); err != nil {
				log.Printf("[send-otp] email error: %v", err)
			}
		}()
	}

	// Modified to include standard expiration time in response payload
	response.JSON(w, http.StatusOK, map[string]interface{}{
		"message":            "OTP sent",
		"expires_in_seconds": 600,
	})
}

// DevReadOTP returns the current OTP from Redis.
// ONLY route this in development mode — never expose in production.
// GET /auth/dev/otp/{email}
func (h *Handler) DevReadOTP(w http.ResponseWriter, r *http.Request) {
	// Assuming Go 1.22+ routing (e.g., r.PathValue).
	// If using an older version or chi/gorilla, adjust the path extraction accordingly.
	email := r.PathValue("email")
	if email == "" {
		email = r.URL.Query().Get("email")
	}

	// Assuming otp.ReadForDev is structured similarly to otp.Generate/Verify in your package
	code := h.otpService.ReadForDev(r.Context(), email)
	ttl := h.otpService.TTLRemaining(r.Context(), email)
	if code == "" {
		response.Error(w, http.StatusNotFound, "no OTP found for this email")
		return
	}

	response.JSON(w, http.StatusOK, map[string]interface{}{
		"code":               code,
		"expires_in_seconds": ttl,
	})
}

// --- Forgot Password ---

func (h *Handler) ForgotPassword(w http.ResponseWriter, r *http.Request) {
	var req struct {
		Email string `json:"email"`
	}

	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		response.Error(w, http.StatusBadRequest, "invalid request body")
		return
	}

	var username string
	err := h.db.QueryRow(r.Context(),
		`SELECT username FROM users WHERE email=$1 AND is_active=true AND deleted_at IS NULL`,
		req.Email,
	).Scan(&username)
	if err != nil {
		response.JSON(w, http.StatusOK, map[string]string{
			"message": "if that email exists, a reset code has been sent",
		})
		return
	}

	code, err := h.otpService.Generate(r.Context(), req.Email)
	if err != nil {
		response.Error(w, http.StatusTooManyRequests, err.Error())
		return
	}

	if h.emailSender != nil {
		go h.emailSender.SendPasswordReset(context.Background(), req.Email, username, code)
	}

	response.JSON(w, http.StatusOK, map[string]string{
		"message": "if that email exists, a reset code has been sent",
	})
}

func (h *Handler) ResetPassword(w http.ResponseWriter, r *http.Request) {
	var req struct {
		Email    string `json:"email"`
		OTP      string `json:"otp"`
		Password string `json:"new_password"`
	}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		response.Error(w, http.StatusBadRequest, "invalid request body")
		return
	}

	if err := ResetPassword(r.Context(), h.db, h.rdb, req.Email, req.OTP, req.Password); err != nil {
		response.Error(w, http.StatusBadRequest, err.Error())
		return
	}

	response.JSON(w, http.StatusOK, map[string]string{
		"message": "password updated — please log in",
	})
}
