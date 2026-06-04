package models

import (
	"database/sql"
	"time"
)

type Goal struct {
	ID          string         `json:"id"`
	UserID      string         `json:"user_id"`
	Title       string         `json:"title"`
	Description sql.NullString `json:"description"`
	Category    sql.NullString `json:"category"`
	SkillArea   string         `json:"skill_area"`
	Priority    int            `json:"priority"`
	TargetDate  *time.Time     `json:"target_date,omitempty"`
	Status      string         `json:"status"`
	Progress    int            `json:"progress"`
	CreatedAt   time.Time      `json:"created_at"`
	CompletedAt *time.Time     `json:"completed_at,omitempty"`
}

type Habit struct {
	ID              string     `json:"id"`
	UserID          string     `json:"user_id"`
	GoalID          *string    `json:"goal_id,omitempty"`
	Title           string     `json:"title"`
	Frequency       string     `json:"frequency"`
	XPReward        int        `json:"xp_reward"`
	CurrentStreak   int        `json:"current_streak"`
	LongestStreak   int        `json:"longest_streak"`
	LastCompletedAt *time.Time `json:"last_completed_at"`
	IsActive        bool       `json:"is_active"`
	CreatedAt       time.Time  `json:"created_at"`
}

type Achievement struct {
	ID        string    `json:"id"`
	UserID    string    `json:"user_id"`
	Key       string    `json:"key"`
	Title     string    `json:"title"`
	Tag       string    `json:"tag"`
	Icon      string    `json:"icon"`
	Earned    bool      `json:"earned"`
	EarnedAt  time.Time `json:"earned_at"`
	CreatedAt time.Time `json:"created_at"`
}

type Notification struct {
	ID          string    `json:"id"`
	UserID      string    `json:"user_id"`
	Type        string    `json:"type"`
	Title       string    `json:"title"`
	Body        string    `json:"body"`
	XPDelta     *int      `json:"xp_delta,omitempty"`
	ActionRoute *string   `json:"action_route,omitempty"`
	IsRead      bool      `json:"is_read"`
	CreatedAt   time.Time `json:"created_at"`
}

type Quest struct {
	ID            string     `json:"id"`
	UserID        string     `json:"user_id"`
	GoalID        *string    `json:"goal_id,omitempty"`
	Title         string     `json:"title"`
	Description   string     `json:"description"`
	Type          string     `json:"type"`
	Difficulty    int        `json:"difficulty"`
	XPReward      int        `json:"xp_reward"`
	Status        string     `json:"status"`
	IsAIGenerated bool       `json:"is_ai_generated"`
	SkillArea     string     `json:"skill_area"`
	ExpiresAt     *time.Time `json:"expires_at,omitempty"`
	CompletedAt   *time.Time `json:"completed_at,omitempty"`
	CreatedAt     time.Time  `json:"created_at"`
	ReminderSent  bool       `json:"reminder_sent"`
}

type HeatmapPoint struct {
	Date  string `json:"date"`
	Count int    `json:"count"`
}
