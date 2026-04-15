package postgres

import (
	"context"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/rajvirsingh2/ascend-backend/internal/models"
)

type GoalStore struct{ db *pgxpool.Pool }

func NewGoalStore(db *pgxpool.Pool) *GoalStore {
	return &GoalStore{db: db}
}

func (s *GoalStore) Create(ctx context.Context, g *models.Goal) error {
	g.ID = uuid.NewString()
	g.CreatedAt = time.Now()
	g.Status = "active"
	_, err := s.db.Exec(ctx,
		`INSERT INTO goals
		   (id, user_id, title, description, category, skill_area, priority, target_date, status, progress, created_at)
		 VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11)`,
		g.ID, g.UserID, g.Title, g.Description, g.Category,
		g.SkillArea, g.Priority, g.TargetDate, g.Status, g.Progress, g.CreatedAt,
	)
	return err
}

func (s *GoalStore) ListByUser(ctx context.Context, userID string) ([]*models.Goal, error) {
	rows, err := s.db.Query(ctx,
		`SELECT id, user_id, title, description, category, skill_area,
		        priority, target_date, status, progress, created_at, completed_at
		 FROM goals WHERE user_id = $1 AND status != 'abandoned'
		 ORDER BY priority DESC, created_at DESC`,
		userID,
	)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var goals []*models.Goal
	for rows.Next() {
		g := &models.Goal{}
		err := rows.Scan(
			&g.ID, &g.UserID, &g.Title, &g.Description, &g.Category,
			&g.SkillArea, &g.Priority, &g.TargetDate, &g.Status,
			&g.Progress, &g.CreatedAt, &g.CompletedAt,
		)
		if err != nil {
			return nil, err
		}
		goals = append(goals, g)
	}
	return goals, nil
}

func (s *GoalStore) GetByID(ctx context.Context, id, userID string) (*models.Goal, error) {
	g := &models.Goal{}
	err := s.db.QueryRow(ctx,
		`SELECT id, user_id, title, description, category, skill_area,
		        priority, target_date, status, progress, created_at, completed_at
		 FROM goals WHERE id = $1 AND user_id = $2`,
		id, userID,
	).Scan(
		&g.ID, &g.UserID, &g.Title, &g.Description, &g.Category,
		&g.SkillArea, &g.Priority, &g.TargetDate, &g.Status,
		&g.Progress, &g.CreatedAt, &g.CompletedAt,
	)
	return g, err
}

func (s *GoalStore) Update(ctx context.Context, g *models.Goal) error {
	_, err := s.db.Exec(ctx,
		`UPDATE goals
		 SET title=$1, description=$2, priority=$3, status=$4,
		     progress=$5, target_date=$6, completed_at=$7
		 WHERE id=$8 AND user_id=$9`,
		g.Title, g.Description, g.Priority, g.Status,
		g.Progress, g.TargetDate, g.CompletedAt, g.ID, g.UserID,
	)
	return err
}

func (s *GoalStore) Delete(ctx context.Context, id, userID string) error {
	_, err := s.db.Exec(ctx,
		`UPDATE goals SET status='abandoned' WHERE id=$1 AND user_id=$2`,
		id, userID,
	)
	return err
}
