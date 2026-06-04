package postgres

import (
	"context"
	"fmt"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
)

// ── Models ─────────────────────────────────────────────────────────────────

type XPEventRow struct {
	UserID    string
	Amount    int
	Source    string
	SourceID  string
	CreatedAt time.Time
}

// ── UserStore ──────────────────────────────────────────────────────────────

type UserStore struct {
	db *pgxpool.Pool
}

func NewUserStore(db *pgxpool.Pool) *UserStore {
	return &UserStore{db: db}
}

// ── AwardXP ────────────────────────────────────────────────────────────────
// Add this method to your existing postgres UserStore struct.

// xpThresholds maps level → total XP required.
// Level formula: each level needs level*150 XP.
func xpForLevel(level int) int {
	total := 0
	for l := 1; l < level; l++ {
		total += l * 150
	}
	return total
}

func levelFromXP(totalXP int) int {
	level := 1
	for xpForLevel(level+1) <= totalXP {
		level++
	}
	return level
}

// AwardXP atomically adds XP to the user and recalculates their level.
// Returns (newTotalXP, newLevel, didLevelUp, error).
func (s *UserStore) AwardXP(ctx context.Context, userID string, amount int) (int, int, bool, error) {
	tx, err := s.db.Begin(ctx)
	if err != nil {
		return 0, 0, false, err
	}
	defer func() { _ = tx.Rollback(ctx) }()

	var oldLevel, currentXP int
	err = tx.QueryRow(ctx,
		`SELECT total_xp, level FROM users WHERE id = $1 FOR UPDATE`,
		userID,
	).Scan(&currentXP, &oldLevel)
	if err != nil {
		return 0, 0, false, fmt.Errorf("select user: %w", err)
	}

	newXP := currentXP + amount
	newLevel := levelFromXP(newXP)
	didLevelUp := newLevel > oldLevel

	_, err = tx.Exec(ctx,
		`UPDATE users SET total_xp = $1, level = $2, updated_at = NOW() WHERE id = $3`,
		newXP, newLevel, userID,
	)
	if err != nil {
		return 0, 0, false, fmt.Errorf("update user xp: %w", err)
	}

	if err := tx.Commit(ctx); err != nil {
		return 0, 0, false, err
	}
	return newXP, newLevel, didLevelUp, nil
}

// DeductHP deducts HP from the user, clamped at 0.
// Returns the new HP value.
func (s *UserStore) DeductHP(ctx context.Context, userID string, amount int) (int, error) {
	var newHP int
	err := s.db.QueryRow(ctx,
		`UPDATE users
		 SET hp = GREATEST(0, hp - $1), updated_at = NOW()
		 WHERE id = $2
		 RETURNING hp`,
		amount, userID,
	).Scan(&newHP)
	if err != nil {
		return 0, fmt.Errorf("DeductHP: %w", err)
	}
	return newHP, nil
}

// ── XPEventStore ───────────────────────────────────────────────────────────

type XPEventStoreImpl struct {
	db *pgxpool.Pool
}

func NewXPEventStore(db *pgxpool.Pool) *XPEventStoreImpl {
	return &XPEventStoreImpl{db: db}
}

func (s *XPEventStoreImpl) Insert(ctx context.Context, row XPEventRow) error {
	_, err := s.db.Exec(ctx,
		`INSERT INTO xp_events (user_id, amount, source, source_id, created_at)
		 VALUES ($1, $2, $3, $4, $5)`,
		row.UserID, row.Amount, row.Source, row.SourceID, row.CreatedAt,
	)
	return err
}

func (s *XPEventStoreImpl) ListByUser(ctx context.Context, userID string, limit int) ([]XPEventRow, error) {
	rows, err := s.db.Query(ctx,
		`SELECT user_id, amount, source, source_id, created_at
		 FROM xp_events
		 WHERE user_id = $1
		 ORDER BY created_at DESC
		 LIMIT $2`,
		userID, limit,
	)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var events []XPEventRow
	for rows.Next() {
		var e XPEventRow
		if err := rows.Scan(&e.UserID, &e.Amount, &e.Source, &e.SourceID, &e.CreatedAt); err != nil {
			return nil, err
		}
		events = append(events, e)
	}
	return events, rows.Err()
}
