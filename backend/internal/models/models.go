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
	LastCompletedAt *time.Time `json:"last_completed_at,omitempty"`
	IsActive        bool       `json:"is_active"`
	CreatedAt       time.Time  `json:"created_at"`
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
}
