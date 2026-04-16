package quest

import (
	"context"
	"crypto/sha256"
	"fmt"
	"log"
	"net/http"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/rajvirsingh2/ascend-backend/internal/ai"
	"github.com/rajvirsingh2/ascend-backend/internal/middleware"
	"github.com/rajvirsingh2/ascend-backend/internal/store"
	"github.com/rajvirsingh2/ascend-backend/internal/store/postgres"
	"github.com/rajvirsingh2/ascend-backend/pkg/response"
	"github.com/redis/go-redis/v9"
)

const (
	generateRateLimitMax    = 10
	generateRateLimitWindow = 24 * time.Hour
	dedupWindow             = 7 * 24 * time.Hour
)

type GenerateHandler struct {
	db         *pgxpool.Pool
	rdb        *redis.Client
	aiClient   *ai.Client
	questStore store.QuestStore
}

func NewGenerateHandler(
	db *pgxpool.Pool,
	rdb *redis.Client,
	aiClient *ai.Client,
) *GenerateHandler {
	return &GenerateHandler{
		db:         db,
		rdb:        rdb,
		aiClient:   aiClient,
		questStore: postgres.NewQuestStore(db, rdb),
	}
}

func (h *GenerateHandler) Generate(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)
	ctx := r.Context()

	// per-user rate limit: 3 calls per 24 hours
	rateLimitKey := fmt.Sprintf("gen_rate:%s", userID)
	count, err := h.rdb.Incr(ctx, rateLimitKey).Result()
	if err == nil && count == 1 {
		h.rdb.Expire(ctx, rateLimitKey, generateRateLimitWindow)
	}
	if count > generateRateLimitMax {
		response.Error(w, http.StatusTooManyRequests,
			"quest generation limit reached (3 per day)")
		return
	}

	// context hash dedup — skip AI call if same context was used recently
	contextHash := h.buildContextHash(ctx, userID)
	if h.isDuplicate(ctx, userID, contextHash) {
		// return cached quests from DB
		quests, _ := h.questStore.ListActive(ctx, userID)
		response.JSON(w, http.StatusOK, quests)
		return
	}

	// call RAG service
	result, err := h.aiClient.GenerateQuests(ctx, ai.GenerateRequest{
		UserID:      userID,
		GenerateFor: "daily",
	})
	if err != nil {
		log.Printf("ai generation failed for user %s: %v — falling back to seeded quests", userID, err)
		h.fallbackToSeeded(ctx, userID, w)
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
			log.Printf("failed to insert generated quest: %v", dbErr)
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

	response.JSON(w, http.StatusOK, inserted)
}

func (h *GenerateHandler) buildContextHash(ctx context.Context, userID string) string {
	var skills, goals string
	rows, _ := h.db.Query(ctx,
		`SELECT skill_name FROM user_skills WHERE user_id=$1 ORDER BY skill_level DESC LIMIT 5`,
		userID,
	)
	defer rows.Close()
	for rows.Next() {
		var s string
		rows.Scan(&s)
		skills += s
	}

	gRows, _ := h.db.Query(ctx,
		`SELECT title FROM goals WHERE user_id=$1 AND status='active' LIMIT 5`,
		userID,
	)
	defer gRows.Close()
	for gRows.Next() {
		var g string
		gRows.Scan(&g)
		goals += g
	}

	raw := fmt.Sprintf("%s:%s:%s:daily", userID, skills, goals)
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

func (h *GenerateHandler) fallbackToSeeded(
	ctx context.Context, userID string, w http.ResponseWriter,
) {
	quests, err := h.questStore.ListActive(ctx, userID)
	if err != nil || len(quests) == 0 {
		response.Error(w, http.StatusServiceUnavailable,
			"quest generation unavailable, please try again later")
		return
	}
	response.JSON(w, http.StatusOK, quests)
}
