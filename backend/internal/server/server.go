package server

import (
	"context"
	"fmt"
	"log/slog"
	"net/http"
	"strings"
	"time"

	"ascend-backend/internal/achievements"
	"ascend-backend/internal/auth"
	"ascend-backend/internal/email"
	"ascend-backend/internal/events"
	"ascend-backend/internal/game"
	"ascend-backend/internal/goal"
	"ascend-backend/internal/habit"
	"ascend-backend/internal/interests"
	"ascend-backend/internal/metrics"
	"ascend-backend/internal/middleware"
	"ascend-backend/internal/mlservice"
	"ascend-backend/internal/notifications"
	"ascend-backend/internal/physique"
	"ascend-backend/internal/quest"
	"ascend-backend/internal/realtime"
	pgstore "ascend-backend/internal/store/postgres"
	"ascend-backend/internal/user"

	"ascend-backend/pkg/config"
	"ascend-backend/pkg/response"

	"github.com/go-chi/chi/v5"
	chimiddleware "github.com/go-chi/chi/v5/middleware"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/redis/go-redis/v9"
)

type Server struct {
	cfg      *config.Config
	db       *pgxpool.Pool
	rdb      *redis.Client
	mlClient *mlservice.Client
	pub      *events.Publisher
	hub      *realtime.Hub
	metrics  *metrics.HTTPMetrics
	profiles *pgstore.UserProfileStore
}

func New(cfg *config.Config, db *pgxpool.Pool, rdb *redis.Client) *Server {
	var mlClient *mlservice.Client
	if cfg.MLServiceURL != "" {
		mlClient = mlservice.NewClient(mlservice.Config{
			SpaceURL: cfg.MLServiceURL,
			Redis:    rdb,
			HFToken:  cfg.HFToken,
		})
		slog.Info("ML model client initialised", "url", cfg.MLServiceURL)
	}

	s := &Server{
		cfg:      cfg,
		db:       db,
		rdb:      rdb,
		mlClient: mlClient,
		hub:      realtime.NewHub(),
		metrics:  metrics.New(),
		profiles: pgstore.NewUserProfileStore(db),
	}
	s.metrics.RegisterGauge("ascend_ws_connections", func() float64 {
		return float64(s.hub.ConnCount())
	})
	go s.runRealtimeBridge(context.Background())
	return s
}

// runRealtimeBridge relays Redis Pub/Sub realtime events (published by the
// XP worker, which may live in another process) to local WebSocket clients.
func (s *Server) runRealtimeBridge(ctx context.Context) {
	for {
		sub := s.rdb.PSubscribe(ctx, realtime.ChannelPrefix+"*")
		for msg := range sub.Channel() {
			userID := strings.TrimPrefix(msg.Channel, realtime.ChannelPrefix)
			s.hub.SendToUser(userID, []byte(msg.Payload))
		}
		// Channel closed (redis hiccup) — back off and resubscribe.
		_ = sub.Close()
		select {
		case <-ctx.Done():
			return
		case <-time.After(2 * time.Second):
			slog.Warn("realtime bridge resubscribing to redis pub/sub")
		}
	}
}

