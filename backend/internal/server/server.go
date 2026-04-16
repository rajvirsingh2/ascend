package server

import (
	"fmt"
	"net/http"

	"github.com/go-chi/chi/v5"
	chimiddleware "github.com/go-chi/chi/v5/middleware"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/rajvirsingh2/ascend-backend/internal/ai"
	"github.com/rajvirsingh2/ascend-backend/internal/auth"
	"github.com/rajvirsingh2/ascend-backend/internal/goal"
	"github.com/rajvirsingh2/ascend-backend/internal/habit"
	"github.com/rajvirsingh2/ascend-backend/internal/middleware"
	"github.com/rajvirsingh2/ascend-backend/internal/quest"
	pgstore "github.com/rajvirsingh2/ascend-backend/internal/store/postgres"
	"github.com/rajvirsingh2/ascend-backend/pkg/config"
	"github.com/rajvirsingh2/ascend-backend/pkg/response"
	"github.com/redis/go-redis/v9"
)

type Server struct {
	cfg      *config.Config
	db       *pgxpool.Pool
	rdb      *redis.Client
	aiClient *ai.Client
}

func New(cfg *config.Config, db *pgxpool.Pool, rdb *redis.Client) *Server {
	return &Server{cfg: cfg, db: db, rdb: rdb, aiClient: ai.NewClient(cfg.RAGServiceURL)}
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
			//goals
			goalHandler := goal.NewHandler(pgstore.NewGoalStore(s.db), s.rdb)
			r.Route("/goals", func(r chi.Router) {
				r.Get("/", goalHandler.List)
				r.Post("/", goalHandler.Create)
				r.Patch("/{id}", goalHandler.Update)
				r.Delete("/{id}", goalHandler.Delete)
			})

			// habits
			habitHandler := habit.NewHandler(pgstore.NewHabitStore(s.db, s.rdb))
			r.Route("/habits", func(r chi.Router) {
				r.Get("/", habitHandler.List)
				r.Post("/", habitHandler.Create)
				r.Post("/{id}/complete", habitHandler.Complete)
			})

			// quests
			questHandler := quest.NewHandler(pgstore.NewQuestStore(s.db, s.rdb))
			r.Route("/quests", func(r chi.Router) {
				r.Get("/", questHandler.ListActive)
				r.Post("/{id}/complete", questHandler.Complete)
				r.Post("/{id}/skip", questHandler.Skip)
				generateHandler := quest.NewGenerateHandler(s.db, s.rdb, s.aiClient)
				r.Post("/generate", generateHandler.Generate)
			})
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
