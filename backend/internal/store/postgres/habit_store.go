package postgres

import (
	"context"
	"math"
	"time"

	"ascend-backend/internal/events"
	"ascend-backend/internal/game"
	"ascend-backend/internal/models"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/redis/go-redis/v9"
)

type HabitStore struct {
	db  *pgxpool.Pool
	rdb *redis.Client
	pub *events.Publisher
	loc *time.Location
}

func NewHabitStore(db *pgxpool.Pool, rdb *redis.Client, pub *events.Publisher, loc *time.Location) *HabitStore {
	if loc == nil {
		loc = time.UTC
	}
	return &HabitStore{db: db, rdb: rdb, pub: pub, loc: loc}
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

	var freezesAvailable int
	_ = s.db.QueryRow(ctx, "SELECT streak_freezes FROM users WHERE id=$1", userID).Scan(&freezesAvailable)

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
		h.CurrentStreak = calculateEffectiveStreak(h, freezesAvailable, s.loc)
		habits = append(habits, h)
	}
	return habits, nil
}

func calculateEffectiveStreak(h *models.Habit, freezesAvailable int, loc *time.Location) int {
	if h.LastCompletedAt == nil {
		return 0
	}
	now := time.Now().In(loc)
	last := h.LastCompletedAt.In(loc)
	lastDay := time.Date(last.Year(), last.Month(), last.Day(), 0, 0, 0, 0, loc)
	nowDay := time.Date(now.Year(), now.Month(), now.Day(), 0, 0, 0, 0, loc)
	daysDiff := int(math.Round(nowDay.Sub(lastDay).Hours() / 24))

	if daysDiff <= 1 {
		return h.CurrentStreak
	}

	missedDays := daysDiff - 1
	if freezesAvailable >= missedDays {
		return h.CurrentStreak // Streak is preserved by freezes
	}

	return 0 // Streak broken
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
	if err == nil {
		var freezesAvailable int
		_ = s.db.QueryRow(ctx, "SELECT streak_freezes FROM users WHERE id=$1", userID).Scan(&freezesAvailable)
		h.CurrentStreak = calculateEffectiveStreak(h, freezesAvailable, s.loc)
	}
	return h, err
}

// Complete is idempotent — completing the same habit twice in one day is a no-op.
func (s *HabitStore) Complete(ctx context.Context, id, userID string) (*game.XPResult, error) {
	h, err := s.GetByID(ctx, id, userID)
	if err != nil {
		return nil, err
	}

	now := time.Now().In(s.loc)

	// idempotency check — already completed today
	if h.LastCompletedAt != nil {
		last := h.LastCompletedAt.In(s.loc)
		if last.Year() == now.Year() && last.YearDay() == now.YearDay() {
			return nil, nil // signal: already done today
		}
	}

	newStreak := 1
	var freezesUsed int

	if h.LastCompletedAt != nil {
		last := h.LastCompletedAt.In(s.loc)
		// calculate whole days between last completion and now
		lastDay := time.Date(last.Year(), last.Month(), last.Day(), 0, 0, 0, 0, s.loc)
		nowDay := time.Date(now.Year(), now.Month(), now.Day(), 0, 0, 0, 0, s.loc)
		daysDiff := int(math.Round(nowDay.Sub(lastDay).Hours() / 24))

		if daysDiff == 1 {
			newStreak = h.CurrentStreak + 1
		} else if daysDiff > 1 {
			// missed some days, check if we have enough freezes
			missedDays := daysDiff - 1
			var freezesAvailable int
			err := s.db.QueryRow(ctx, "SELECT streak_freezes FROM users WHERE id=$1", userID).Scan(&freezesAvailable)
			if err == nil && freezesAvailable >= missedDays {
				freezesUsed = missedDays
				newStreak = h.CurrentStreak + 1
			}
		}
	}

	newLongest := h.LongestStreak
	if newStreak > newLongest {
		newLongest = newStreak
	}

	tx, err := s.db.Begin(ctx)
	if err != nil {
		return nil, err
	}
	defer func() { _ = tx.Rollback(ctx) }()

	_, err = tx.Exec(ctx,
		`UPDATE habits
		 SET current_streak=$1, longest_streak=$2, last_completed_at=$3
		 WHERE id=$4`,
		newStreak, newLongest, now, id,
	)
	if err != nil {
		return nil, err
	}

	if freezesUsed > 0 {
		_, err = tx.Exec(ctx,
			`UPDATE users SET streak_freezes = streak_freezes - $1 WHERE id = $2`,
			freezesUsed, userID,
		)
		if err != nil {
			return nil, err
		}
	}

	if err := tx.Commit(ctx); err != nil {
		return nil, err
	}

	if newStreak%5 == 0 && s.pub != nil {
		go func() {
			_ = s.pub.Publish(context.Background(), events.StreamHabitCompleted, events.Event{
				UserID: userID,
				Payload: map[string]any{
					"id":             h.ID,
					"xp_reward":      h.XPReward,
					"frequency":      h.Frequency,
					"current_streak": newStreak,
					"longest_streak": newLongest,
					"title":          h.Title,
				},
			})
		}()

	}

	hpRestored := newStreak * 2
	return game.AwardXP(ctx, s.db, userID, "habit", id, "habit_completed", "General", h.XPReward, hpRestored)
}
