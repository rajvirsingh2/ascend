package postgres

import (
	"context"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
)

// UserProfileStore owns the read queries behind /me, /me/progress and
// /me/achievements (previously inline SQL in the server package).
type UserProfileStore struct {
	db *pgxpool.Pool
}

func NewUserProfileStore(db *pgxpool.Pool) *UserProfileStore {
	return &UserProfileStore{db: db}
}

type UserProfile struct {
	Email     string
	Username  string
	Level     int
	CurrentXP int
	TotalXP   int
	HP        int
	MaxHP     int
	Strength  int
	Agility   int
	Mana      int
}

func (s *UserProfileStore) GetProfile(ctx context.Context, userID string) (UserProfile, error) {
	var p UserProfile
	err := s.db.QueryRow(ctx,
		`SELECT email, username, level, current_xp, total_xp, hp, max_hp, strength, agility, mana
		 FROM users WHERE id = $1`,
		userID,
	).Scan(&p.Email, &p.Username, &p.Level, &p.CurrentXP, &p.TotalXP,
		&p.HP, &p.MaxHP, &p.Strength, &p.Agility, &p.Mana)
	return p, err
}

type ProgressLog struct {
	EventType   string
	XPDelta     int
	LevelBefore int
	LevelAfter  int
	CreatedAt   time.Time
}

func (s *UserProfileStore) ListProgress(ctx context.Context, userID string, limit int) ([]ProgressLog, error) {
	rows, err := s.db.Query(ctx,
		`SELECT event_type, xp_delta, level_before, level_after, created_at
		 FROM progress_logs
		 WHERE user_id=$1
		 ORDER BY created_at DESC LIMIT $2`,
		userID, limit,
	)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	logs := []ProgressLog{}
	for rows.Next() {
		var l ProgressLog
		if err := rows.Scan(&l.EventType, &l.XPDelta, &l.LevelBefore, &l.LevelAfter, &l.CreatedAt); err != nil {
			continue
		}
		logs = append(logs, l)
	}
	return logs, rows.Err()
}

// ListEarnedAchievements returns achievement_key → earned_at for the user.
func (s *UserProfileStore) ListEarnedAchievements(ctx context.Context, userID string) (map[string]string, error) {
	rows, err := s.db.Query(ctx,
		`SELECT achievement_key, earned_at
		 FROM user_achievements
		 WHERE user_id=$1
		 ORDER BY earned_at DESC`,
		userID,
	)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	earned := map[string]string{}
	for rows.Next() {
		var key, earnedAt string
		if err := rows.Scan(&key, &earnedAt); err != nil {
			continue
		}
		earned[key] = earnedAt
	}
	return earned, rows.Err()
}
