package quest

import (
	"context"
	"crypto/sha256"
	"fmt"
	"log/slog"
	"net/http"
	"strconv"
	"strings"
	"time"

	"ascend-backend/internal/interests"
	"ascend-backend/internal/middleware"
	"ascend-backend/internal/mlservice"
	"ascend-backend/internal/physique"
	"ascend-backend/internal/store"
	pgstore "ascend-backend/internal/store/postgres"
	"ascend-backend/pkg/response"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/redis/go-redis/v9"
)

type GenerateHandler struct {
	db             *pgxpool.Pool
	rdb            *redis.Client
	mlClient       *mlservice.Client
	questStore     store.QuestStore
	interestsStore *interests.Store
	tracker        *InteractionTracker
}

func NewGenerateHandler(
	db *pgxpool.Pool,
	rdb *redis.Client,
	mlClient *mlservice.Client,
	interestsStore *interests.Store,
	tracker *InteractionTracker,
) *GenerateHandler {
	return &GenerateHandler{
		db:             db,
		rdb:            rdb,
		mlClient:       mlClient,
		questStore:     pgstore.NewQuestStore(db, rdb),
		interestsStore: interestsStore,
		tracker:        tracker,
	}
}

func (h *GenerateHandler) Generate(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	ctx := r.Context()

	// All rate limits and dedup checks removed for infinite generation
	contextHash := h.buildContextHash(ctx, userID)

	// enrich with user interests — required before generation
	var userInterests []interests.UserInterest
	if h.interestsStore != nil {
		var err error
		userInterests, err = h.interestsStore.GetByUser(ctx, userID)
		if err != nil {
			slog.Warn("interests load error", "user_id", userID, "error", err)
		}
	}
	if len(userInterests) == 0 {
		response.Error(w, http.StatusBadRequest,
			"please configure your interests before generating quests")
		return
	}
	// calculate difficulty adaptation based on completion rate
	var completedCount, totalCount int
	_ = h.db.QueryRow(ctx,
		`SELECT 
			COUNT(*) FILTER (WHERE status = 'completed'),
			COUNT(*)
		 FROM (
			SELECT status FROM quests 
			WHERE user_id = $1 AND is_ai_generated = true 
			ORDER BY created_at DESC LIMIT 10
		 ) sub`, userID,
	).Scan(&completedCount, &totalCount)

	completionRate := 0.5
	if totalCount >= 3 {
		completionRate = float64(completedCount) / float64(totalCount)
	}

	// ── try custom ML model first (HuggingFace Spaces) ──
	if h.mlClient != nil {
		mlProfile := h.buildMLProfile(ctx, userID, userInterests)
		mlProfile.CompletionRate = completionRate
		mlQuests, mlErr := h.mlClient.GenerateQuests(ctx, mlProfile)
		if mlErr == nil && len(mlQuests) > 0 {
			inserted := h.persistMLQuests(ctx, userID, mlQuests, contextHash)
			if len(inserted) > 0 {
				slog.Info("quests generated via ML model",
					"user_id", userID, "count", len(inserted))

				// log for DPO tracking
				if h.tracker != nil {
					for _, q := range inserted {
						_ = h.tracker.LogShown(ctx, userID, q["id"].(string), mlProfile, q, "v2-ml")
					}
				}

				response.JSON(w, http.StatusOK, inserted)
				return
			}
		}
		if mlErr != nil {
			slog.Warn("ML model failed, falling back to seeded quests",
				"user_id", userID, "error", mlErr)
		}
	}

	// ── fallback: seeded quests ──
	h.fallbackToSeeded(ctx, userID, w)
}

func (h *GenerateHandler) buildContextHash(ctx context.Context, userID string) string {
	var combined string
	rows, err := h.db.Query(ctx,
		`SELECT skill_name FROM user_skills WHERE user_id=$1 ORDER BY skill_level DESC LIMIT 5`,
		userID)
	if err == nil {
		defer rows.Close()
		for rows.Next() {
			var s string
			_ = rows.Scan(&s)
			combined += s
		}
	}
	gRows, err := h.db.Query(ctx,
		`SELECT title FROM goals WHERE user_id=$1 AND status='active' LIMIT 5`, userID)
	if err == nil {
		defer gRows.Close()
		for gRows.Next() {
			var g string
			_ = gRows.Scan(&g)
			combined += g
		}
	}
	raw := fmt.Sprintf("%s:%s:daily", userID, combined)
	sum := sha256.Sum256([]byte(raw))
	return fmt.Sprintf("%x", sum)
}

func (h *GenerateHandler) fallbackToSeeded(ctx context.Context, userID string, w http.ResponseWriter) {
	quests, err := h.questStore.ListActive(ctx, userID)
	if err != nil || len(quests) == 0 {
		response.JSON(w, http.StatusOK, []any{})
		return
	}
	response.JSON(w, http.StatusOK, quests)
}

