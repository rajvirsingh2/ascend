package postgres

import (
	"context"
	"time"

	"ascend-backend/internal/events"
	"ascend-backend/internal/game"
	"ascend-backend/internal/models"
	"ascend-backend/internal/store"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/redis/go-redis/v9"
)

type QuestStore struct {
	db  *pgxpool.Pool
	rdb *redis.Client
	pub *events.Publisher
}

func NewQuestStore(db *pgxpool.Pool, rdb *redis.Client) *QuestStore {
	return &QuestStore{
		db:  db,
		rdb: rdb,
		pub: events.NewPublisher(rdb),
	}
}

func (s *QuestStore) ListActive(ctx context.Context, userID string) ([]*models.Quest, error) {
	rows, err := s.db.Query(ctx,
		`SELECT id, user_id, goal_id, title, description, type, difficulty,
		        xp_reward, status, is_ai_generated, skill_area, expires_at,
		        completed_at, created_at, reminder_sent
		 FROM quests
		 WHERE user_id=$1 AND status='active'
		 ORDER BY created_at DESC`,
		userID,
	)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var quests []*models.Quest
	for rows.Next() {
		q := &models.Quest{}
		err := rows.Scan(
			&q.ID, &q.UserID, &q.GoalID, &q.Title, &q.Description,
			&q.Type, &q.Difficulty, &q.XPReward, &q.Status,
			&q.IsAIGenerated, &q.SkillArea, &q.ExpiresAt,
			&q.CompletedAt, &q.CreatedAt, &q.ReminderSent,
		)
		if err != nil {
			return nil, err
		}
		quests = append(quests, q)
	}
	return quests, nil
}

func (s *QuestStore) ListHistory(ctx context.Context, userID string) ([]*models.Quest, error) {
	rows, err := s.db.Query(ctx,
		`SELECT id, user_id, goal_id, title, description, type, difficulty,
		        xp_reward, status, is_ai_generated, skill_area, expires_at,
		        completed_at, created_at, reminder_sent
		 FROM quests
		 WHERE user_id=$1 AND status IN ('completed', 'skipped', 'expired')
		 ORDER BY completed_at DESC NULLS LAST, created_at DESC
		 LIMIT 50`,
		userID,
	)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var quests []*models.Quest
	for rows.Next() {
		q := &models.Quest{}
		err := rows.Scan(
			&q.ID, &q.UserID, &q.GoalID, &q.Title, &q.Description,
			&q.Type, &q.Difficulty, &q.XPReward, &q.Status,
			&q.IsAIGenerated, &q.SkillArea, &q.ExpiresAt,
			&q.CompletedAt, &q.CreatedAt, &q.ReminderSent,
		)
		if err != nil {
			return nil, err
		}
		quests = append(quests, q)
	}
	return quests, nil
}

func (s *QuestStore) GetByID(ctx context.Context, id, userID string) (*models.Quest, error) {
	q := &models.Quest{}
	err := s.db.QueryRow(ctx,
		`SELECT id, user_id, goal_id, title, description, type, difficulty,
		        xp_reward, status, is_ai_generated, skill_area, expires_at,
		        completed_at, created_at, reminder_sent
		 FROM quests WHERE id=$1 AND user_id=$2`,
		id, userID,
	).Scan(
		&q.ID, &q.UserID, &q.GoalID, &q.Title, &q.Description,
		&q.Type, &q.Difficulty, &q.XPReward, &q.Status,
		&q.IsAIGenerated, &q.SkillArea, &q.ExpiresAt,
		&q.CompletedAt, &q.CreatedAt, &q.ReminderSent,
	)
	return q, err
}

func (s *QuestStore) Complete(ctx context.Context, id, userID string) (*game.XPResult, error) {
	q, err := s.GetByID(ctx, id, userID)
	if err != nil {
		return nil, err
	}

	now := time.Now()
	_, err = s.db.Exec(ctx,
		`UPDATE quests SET status='completed', completed_at=$1 WHERE id=$2`,
		now, id,
	)
	if err != nil {
		return nil, err
	}

	// publish to Redis Stream (fire and forget)
	if s.pub != nil {
		go func() {
			_ = s.pub.Publish(context.Background(), events.StreamQuestCompleted, events.Event{
				UserID: userID,
				Type:   "QuestCompleted",
				Payload: map[string]any{
					"quest_id":        q.ID,
					"xp_reward":       q.XPReward,
					"skill_area":      q.SkillArea,
					"difficulty":      q.Difficulty,
					"type":            q.Type,
					"is_ai_generated": q.IsAIGenerated,
					"title":           q.Title,
					"status":          "completed",
				},
			})
		}()
	}

	// award XP synchronously
	return game.AwardXP(ctx, s.db, userID, "quest", id, "quest_completed", q.SkillArea, q.XPReward, 0)
}

