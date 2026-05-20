package notifications

import (
	"context"
	"log"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
)

// RunDailyReminder sends daily reminder push notifications to users
// who have incomplete quests/habits at a configured time each day.
// Runs as a goroutine — no external scheduler needed.
func RunDailyReminder(ctx context.Context, db *pgxpool.Pool, notifier *FCMNotifier) {
	if notifier == nil {
		log.Println("[reminder-worker] FCM not configured — reminders disabled")
		return
	}

	log.Println("[reminder-worker] starting")

	for {
		// Calculate time until next reminder (8:00 PM local server time).
		now := time.Now()
		next := time.Date(now.Year(), now.Month(), now.Day(), 20, 0, 0, 0, now.Location())
		if now.After(next) {
			// Already past 8 PM today — schedule for tomorrow.
			next = next.Add(24 * time.Hour)
		}

		sleepDuration := time.Until(next)
		log.Printf("[reminder-worker] next reminder at %v (in %v)", next.Format(time.RFC3339), sleepDuration.Round(time.Minute))

		select {
		case <-ctx.Done():
			log.Println("[reminder-worker] shutting down")
			return
		case <-time.After(sleepDuration):
		}

		// Fire reminders.
		if err := sendDailyReminders(ctx, notifier, db); err != nil {
			log.Printf("[reminder-worker] error: %v", err)
		}
	}
}

func sendDailyReminders(ctx context.Context, notifier *FCMNotifier, db *pgxpool.Pool) error {
	// Get all users who have at least one incomplete quest today.
	rows, err := db.Query(ctx, `SELECT DISTINCT user_id FROM quests WHERE status='active' AND type='daily'`)
	if err != nil {
		return err
	}
	defer rows.Close()

	var usersWithPendingQuests []string
	for rows.Next() {
		var uid string
		if err := rows.Scan(&uid); err == nil {
			usersWithPendingQuests = append(usersWithPendingQuests, uid)
		}
	}

	log.Printf("[reminder-worker] sending reminders to %d users", len(usersWithPendingQuests))

	for _, userID := range usersWithPendingQuests {
		// Non-blocking — fire and forget per user.
		go func(uid string) {
			if err := notifier.SendToUser(ctx, uid, Notification{
				Title: "⚔ Daily Quests Awaiting",
				Body:  "You have incomplete quests today. Complete them before midnight or lose HP!",
				Data:  map[string]string{"type": "daily_reminder", "action": "open_quests"},
			}); err != nil {
				log.Printf("[reminder-worker] send error (user=%s): %v", uid, err)
			}
		}(userID)
	}

	return nil
}