func (s *Server) Routes() http.Handler {
	r := chi.NewRouter()

	// global middleware
	r.Use(chimiddleware.RequestID)
	r.Use(chimiddleware.RealIP)
	r.Use(middleware.RequestLogger)
	r.Use(middleware.Metrics(s.metrics))
	r.Use(chimiddleware.Recoverer)
	r.Use(middleware.SecurityHeaders)
	r.Use(middleware.CORS(s.cfg.AllowedOrigins))

	// health
	r.Get("/health", func(w http.ResponseWriter, r *http.Request) {
		response.JSON(w, http.StatusOK, map[string]string{"status": "ok"})
	})
	r.Get("/ready", s.readyHandler())

	// Prometheus scrape target. Deliberately not proxied by nginx — reachable
	// only from inside the docker network / localhost.
	r.Handle("/metrics", s.metrics.Handler())

	// API v1
	r.Route("/api/v1", func(r chi.Router) {

		// auth — rate limited, no JWT required
		emailSender := email.NewSender(s.cfg)
		authHandler := auth.NewHandler(
			s.db, s.rdb,
			s.cfg.JWTSecret,
			s.cfg.JWTExpiryMinutes,
			s.cfg.RefreshExpiryDays,
			emailSender,
		)
		authRateLimit := middleware.RateLimit(s.rdb, 10, 15*60*1e9) // 10 req / 15 min

		r.Route("/auth", func(r chi.Router) {
			r.Use(authRateLimit)
			r.Post("/register", authHandler.Register)
			r.Post("/login", authHandler.Login)
			r.Post("/refresh", authHandler.Refresh)
			r.Post("/logout", authHandler.Logout)
			r.Post("/verify-email", authHandler.VerifyEmail)
			r.Post("/resend-otp", authHandler.SendOTP)
			r.Post("/forgot-password", authHandler.ForgotPassword)
			r.Post("/reset-password", authHandler.ResetPassword)
		})

		interestsStore := interests.NewStore(s.db)
		interestsHandler := interests.NewHandler(interestsStore)

		// public interests route
		r.Get("/interests/categories", interestsHandler.GetCategories)

		// protected routes — JWT required
		r.Group(func(r chi.Router) {
			r.Use(middleware.JWTGuard(s.cfg.JWTSecret))

			// realtime events (LEVEL_UP / XP_AWARDED). No timeout middleware:
			// the connection is long-lived by design.
			r.Get("/ws", realtime.ServeWS(s.hub, middleware.GetUserID))

			userHandler := user.NewHandler(s.db, s.cfg.GetLocalLocation())
			avatarUploader := user.NewAvatarUploader(
				s.db,
				s.cfg.CloudinaryCloudName,
				s.cfg.CloudinaryAPIKey,
				s.cfg.CloudinaryAPISecret,
			)
			r.Route("/me", func(r chi.Router) {
				r.Use(chimiddleware.Timeout(20 * time.Second))
				r.Get("/", s.meHandler())
				r.Get("/stats", userHandler.GetStats)
				r.Post("/delete", userHandler.RequestDeletion)
				r.Post("/cancel-delete", userHandler.CancelDeletion)
				r.Get("/progress", s.progressHandler())
				r.Post("/avatar", avatarUploader.Upload)
				r.Get("/achievements", s.achievementsHandler())
				r.Post("/fcm-token", userHandler.RegisterFCMToken)
			})

			physiqueHandler := physique.NewHandler(s.db)
			r.Route("/physique", func(r chi.Router) {
				r.Group(func(r chi.Router) {
					r.Use(chimiddleware.Timeout(20 * time.Second))
					r.Post("/", physiqueHandler.Save)
					r.Get("/", physiqueHandler.Get)
				})
				r.Group(func(r chi.Router) {
					r.Use(chimiddleware.Timeout(10 * time.Minute))
					r.Use(middleware.UserRateLimit(s.rdb, 5, 24*time.Hour, "daily limit of 5 AI generations reached"))
					r.Post("/generate-quests", s.physiqueQuestHandler())
				})
			})

			//goals
			goalHandler := goal.NewHandler(pgstore.NewGoalStore(s.db), s.rdb)
			r.Route("/goals", func(r chi.Router) {
				r.Use(chimiddleware.Timeout(20 * time.Second))
				r.Get("/", goalHandler.List)
				r.Post("/", goalHandler.Create)
				r.Patch("/{id}", goalHandler.Update)
				r.Delete("/{id}", goalHandler.Delete)
			})

			// habits
			habitHandler := habit.NewHandler(pgstore.NewHabitStore(s.db, s.rdb, s.pub, s.cfg.GetLocalLocation()))
			r.Route("/habits", func(r chi.Router) {
				r.Use(chimiddleware.Timeout(20 * time.Second))
				r.Get("/", habitHandler.List)
				r.Post("/", habitHandler.Create)
				r.Post("/{id}/complete", habitHandler.Complete)
			})

			tracker := quest.NewInteractionTracker(s.db)

			// quests
			questHandler := quest.NewHandler(pgstore.NewQuestStore(s.db, s.rdb), tracker)
			r.Route("/quests", func(r chi.Router) {
				r.Group(func(r chi.Router) {
					r.Use(chimiddleware.Timeout(20 * time.Second))
					r.Get("/", questHandler.ListActive)
					r.Get("/history", questHandler.ListHistory)
					r.Get("/heatmap", questHandler.GetHeatmap)
					r.Post("/{id}/complete", questHandler.Complete)
					r.Post("/{id}/skip", questHandler.Skip)
				})
				r.Group(func(r chi.Router) {
					r.Use(chimiddleware.Timeout(10 * time.Minute))
					r.Use(middleware.UserRateLimit(s.rdb, 5, 24*time.Hour, "daily limit of 5 AI generations reached"))
					generateHandler := quest.NewGenerateHandler(s.db, s.rdb, s.mlClient, interestsStore, tracker)
					r.Post("/generate", generateHandler.Generate)
				})
			})

			// interests
			r.Route("/interests", func(r chi.Router) {
				r.Use(chimiddleware.Timeout(20 * time.Second))
				r.Get("/", interestsHandler.GetMyInterests)
				r.Post("/", interestsHandler.SaveInterests)
			})

			// notifications
			notificationStore := pgstore.NewNotificationStore(s.db)
			notificationHandler := notifications.NewHandler(notificationStore)
			r.Mount("/notifications", notificationHandler.Routes())
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
		p, err := s.profiles.GetProfile(r.Context(), userID)
		if err != nil {
			response.Error(w, http.StatusNotFound, "user not found")
			return
		}
		response.JSON(w, http.StatusOK, map[string]any{
			"id":         userID,
			"email":      p.Email,
			"username":   p.Username,
			"level":      p.Level,
			"current_xp": p.CurrentXP,
			"total_xp":   p.TotalXP,
			"xp_to_next": game.XPForLevel(p.Level + 1),
			"hp":         p.HP,
			"max_hp":     p.MaxHP,
			"strength":   p.Strength,
			"agility":    p.Agility,
			"mana":       p.Mana,
		})
	}
}

func (s *Server) Addr() string {
	return fmt.Sprintf(":%s", s.cfg.AppPort)
}

func (s *Server) progressHandler() http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		userID := middleware.GetUserID(r)
		entries, err := s.profiles.ListProgress(r.Context(), userID, 30)
		if err != nil {
			response.Error(w, http.StatusInternalServerError, "failed to fetch progress")
			return
		}

		logs := make([]map[string]any, 0, len(entries))
		for _, l := range entries {
			logs = append(logs, map[string]any{
				"event_type":   l.EventType,
				"xp_delta":     l.XPDelta,
				"level_before": l.LevelBefore,
				"level_after":  l.LevelAfter,
				"created_at":   l.CreatedAt,
			})
		}
		response.JSON(w, http.StatusOK, logs)
	}
}

func (s *Server) achievementsHandler() http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		userID := middleware.GetUserID(r)
		earnedMap, err := s.profiles.ListEarnedAchievements(r.Context(), userID)
		if err != nil {
			response.Error(w, http.StatusInternalServerError, "failed to fetch achievements")
			return
		}

		type achievementResp struct {
			Key         string  `json:"key"`
			Title       string  `json:"title"`
			Description string  `json:"description"`
			Tag         string  `json:"tag"`
			Icon        string  `json:"icon"`
			Earned      bool    `json:"earned"`
			EarnedAt    *string `json:"earned_at,omitempty"`
		}

		var result []achievementResp
		for _, a := range achievements.All {
			earned, ok := earnedMap[a.Key]
			resp := achievementResp{
				Key: a.Key, Title: a.Title,
				Description: a.Description,
				Tag:         a.Tag, Icon: a.Icon, Earned: ok,
			}
			if ok {
				resp.EarnedAt = &earned
			}
			result = append(result, resp)
		}
		response.JSON(w, http.StatusOK, result)
	}
}

func (s *Server) physiqueQuestHandler() http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		userID := middleware.GetUserID(r)
		if err := physique.GenerateExerciseQuests(r.Context(), s.db, userID); err != nil {
			response.Error(w, http.StatusBadRequest, err.Error())
			return
		}
		response.JSON(w, http.StatusOK, map[string]string{
			"message": "exercise quests generated based on your physique profile",
		})
	}
}
