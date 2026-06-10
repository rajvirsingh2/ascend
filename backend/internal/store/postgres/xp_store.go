package postgres

import (
	"context"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
)

// XP and levels are applied synchronously by game.AwardXP — the single XP
// engine. This file only persists the long-term XP event history (which also
// drives activity streaks). The old UserStore.AwardXP (a second, conflicting
// leveling formula) was removed during the dedup pass.

type XPEventRow struct {
	UserID    string
	Amount    int
	Source    string
	SourceID  string
	CreatedAt time.Time
}

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
