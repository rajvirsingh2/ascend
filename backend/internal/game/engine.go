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

type StatDelta struct {
	StatName string `json:"stat_name"`
	Before   int    `json:"before"`
	After    int    `json:"after"`
	Delta    int    `json:"delta"`
}

type XPResult struct {
	XPAwarded   int
	XPBefore    int
	XPAfter     int
	LevelBefore int
	LevelAfter  int
	LeveledUp   bool
	StatDeltas  []StatDelta
	HPRestored  int
	HPDamage    int
	HPAfter     int
	Died        bool
}

// AwardXP adds xpDelta to the user, handles level-ups, and writes a
// progress_log entry. Runs in a single transaction.
func AwardXP(ctx context.Context, db *pgxpool.Pool, userID, entityType, entityID, eventType, skillArea string, xpDelta int, hpDelta int) (*XPResult, error) {
	tx, err := db.Begin(ctx)
	if err != nil {
		return nil, err
	}
	defer func() { _ = tx.Rollback(ctx) }()

	var currentXP, level, str, agi, mana, hp, maxHp int
	err = tx.QueryRow(ctx,
		`SELECT current_xp, level, strength, agility, mana, hp, max_hp FROM users WHERE id = $1 FOR UPDATE`,
		userID,
	).Scan(&currentXP, &level, &str, &agi, &mana, &hp, &maxHp)
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

	var dStr, dAgi, dMana int
	if result.LeveledUp {
		switch skillArea {
		case "Physical":
			dStr = 3
			dAgi = 2
			dMana = 0
		case "Mental":
			dStr = 0
			dAgi = 1
			dMana = 4
		case "Social", "Finance":
			dStr = 1
			dAgi = 1
			dMana = 3
		default:
			dStr = 2
			dAgi = 2
			dMana = 1
		}

		result.StatDeltas = []StatDelta{
			{"STRENGTH", str, str + dStr, dStr},
			{"AGILITY", agi, agi + dAgi, dAgi},
			{"MANA", mana, mana + dMana, dMana},
		}
	}

	var hpRestored, hpDamage int
	var died bool
	newHp := hp + hpDelta
	if hpDelta > 0 {
		if newHp > maxHp {
			newHp = maxHp
		}
		hpRestored = newHp - hp
	} else if hpDelta < 0 {
		hpDamage = -hpDelta
	}

	if newHp <= 0 {
		died = true
		newHp = maxHp
		if newLevel > 1 {
			newLevel--
			newXP = 0
		}
		dStr -= 1
		dAgi -= 1
		dMana -= 1
		// Make sure they don't drop below 0 stats
		if str+dStr < 0 {
			dStr = -str
		}
		if agi+dAgi < 0 {
			dAgi = -agi
		}
		if mana+dMana < 0 {
			dMana = -mana
		}

		result.StatDeltas = []StatDelta{
			{"STRENGTH", str, str + dStr, dStr},
			{"AGILITY", agi, agi + dAgi, dAgi},
			{"MANA", mana, mana + dMana, dMana},
		}
	}

	result.HPRestored = hpRestored
	result.HPDamage = hpDamage
	result.HPAfter = newHp
	result.Died = died
	result.LevelAfter = newLevel
	result.XPAfter = newXP

	_, err = tx.Exec(ctx,
		`UPDATE users
		 SET current_xp = $1, level = $2, total_xp = total_xp + $3, updated_at = $4,
		 strength = strength + $5, agility = agility + $6, mana = mana + $7, hp = $8
		 WHERE id = $9`,
		newXP, newLevel, xpDelta, time.Now(), dStr, dAgi, dMana, newHp, userID,
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
