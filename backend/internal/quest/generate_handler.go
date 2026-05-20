package quest

import (
	"context"
	"crypto/sha256"
	"fmt"
	"log/slog"
	"net/http"
	"time"

	"ascend-backend/internal/ai"
	"ascend-backend/internal/interests"
	"ascend-backend/internal/keyvault"
	"ascend-backend/internal/middleware"
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
	aiClient       *ai.Client
	vault          *keyvault.Vault
	questStore     store.QuestStore
	interestsStore *interests.Store
}

func NewGenerateHandler(
	db *pgxpool.Pool,
	rdb *redis.Client,
	aiClient *ai.Client,
	vault *keyvault.Vault,
	interestsStore *interests.Store,
) *GenerateHandler {
	return &GenerateHandler{
		db:             db,
		rdb:            rdb,
		aiClient:       aiClient,
		vault:          vault,
		questStore:     pgstore.NewQuestStore(db, rdb),
		interestsStore: interestsStore,
	}
}

func (h *GenerateHandler) Generate(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	ctx := r.Context()

	// per-user rate limit: 3 calls per 24 hours
	rateLimitKey := fmt.Sprintf("gen_rate:%s", userID)
	count, err := h.rdb.Incr(ctx, rateLimitKey).Result()
	if err == nil && count == 1 {
		h.rdb.Expire(ctx, rateLimitKey, 24*time.Hour)
	}
	if count > 3 {
		response.Error(w, http.StatusTooManyRequests,
			"quest generation limit reached (3 per day)")
		return
	}

	// build AI request — use BYOK key if available, else use mock
	aiReq := ai.GenerateRequest{
		UserID:      userID,
		GenerateFor: "daily",
		Provider:    "mock",
		APIKey:      "",
		Model:       "",
	}

	if h.vault != nil {
		plaintextKey, rec, err := h.vault.Decrypt(ctx, userID)
		if err == nil {
			aiReq.Provider = rec.Provider
			aiReq.APIKey = string(plaintextKey)
			aiReq.Model = rec.ModelOverride
			defer keyvault.ZeroBytes(plaintextKey)
		} else {
			slog.Info("no API key for user — using mock provider", "user_id", userID)
		}
	}

	// dedup check
	contextHash := h.buildContextHash(ctx, userID)
	if h.isDuplicate(ctx, userID, contextHash) {
		quests, _ := h.questStore.ListActive(ctx, userID)
		response.JSON(w, http.StatusOK, quests)
		return
	}

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
	aiReq.AdditionalContext = interests.BuildRAGContext(userInterests)

	// inject physique data only if user has a physical interest
	hasPhysical := false
	for _, i := range userInterests {
		if i.Category == "physical" {
			hasPhysical = true
			break
		}
	}
	if hasPhysical {
		if profile, metrics, err := physique.GetProfile(ctx, h.db, userID); err == nil {
			aiReq.BodyGoal = profile.BodyGoal
			aiReq.FitnessLevel = profile.FitnessLevel
			aiReq.BMI = metrics.BMI
			aiReq.TDEE = metrics.TDEE
			aiReq.GoalCalories = metrics.GoalCalories
		}
	}

	// call RAG service
	result, err := h.aiClient.GenerateQuests(ctx, aiReq)
	if err != nil {
		slog.Error("AI generation failed", "user_id", userID, "error", err)
		response.Error(w, http.StatusServiceUnavailable,
			"quest generation is unavailable — please add your API key in Settings")
		return
	}

	// persist generated quests
	var inserted []map[string]any
	for _, q := range result.Quests {
		questID := uuid.NewString()
		expires := time.Now().Add(24 * time.Hour)
		_, dbErr := h.db.Exec(ctx,
			`INSERT INTO quests
			   (id, user_id, title, description, type, difficulty, xp_reward,
			    status, is_ai_generated, skill_area, ai_prompt_hash, expires_at, created_at)
			 VALUES ($1,$2,$3,$4,$5,$6,$7,'active',true,$8,$9,$10,$11)`,
			questID, userID, q.Title, q.Description, q.Type,
			q.Difficulty, q.XPReward, q.SkillArea,
			contextHash, expires, time.Now(),
		)
		if dbErr != nil {
			slog.Error("insert generated quest failed", "error", dbErr)
			continue
		}
		inserted = append(inserted, map[string]any{
			"id":              questID,
			"title":           q.Title,
			"description":     q.Description,
			"type":            q.Type,
			"difficulty":      q.Difficulty,
			"xp_reward":       q.XPReward,
			"skill_area":      q.SkillArea,
			"status":          "active",
			"is_ai_generated": true,
		})
	}

	if len(inserted) == 0 {
		response.Error(w, http.StatusInternalServerError, "failed to save generated quests")
		return
	}

	response.JSON(w, http.StatusOK, inserted)
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
			rows.Scan(&s)
			combined += s
		}
	}
	gRows, err := h.db.Query(ctx,
		`SELECT title FROM goals WHERE user_id=$1 AND status='active' LIMIT 5`, userID)
	if err == nil {
		defer gRows.Close()
		for gRows.Next() {
			var g string
			gRows.Scan(&g)
			combined += g
		}
	}
	raw := fmt.Sprintf("%s:%s:daily", userID, combined)
	sum := sha256.Sum256([]byte(raw))
	return fmt.Sprintf("%x", sum)
}

func (h *GenerateHandler) isDuplicate(ctx context.Context, userID, hash string) bool {
	var count int
	h.db.QueryRow(ctx,
		`SELECT COUNT(*) FROM quest_generation_log
		 WHERE user_id=$1 AND context_hash=$2
		   AND created_at > NOW() - INTERVAL '7 days'`,
		userID, hash,
	).Scan(&count)
	return count > 0
}

func (h *GenerateHandler) fallbackToSeeded(ctx context.Context, userID string, w http.ResponseWriter) {
	quests, err := h.questStore.ListActive(ctx, userID)
	if err != nil || len(quests) == 0 {
		response.JSON(w, http.StatusOK, []any{})
		return
	}
	response.JSON(w, http.StatusOK, quests)
}
