package workers

import (
	"context"
	"fmt"
	"log"
	"time"

	"ascend-backend/internal/models"
	"ascend-backend/internal/notifications"
	"ascend-backend/internal/store"
	"ascend-backend/internal/store/postgres"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
)

type QuestReminderWorkerConfig struct {
	DB       *pgxpool.Pool
	Notifier *notifications.FCMNotifier
}

// StartQuestReminderWorker runs in a goroutine and checks for expiring quests
func StartQuestReminderWorker(ctx context.Context, cfg QuestReminderWorkerConfig) {
	ticker := time.NewTicker(15 * time.Minute)
	defer ticker.Stop()

	log.Println("[quest-reminder-worker] started")

	questStore := postgres.NewQuestStore(cfg.DB, nil)
	notificationStore := postgres.NewNotificationStore(cfg.DB)

	for {
		select {
		case <-ticker.C:
			checkExpiringQuests(ctx, questStore, notificationStore, cfg.Notifier)
		case <-ctx.Done():
			log.Println("[quest-reminder-worker] stopped")
			return
		}
	}
}

func checkExpiringQuests(
	ctx context.Context,
	qs store.QuestStore,
	ns *postgres.NotificationStore,
	notifier *notifications.FCMNotifier,
) {
	// Look for quests expiring in the next 2 hours
	quests, err := qs.GetExpiringQuests(ctx, 2*time.Hour)
	if err != nil {
		log.Printf("[quest-reminder-worker] error getting expiring quests: %v", err)
		return
	}

	for _, q := range quests {
		// 1. Mark reminder sent first to prevent duplicate loops if subsequent steps fail
		if err := qs.MarkReminderSent(ctx, q.ID); err != nil {
			log.Printf("[quest-reminder-worker] error marking quest %s: %v", q.ID, err)
			continue
		}

		title := "Quest Expiring Soon!"
		body := fmt.Sprintf("Your quest '%s' expires soon. Complete it to avoid losing HP!", q.Title)

		// 2. Insert in-app notification
		notif := &models.Notification{
			ID:        uuid.New().String(),
			UserID:    q.UserID,
			Type:      "SYSTEM",
			Title:     title,
			Body:      body,
			IsRead:    false,
			CreatedAt: time.Now().UTC(),
		}
		if err := ns.Insert(ctx, notif); err != nil {
			log.Printf("[quest-reminder-worker] error inserting notification: %v", err)
		}

		// 3. Send Push Notification via FCM
		if notifier != nil {
			_ = notifier.SendToUser(ctx, q.UserID, notifications.Notification{
				Title: title,
				Body:  body,
				Data: map[string]string{
					"type": "quest_expiring",
					"id":   q.ID,
				},
			})
		}

		log.Printf("[quest-reminder-worker] sent reminder for quest %s to user %s", q.ID, q.UserID)
	}
}
