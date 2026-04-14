package server

import (
	"fmt"
	"net/http"

	"github.com/go-chi/chi/v5"
	chimiddleware "github.com/go-chi/chi/v5/middleware"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/rajvirsingh2/ascend-backend/internal/auth"
	"github.com/rajvirsingh2/ascend-backend/internal/middleware"
	"github.com/rajvirsingh2/ascend-backend/pkg/config"
	"github.com/rajvirsingh2/ascend-backend/pkg/response"
	"github.com/redis/go-redis/v9"
)

type Server struct {
	cfg *config.Config
	db  *pgxpool.Pool
	rdb *redis.Client
}

func New(cfg *config.Config, db *pgxpool.Pool, rdb *redis.Client) *Server {
	return &Server{cfg: cfg, db: db, rdb: rdb}
}

func (s *Server) Routes() http.Handler {
	r := chi.NewRouter()

	// global middleware
	r.Use(chimiddleware.RequestID)
	r.Use(chimiddleware.RealIP)
	r.Use(chimiddleware.Logger)
	r.Use(chimiddleware.Recoverer)
	r.Use(middleware.SecurityHeaders)
	r.Use(middleware.CORS(s.cfg.AllowedOrigins))

	// health
	r.Get("/health", func(w http.ResponseWriter, r *http.Request) {
		response.JSON(w, http.StatusOK, map[string]string{"status": "ok"})
	})
	r.Get("/ready", s.readyHandler())

	// API v1
	r.Route("/api/v1", func(r chi.Router) {

		// auth — rate limited, no JWT required
		authHandler := auth.NewHandler(
			s.db, s.rdb,
			s.cfg.JWTSecret,
			s.cfg.JWTExpiryMinutes,
			s.cfg.RefreshExpiryDays,
		)
		authRateLimit := middleware.RateLimit(s.rdb, 10, 15*60*1e9) // 10 req / 15 min

		r.Route("/auth", func(r chi.Router) {
			r.Use(authRateLimit)
			r.Post("/register", authHandler.Register)
			r.Post("/login", authHandler.Login)
			r.Post("/refresh", authHandler.Refresh)
			r.Post("/logout", authHandler.Logout)
		})

		// protected routes — JWT required
		r.Group(func(r chi.Router) {
			r.Use(middleware.JWTGuard(s.cfg.JWTSecret))

			r.Get("/me", s.meHandler())
			// quest, habit, goal routes added in M5
		})
	})

	return r
}

func (s *Server) readyHandler() http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		if err := s.db.Ping(r.Context()); err != nil {
			response.Error(w, http.StatusServiceUnavailable, "database not ready")
			return
		}
		response.JSON(w, http.StatusOK, map[string]string{"status": "ready"})
	}
}

func (s *Server) meHandler() http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		userID := middleware.GetUserID(r)
		var email, username string
		var level, currentXP int
		err := s.db.QueryRow(r.Context(),
			`SELECT email, username, level, current_xp FROM users WHERE id = $1`,
			userID,
		).Scan(&email, &username, &level, &currentXP)
		if err != nil {
			response.Error(w, http.StatusNotFound, "user not found")
			return
		}
		response.JSON(w, http.StatusOK, map[string]any{
			"id":         userID,
			"email":      email,
			"username":   username,
			"level":      level,
			"current_xp": currentXP,
		})
	}
}

func (s *Server) Addr() string {
	return fmt.Sprintf(":%s", s.cfg.AppPort)
}
