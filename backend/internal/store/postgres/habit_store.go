package postgres

import (
	"context"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/rajvirsingh2/ascend-backend/internal/game"
	"github.com/rajvirsingh2/ascend-backend/internal/ingestion"
	"github.com/rajvirsingh2/ascend-backend/internal/models"
	"github.com/redis/go-redis/v9"
)

type HabitStore struct {
	db  *pgxpool.Pool
	rdb *redis.Client
}

func NewHabitStore(db *pgxpool.Pool, rdb *redis.Client) *HabitStore {
	return &HabitStore{db: db, rdb: rdb}
}

func (s *HabitStore) Create(ctx context.Context, h *models.Habit) error {
	h.ID = uuid.NewString()
	h.CreatedAt = time.Now()
	h.IsActive = true
	_, err := s.db.Exec(ctx,
		`INSERT INTO habits
		   (id, user_id, goal_id, title, frequency, xp_reward, is_active, created_at)
		 VALUES ($1,$2,$3,$4,$5,$6,$7,$8)`,
		h.ID, h.UserID, h.GoalID, h.Title,
		h.Frequency, h.XPReward, h.IsActive, h.CreatedAt,
	)
	return err
}

func (s *HabitStore) ListByUser(ctx context.Context, userID string) ([]*models.Habit, error) {
	rows, err := s.db.Query(ctx,
		`SELECT id, user_id, goal_id, title, frequency, xp_reward,
		        current_streak, longest_streak, last_completed_at, is_active, created_at
		 FROM habits WHERE user_id=$1 AND is_active=true
		 ORDER BY created_at ASC`,
		userID,
	)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var habits []*models.Habit
	for rows.Next() {
		h := &models.Habit{}
		err := rows.Scan(
			&h.ID, &h.UserID, &h.GoalID, &h.Title, &h.Frequency,
			&h.XPReward, &h.CurrentStreak, &h.LongestStreak,
			&h.LastCompletedAt, &h.IsActive, &h.CreatedAt,
		)
		if err != nil {
			return nil, err
		}
		habits = append(habits, h)
	}
	return habits, nil
}

func (s *HabitStore) GetByID(ctx context.Context, id, userID string) (*models.Habit, error) {
	h := &models.Habit{}
	err := s.db.QueryRow(ctx,
		`SELECT id, user_id, goal_id, title, frequency, xp_reward,
		        current_streak, longest_streak, last_completed_at, is_active, created_at
		 FROM habits WHERE id=$1 AND user_id=$2`,
		id, userID,
	).Scan(
		&h.ID, &h.UserID, &h.GoalID, &h.Title, &h.Frequency,
		&h.XPReward, &h.CurrentStreak, &h.LongestStreak,
		&h.LastCompletedAt, &h.IsActive, &h.CreatedAt,
	)
	return h, err
}

// Complete is idempotent — completing the same habit twice in one day is a no-op.
func (s *HabitStore) Complete(ctx context.Context, id, userID string) (*game.XPResult, error) {
	h, err := s.GetByID(ctx, id, userID)
	if err != nil {
		return nil, err
	}

	now := time.Now()

	// idempotency check — already completed today
	if h.LastCompletedAt != nil {
		last := *h.LastCompletedAt
		if last.Year() == now.Year() && last.YearDay() == now.YearDay() {
			return nil, nil // signal: already done today
		}
	}

	// determine if streak continues or resets
	newStreak := 1
	if h.LastCompletedAt != nil {
		yesterday := now.AddDate(0, 0, -1)
		last := *h.LastCompletedAt
		if last.Year() == yesterday.Year() && last.YearDay() == yesterday.YearDay() {
			newStreak = h.CurrentStreak + 1
		}
	}

	newLongest := h.LongestStreak
	if newStreak > newLongest {
		newLongest = newStreak
	}

	_, err = s.db.Exec(ctx,
		`UPDATE habits
		 SET current_streak=$1, longest_streak=$2, last_completed_at=$3
		 WHERE id=$4`,
		newStreak, newLongest, now, id,
	)
	if err != nil {
		return nil, err
	}

	// publish habit milestone every 5 streak days
	if newStreak%5 == 0 {
		go ingestion.Publish(context.Background(), s.rdb, ingestion.Job{
			EventType: ingestion.EventHabitMilestone,
			UserID:    userID,
			Payload: map[string]any{
				"id":             id,
				"title":          h.Title,
				"frequency":      h.Frequency,
				"xp_reward":      h.XPReward,
				"current_streak": newStreak,
				"longest_streak": newLongest,
			},
		})
	}

	return game.AwardXP(ctx, s.db, userID, "habit", id, "habit_completed", h.XPReward)
}
