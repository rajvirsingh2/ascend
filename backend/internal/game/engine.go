package game

import (
	"context"
	"math"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
)

const xpBase = 100

// XPForLevel returns XP needed to reach the given level from the previous.
func XPForLevel(level int) int {
	return int(float64(xpBase) * math.Pow(float64(level), 1.5))
}

// QuestXPReward scales reward by difficulty (1-5) with diminishing returns at
// higher levels to prevent inflation.
func QuestXPReward(difficulty, userLevel int) int {
	base := difficulty * 25
	penalty := 1.0 - (float64(userLevel-1) * 0.02)
	if penalty < 0.4 {
		penalty = 0.4
	}
	return int(float64(base) * penalty)
}

type XPResult struct {
	XPAwarded   int
	XPBefore    int
	XPAfter     int
	LevelBefore int
	LevelAfter  int
	LeveledUp   bool
}

// AwardXP adds xpDelta to the user, handles level-ups, and writes a
// progress_log entry. Runs in a single transaction.
func AwardXP(ctx context.Context, db *pgxpool.Pool, userID, entityType, entityID, eventType string, xpDelta int) (*XPResult, error) {
	tx, err := db.Begin(ctx)
	if err != nil {
		return nil, err
	}
	defer tx.Rollback(ctx)

	var currentXP, level int
	err = tx.QueryRow(ctx,
		`SELECT current_xp, level FROM users WHERE id = $1 FOR UPDATE`,
		userID,
	).Scan(&currentXP, &level)
	if err != nil {
		return nil, err
	}

	result := &XPResult{
		XPAwarded:   xpDelta,
		XPBefore:    currentXP,
		LevelBefore: level,
	}

	newXP := currentXP + xpDelta
	newLevel := level

	for newXP >= XPForLevel(newLevel+1) {
		newXP -= XPForLevel(newLevel + 1)
		newLevel++
	}

	result.XPAfter = newXP
	result.LevelAfter = newLevel
	result.LeveledUp = newLevel > level

	_, err = tx.Exec(ctx,
		`UPDATE users
		 SET current_xp = $1, level = $2, total_xp = total_xp + $3, updated_at = $4
		 WHERE id = $5`,
		newXP, newLevel, xpDelta, time.Now(), userID,
	)
	if err != nil {
		return nil, err
	}

	_, err = tx.Exec(ctx,
		`INSERT INTO progress_logs
		   (id, user_id, entity_type, entity_id, event_type, xp_delta,
		    xp_before, xp_after, level_before, level_after, created_at)
		 VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11)`,
		uuid.NewString(), userID, entityType, entityID, eventType,
		xpDelta, currentXP, newXP, level, newLevel, time.Now(),
	)
	if err != nil {
		return nil, err
	}

	if err := tx.Commit(ctx); err != nil {
		return nil, err
	}

	return result, nil
}
