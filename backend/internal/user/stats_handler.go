package user

import (
	"net/http"
	"time"

	"ascend-backend/internal/middleware"
	"ascend-backend/pkg/response"
)

type XpHistoryPoint struct {
	Date string `json:"date"`
	Xp   int    `json:"xp"`
}

type SkillCount struct {
	SkillArea string `json:"skill_area"`
	Count     int    `json:"count"`
}

type StatsResponse struct {
	TotalXP           int              `json:"total_xp"`
	Level             int              `json:"level"`
	TotalQuests       int              `json:"total_quests"`
	HabitsCompleted   int              `json:"habits_completed"`
	StreakFreezes     int              `json:"streak_freezes"`
	CurrentStreak     int              `json:"current_streak"`
	BestStreak        int              `json:"best_streak"`
	XpHistory         []XpHistoryPoint `json:"xp_history"`
	QuestDistribution []SkillCount     `json:"quest_distribution"`
	QuestsThisWeek    int              `json:"quests_this_week"`
	QuestsSkipped     int              `json:"quests_skipped"`
	OnTimePercentage  float64          `json:"on_time_percentage"`
}

func (h *Handler) GetStats(w http.ResponseWriter, r *http.Request) {
	userID := middleware.GetUserID(r)

	var stats StatsResponse
	stats.XpHistory = make([]XpHistoryPoint, 0)
	stats.QuestDistribution = make([]SkillCount, 0)

	// Get basic user stats (XP, level, streak freezes)
	err := h.db.QueryRow(r.Context(),
		`SELECT total_xp, level, streak_freezes FROM users WHERE id = $1`,
		userID,
	).Scan(&stats.TotalXP, &stats.Level, &stats.StreakFreezes)

	if err != nil {
		response.Error(w, http.StatusInternalServerError, "failed to get user stats")
		return
	}

	// Get total quests completed
	err = h.db.QueryRow(r.Context(),
		`SELECT COUNT(*) FROM quests WHERE user_id = $1 AND status = 'completed'`,
		userID,
	).Scan(&stats.TotalQuests)

	if err != nil {
		response.Error(w, http.StatusInternalServerError, "failed to get quest stats")
		return
	}

	// Get habits completed (approximate by sum of longest streaks)
	err = h.db.QueryRow(r.Context(),
		`SELECT COALESCE(SUM(longest_streak), 0) FROM habits WHERE user_id = $1`,
		userID,
	).Scan(&stats.HabitsCompleted)

	if err != nil {
		response.Error(w, http.StatusInternalServerError, "failed to get habit stats")
		return
	}

	// 1. Streaks — based on daily ACTIVITY (any quest/habit/physique
	// completion logs an xp_event), not just habits. Completing a quest
	// today counts toward the streak.
	var activityDays []time.Time
	dRows, err := h.db.Query(r.Context(),
		`SELECT DISTINCT (created_at AT TIME ZONE $2)::date AS d
		 FROM xp_events
		 WHERE user_id = $1
		 ORDER BY d DESC
		 LIMIT 400`,
		userID, h.loc.String(),
	)
	if err == nil {
		for dRows.Next() {
			var d time.Time
			if dRows.Scan(&d) == nil {
				activityDays = append(activityDays, d)
			}
		}
		dRows.Close()
	}
	now := time.Now().In(h.loc)
	today := time.Date(now.Year(), now.Month(), now.Day(), 0, 0, 0, 0, time.UTC)
	current, activityBest := CalcStreaks(activityDays, today)
	stats.CurrentStreak = current

	// best_streak keeps its old habit-based floor for users whose habit
	// history predates xp_events.
	var habitBest int
	_ = h.db.QueryRow(r.Context(),
		`SELECT COALESCE(MAX(longest_streak), 0) FROM habits WHERE user_id = $1`,
		userID,
	).Scan(&habitBest)
	stats.BestStreak = activityBest
	if habitBest > stats.BestStreak {
		stats.BestStreak = habitBest
	}

	// 2. XP History (last 30 days)
	rows, err := h.db.Query(r.Context(),
		`SELECT TO_CHAR(DATE(created_at), 'YYYY-MM-DD'), SUM(amount) 
		 FROM xp_events 
		 WHERE user_id = $1 AND created_at >= NOW() - INTERVAL '30 days' 
		 GROUP BY DATE(created_at) 
		 ORDER BY DATE(created_at)`,
		userID,
	)
	if err == nil {
		defer rows.Close()
		for rows.Next() {
			var pt XpHistoryPoint
			if err := rows.Scan(&pt.Date, &pt.Xp); err == nil {
				stats.XpHistory = append(stats.XpHistory, pt)
			}
		}
	}

	// 3. Quest Distribution
	qRows, err := h.db.Query(r.Context(),
		`SELECT COALESCE(skill_area, 'general'), COUNT(*) 
		 FROM quests 
		 WHERE user_id = $1 AND status = 'completed' 
		 GROUP BY skill_area`,
		userID,
	)
	if err == nil {
		defer qRows.Close()
		for qRows.Next() {
			var sc SkillCount
			if err := qRows.Scan(&sc.SkillArea, &sc.Count); err == nil {
				stats.QuestDistribution = append(stats.QuestDistribution, sc)
			}
		}
	}

	// 4. KPIs
	// Quests this week
	_ = h.db.QueryRow(r.Context(),
		`SELECT COUNT(*) FROM quests WHERE user_id = $1 AND status = 'completed' AND completed_at >= date_trunc('week', NOW())`,
		userID,
	).Scan(&stats.QuestsThisWeek)

	// Quests skipped total
	_ = h.db.QueryRow(r.Context(),
		`SELECT COUNT(*) FROM quests WHERE user_id = $1 AND status = 'skipped'`,
		userID,
	).Scan(&stats.QuestsSkipped)

	// On-time percentage calculation
	totalCompletedOrSkipped := stats.TotalQuests + stats.QuestsSkipped
	if totalCompletedOrSkipped > 0 {
		stats.OnTimePercentage = float64(stats.TotalQuests) * 100.0 / float64(totalCompletedOrSkipped)
	} else {
		stats.OnTimePercentage = 100.0 // default
	}

	response.JSON(w, http.StatusOK, stats)
}