func (s *QuestStore) Skip(ctx context.Context, id, userID string) (*store.SkipResult, error) {
	tx, err := s.db.Begin(ctx)
	if err != nil {
		return nil, err
	}
	defer func() { _ = tx.Rollback(ctx) }()

	res, err := tx.Exec(ctx,
		`UPDATE quests SET status='skipped', completed_at=NOW() WHERE id=$1 AND user_id=$2 AND status='active'`,
		id, userID,
	)
	if err != nil {
		return nil, err
	}
	if res.RowsAffected() == 0 {
		return nil, pgx.ErrNoRows
	}

	var skipsUsed int
	err = tx.QueryRow(ctx,
		`SELECT COUNT(*) FROM quests WHERE user_id=$1 AND status='skipped' AND date_trunc('month', completed_at) = date_trunc('month', NOW())`,
		userID,
	).Scan(&skipsUsed)
	if err != nil {
		return nil, err
	}

	if err := tx.Commit(ctx); err != nil {
		return nil, err
	}

	result := &store.SkipResult{
		SkipsUsed: skipsUsed,
	}

	if skipsUsed >= 5 {
		hpDamage := (skipsUsed - 4) * 5
		xpResult, err := game.AwardXP(ctx, s.db, userID, "quest", id, "quest_skipped", "General", 0, -hpDamage)
		if err != nil {
			return nil, err
		}
		result.HPDamage = xpResult.HPDamage
		result.HPAfter = xpResult.HPAfter
		result.Died = xpResult.Died
		result.StatDeltas = xpResult.StatDeltas
	} else {
		var hp int
		_ = s.db.QueryRow(ctx, "SELECT hp FROM users WHERE id=$1", userID).Scan(&hp)
		result.HPAfter = hp
	}

	return result, nil
}

func (s *QuestStore) ExpireOld(ctx context.Context) error {
	_, err := s.db.Exec(ctx,
		`UPDATE quests SET status='expired'
		 WHERE status='active'
		   AND expires_at IS NOT NULL
		   AND expires_at < NOW()`,
	)
	return err
}

func (s *QuestStore) GetHeatmap(ctx context.Context, userID string) ([]models.HeatmapPoint, error) {
	rows, err := s.db.Query(ctx,
		`SELECT TO_CHAR(completed_at, 'YYYY-MM-DD') as date, COUNT(*) as count
		 FROM quests
		 WHERE user_id=$1 AND status='completed' AND completed_at IS NOT NULL
		 GROUP BY TO_CHAR(completed_at, 'YYYY-MM-DD')
		 ORDER BY date ASC`,
		userID,
	)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var points []models.HeatmapPoint
	for rows.Next() {
		var p models.HeatmapPoint
		if err := rows.Scan(&p.Date, &p.Count); err != nil {
			return nil, err
		}
		points = append(points, p)
	}
	if points == nil {
		points = []models.HeatmapPoint{}
	}
	return points, nil
}

func (s *QuestStore) GetExpiringQuests(ctx context.Context, duration time.Duration) ([]*models.Quest, error) {
	threshold := time.Now().Add(duration)
	rows, err := s.db.Query(ctx,
		`SELECT id, user_id, goal_id, title, description, type, difficulty,
		        xp_reward, status, is_ai_generated, skill_area, expires_at,
		        completed_at, created_at, reminder_sent
		 FROM quests
		 WHERE status='active' AND reminder_sent=FALSE AND expires_at IS NOT NULL AND expires_at <= $1 AND expires_at > NOW()`,
		threshold,
	)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var quests []*models.Quest
	for rows.Next() {
		q := &models.Quest{}
		err := rows.Scan(
			&q.ID, &q.UserID, &q.GoalID, &q.Title, &q.Description,
			&q.Type, &q.Difficulty, &q.XPReward, &q.Status,
			&q.IsAIGenerated, &q.SkillArea, &q.ExpiresAt,
			&q.CompletedAt, &q.CreatedAt, &q.ReminderSent,
		)
		if err != nil {
			return nil, err
		}
		quests = append(quests, q)
	}
	return quests, nil
}

func (s *QuestStore) MarkReminderSent(ctx context.Context, questID string) error {
	_, err := s.db.Exec(ctx, `UPDATE quests SET reminder_sent=TRUE WHERE id=$1`, questID)
	return err
}