// buildMLProfile assembles an mlservice.UserProfile from the user's DB data
// for the HuggingFace Spaces model.
func (h *GenerateHandler) buildMLProfile(
	ctx context.Context,
	userID string,
	userInterests []interests.UserInterest,
) mlservice.UserProfile {
	profile := mlservice.UserProfile{}

	// user level
	var level int
	err := h.db.QueryRow(ctx,
		`SELECT level FROM users WHERE id = $1`, userID,
	).Scan(&level)
	if err == nil {
		profile.Level = level
	}

	// quest completion count
	var questsCompleted int
	_ = h.db.QueryRow(ctx,
		`SELECT COUNT(*) FROM quests WHERE user_id=$1 AND status='completed'`, userID,
	).Scan(&questsCompleted)
	profile.QuestsCompleted = questsCompleted

	// best active streak from habits
	var currentStreak int
	_ = h.db.QueryRow(ctx,
		`SELECT COALESCE(MAX(current_streak), 0) FROM habits WHERE user_id=$1 AND is_active=true`, userID,
	).Scan(&currentStreak)
	profile.CurrentStreak = currentStreak

	// active goal text
	var goalText string
	_ = h.db.QueryRow(ctx,
		`SELECT title FROM goals WHERE user_id=$1 AND status='active'
		 ORDER BY priority DESC LIMIT 1`, userID,
	).Scan(&goalText)
	profile.Goal = goalText

	// interests
	for _, ui := range userInterests {
		profile.Interests = append(profile.Interests, mlservice.Interest{
			Category:    ui.Category,
			Subcategory: ui.Subcategory,
			Priority:    strconv.Itoa(ui.Priority),
		})
	}

	// physique (optional)
	if p, m, err := physique.GetProfile(ctx, h.db, userID); err == nil {
		bmi := m.BMI
		profile.Physique = &mlservice.Physique{
			BMI:           &bmi,
			Goal:          p.BodyGoal,
			ActivityLevel: p.FitnessLevel,
		}
	}

	return profile
}

// persistMLQuests maps mlservice.Quest → DB insert, mirroring the existing RAG persist logic.
func (h *GenerateHandler) persistMLQuests(
	ctx context.Context,
	userID string,
	mlQuests []mlservice.Quest,
	contextHash string,
) []map[string]any {
	// 1. Load existing active and completed quests to prevent duplicates
	existingTitles := make(map[string]bool)
	rows, err := h.db.Query(ctx, `SELECT title FROM quests WHERE user_id = $1`, userID)
	if err == nil {
		defer rows.Close()
		for rows.Next() {
			var t string
			if scanErr := rows.Scan(&t); scanErr == nil {
				existingTitles[strings.ToLower(strings.TrimSpace(t))] = true
			}
		}
	}

	var inserted []map[string]any
	for _, q := range mlQuests {
		// Dedup check
		if existingTitles[strings.ToLower(strings.TrimSpace(q.Title))] {
			continue
		}
		existingTitles[strings.ToLower(strings.TrimSpace(q.Title))] = true

		questID := uuid.NewString()
		expires := time.Now().Add(24 * time.Hour)

		// map difficulty string → int (easy=1, medium=2, hard=3, etc.)
		diff := mapDifficulty(q.Difficulty)

		questType := strings.ToLower(q.QuestType)
		if questType != "daily" && questType != "weekly" {
			questType = "daily"
		}

		_, dbErr := h.db.Exec(ctx,
			`INSERT INTO quests
			   (id, user_id, title, description, type, difficulty, xp_reward,
			    status, is_ai_generated, skill_area, ai_prompt_hash, expires_at, created_at)
			 VALUES ($1,$2,$3,$4,$5,$6,$7,'active',true,$8,$9,$10,$11)`,
			questID, userID, q.Title, q.Description, questType,
			diff, q.XPReward, q.SkillArea,
			contextHash, expires, time.Now(),
		)
		if dbErr != nil {
			slog.Error("insert ML quest failed", "error", dbErr)
			continue
		}
		inserted = append(inserted, map[string]any{
			"id":              questID,
			"title":           q.Title,
			"description":     q.Description,
			"type":            questType,
			"difficulty":      diff,
			"xp_reward":       q.XPReward,
			"skill_area":      q.SkillArea,
			"status":          "active",
			"is_ai_generated": true,
			"source":          "ml-model",
		})
	}
	return inserted
}

// mapDifficulty converts a string difficulty label to an integer.
func mapDifficulty(s string) int {
	switch s {
	case "easy", "1":
		return 1
	case "medium", "2":
		return 2
	case "hard", "3":
		return 3
	case "expert", "4":
		return 4
	case "legendary", "5":
		return 5
	default:
		return 1
	}
}
