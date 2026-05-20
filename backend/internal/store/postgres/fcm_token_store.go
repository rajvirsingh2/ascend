package postgres

import (
	"context"
	"database/sql"
)

// FCMTokenStore manages FCM device tokens in postgres.
type FCMTokenStore struct {
	db *sql.DB
}

func NewFCMTokenStore(db *sql.DB) *FCMTokenStore {
	return &FCMTokenStore{db: db}
}

// Upsert registers or refreshes a token for a user.
// One user can have multiple devices — each token is stored separately.
func (s *FCMTokenStore) Upsert(ctx context.Context, userID, token string) error {
	_, err := s.db.ExecContext(ctx, `
		INSERT INTO fcm_tokens (user_id, token, updated_at)
		VALUES ($1, $2, NOW())
		ON CONFLICT (token) DO UPDATE
		SET user_id = EXCLUDED.user_id, updated_at = NOW()
	`, userID, token)
	return err
}

// GetByUser returns all FCM tokens for a user.
func (s *FCMTokenStore) GetByUser(ctx context.Context, userID string) ([]string, error) {
	rows, err := s.db.QueryContext(ctx,
		`SELECT token FROM fcm_tokens WHERE user_id = $1`, userID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var tokens []string
	for rows.Next() {
		var t string
		if err := rows.Scan(&t); err != nil {
			return nil, err
		}
		tokens = append(tokens, t)
	}
	return tokens, rows.Err()
}

// Delete removes a stale or revoked token.
func (s *FCMTokenStore) Delete(ctx context.Context, token string) error {
	_, err := s.db.ExecContext(ctx,
		`DELETE FROM fcm_tokens WHERE token = $1`, token)
	return err
}

// DeleteByUser removes all tokens for a user (called on account deletion or logout).
func (s *FCMTokenStore) DeleteByUser(ctx context.Context, userID string) error {
	_, err := s.db.ExecContext(ctx,
		`DELETE FROM fcm_tokens WHERE user_id = $1`, userID)
	return err
}
