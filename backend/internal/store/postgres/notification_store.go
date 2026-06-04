package postgres

import (
	"context"

	"ascend-backend/internal/models"

	"github.com/jackc/pgx/v5/pgxpool"
)

type NotificationStore struct {
	db *pgxpool.Pool
}

func NewNotificationStore(db *pgxpool.Pool) *NotificationStore {
	return &NotificationStore{db: db}
}

func (s *NotificationStore) Insert(ctx context.Context, item *models.Notification) error {
	_, err := s.db.Exec(ctx, `
		INSERT INTO notifications (id, user_id, type, title, body, xp_delta, action_route, is_read, created_at)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
	`, item.ID, item.UserID, item.Type, item.Title, item.Body, item.XPDelta, item.ActionRoute, item.IsRead, item.CreatedAt)
	return err
}

func (s *NotificationStore) GetByUser(ctx context.Context, userID string) ([]*models.Notification, error) {
	rows, err := s.db.Query(ctx, `
		SELECT id, user_id, type, title, body, xp_delta, action_route, is_read, created_at
		FROM notifications
		WHERE user_id = $1
		ORDER BY created_at DESC
		LIMIT 100
	`, userID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var notifs []*models.Notification
	for rows.Next() {
		var n models.Notification
		if err := rows.Scan(
			&n.ID, &n.UserID, &n.Type, &n.Title, &n.Body,
			&n.XPDelta, &n.ActionRoute, &n.IsRead, &n.CreatedAt,
		); err != nil {
			return nil, err
		}
		notifs = append(notifs, &n)
	}
	return notifs, rows.Err()
}

func (s *NotificationStore) MarkRead(ctx context.Context, id string, userID string) error {
	_, err := s.db.Exec(ctx, `
		UPDATE notifications SET is_read = true
		WHERE id = $1 AND user_id = $2
	`, id, userID)
	return err
}

func (s *NotificationStore) MarkAllRead(ctx context.Context, userID string) error {
	_, err := s.db.Exec(ctx, `
		UPDATE notifications SET is_read = true
		WHERE user_id = $1
	`, userID)
	return err
}

func (s *NotificationStore) Delete(ctx context.Context, id string, userID string) error {
	_, err := s.db.Exec(ctx, `
		DELETE FROM notifications
		WHERE id = $1 AND user_id = $2
	`, id, userID)
	return err
}

func (s *NotificationStore) DeleteAll(ctx context.Context, userID string) error {
	_, err := s.db.Exec(ctx, `
		DELETE FROM notifications
		WHERE user_id = $1
	`, userID)
	return err
}
