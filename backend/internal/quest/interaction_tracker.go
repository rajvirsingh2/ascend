package quest

import (
	"context"
	"encoding/json"

	"github.com/jackc/pgx/v5/pgxpool"
)

type InteractionTracker struct {
	db *pgxpool.Pool
}

func NewInteractionTracker(db *pgxpool.Pool) *InteractionTracker {
	return &InteractionTracker{db: db}
}

// LogShown — called right after quests are generated and shown to user
func (t *InteractionTracker) LogShown(
	ctx context.Context,
	userID string,
	questID string,
	profileSnapshot any,
	questData any,
	modelVersion string,
) error {
	profileJSON, _ := json.Marshal(profileSnapshot)
	questJSON, _ := json.Marshal(questData)

	_, err := t.db.Exec(ctx, `
		INSERT INTO quest_interactions
		(user_id, quest_id, profile_snapshot, quest_data, outcome, model_version)
		VALUES ($1, $2, $3, $4, 'pending', $5)
	`, userID, questID, profileJSON, questJSON, modelVersion)
	return err
}

// LogOutcome — called when user completes, skips, or abandons a quest
func (t *InteractionTracker) LogOutcome(
	ctx context.Context,
	questID string,
	outcome string, // 'completed' | 'skipped' | 'abandoned'
) error {
	_, err := t.db.Exec(ctx, `
		UPDATE quest_interactions
		SET outcome         = $1,
		    outcome_at      = NOW(),
		    time_to_outcome = NOW() - shown_at
		WHERE quest_id = $2 AND outcome = 'pending'
	`, outcome, questID)
	return err
}

// Auto-abandon stale pending interactions (run hourly via cron)
func (t *InteractionTracker) AbandonStale(ctx context.Context) error {
	_, err := t.db.Exec(ctx, `
		UPDATE quest_interactions
		SET outcome = 'abandoned', outcome_at = NOW(),
		    time_to_outcome = NOW() - shown_at
		WHERE outcome = 'pending'
		  AND shown_at < NOW() - INTERVAL '3 days'
	`)
	return err
}

// CountReadyPairs — checks if enough preference data exists to retrain
func (t *InteractionTracker) CountReadyPairs(ctx context.Context) (int, error) {
	var count int
	err := t.db.QueryRow(ctx, `
		SELECT COUNT(*) FROM dpo_preference_pairs
	`).Scan(&count)
	return count, err
}
