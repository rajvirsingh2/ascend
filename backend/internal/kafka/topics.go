package kafka

import (
	"context"
	"encoding/json"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
)

// WriteOutbox writes the event to the Postgres outbox table atomically
// with the business operation. Call inside the same transaction.
func WriteOutbox(ctx context.Context, tx pgxTx, e Event) error {
	data, _ := json.Marshal(e)
	_, err := tx.Exec(ctx,
		`INSERT INTO event_outbox (id, user_id, topic, payload, status, created_at)
         VALUES ($1, $2, $3, $4, 'pending', $5)`,
		uuid.NewString(), e.UserID, e.Type, data, time.Now(),
	)
	return err
}

// OutboxReplayer polls the outbox for pending events and re-publishes them.
// Runs as a background goroutine — recovers from Kafka downtime.
func OutboxReplayer(ctx context.Context, db *pgxpool.Pool, p *Producer) {
	ticker := time.NewTicker(10 * time.Second)
	defer ticker.Stop()
	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
			rows, err := db.Query(ctx,
				`SELECT id, topic, payload FROM event_outbox
                 WHERE status='pending' AND created_at < NOW() - INTERVAL '5 seconds'
                 ORDER BY created_at ASC LIMIT 50`,
			)
			if err != nil {
				continue
			}
			for rows.Next() {
				var id, topic string
				var payload []byte
				if err := rows.Scan(&id, &topic, &payload); err != nil {
					continue
				}
				var e Event
				if err := json.Unmarshal(payload, &e); err != nil {
					continue
				}
				if err := p.Publish(ctx, topic, e); err == nil {
					db.Exec(ctx,
						`UPDATE event_outbox SET status='published' WHERE id=$1`, id)
				}
			}
			rows.Close()
		}
	}
}

type pgxTx interface {
	Exec(ctx context.Context, sql string, args ...any) (interface{}, error)
}
