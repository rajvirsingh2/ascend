package postgres

import (
	"context"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/rajvirsingh2/ascend-backend/internal/game"
	"github.com/rajvirsingh2/ascend-backend/internal/models"
)

type QuestStore struct{ db *pgxpool.Pool }

func NewQuestStore(db *pgxpool.Pool) *QuestStore {
	return &QuestStore{db: db}
}

func (s *QuestStore) ListActive(ctx context.Context, userID string) ([]*models.Quest, error) {
	rows, err := s.db.Query(ctx,
		`SELECT id, user_id, goal_id, title, description, type, difficulty,
		        xp_reward, status, is_ai_generated, skill_area, expires_at,
		        completed_at, created_at
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
			&q.CompletedAt, &q.CreatedAt,
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
		        completed_at, created_at
		 FROM quests WHERE id=$1 AND user_id=$2`,
		id, userID,
	).Scan(
		&q.ID, &q.UserID, &q.GoalID, &q.Title, &q.Description,
		&q.Type, &q.Difficulty, &q.XPReward, &q.Status,
		&q.IsAIGenerated, &q.SkillArea, &q.ExpiresAt,
		&q.CompletedAt, &q.CreatedAt,
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

	return game.AwardXP(ctx, s.db, userID, "quest", id, "quest_completed", q.XPReward)
}

func (s *QuestStore) Skip(ctx context.Context, id, userID string) error {
	_, err := s.db.Exec(ctx,
		`UPDATE quests SET status='skipped' WHERE id=$1 AND user_id=$2`,
		id, userID,
	)
	return err
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
