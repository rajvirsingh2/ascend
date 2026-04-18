package store

import (
	"context"

	"ascend-backend/internal/game"

	"ascend-backend/internal/models"
)

type GoalStore interface {
	Create(ctx context.Context, goal *models.Goal) error
	ListByUser(ctx context.Context, userID string) ([]*models.Goal, error)
	GetByID(ctx context.Context, id, userID string) (*models.Goal, error)
	Update(ctx context.Context, goal *models.Goal) error
	Delete(ctx context.Context, id, userID string) error
}

type HabitStore interface {
	Create(ctx context.Context, habit *models.Habit) error
	ListByUser(ctx context.Context, userID string) ([]*models.Habit, error)
	GetByID(ctx context.Context, id, userID string) (*models.Habit, error)
	Complete(ctx context.Context, id, userID string) (*game.XPResult, error)
}

type QuestStore interface {
	ListActive(ctx context.Context, userID string) ([]*models.Quest, error)
	GetByID(ctx context.Context, id, userID string) (*models.Quest, error)
	Complete(ctx context.Context, id, userID string) (*game.XPResult, error)
	Skip(ctx context.Context, id, userID string) error
	ExpireOld(ctx context.Context) error
}
