package interests

import (
	"context"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
)

// UserInterest is a single interest row in the DB.
type UserInterest struct {
	ID          string    `json:"id"`
	UserID      string    `json:"user_id"`
	Category    string    `json:"category"`
	Subcategory string    `json:"subcategory"`
	CustomGoal  string    `json:"custom_goal"`
	Priority    int       `json:"priority"`
	Proficiency string    `json:"proficiency"`
	CreatedAt   time.Time `json:"created_at"`
}

// Store handles persistence of user interests.
type Store struct {
	db *pgxpool.Pool
}

func NewStore(db *pgxpool.Pool) *Store {
	return &Store{db: db}
}

// Save replaces all interests for a user atomically.
func (s *Store) Save(ctx context.Context, userID string, interests []UserInterest) error {
	tx, err := s.db.Begin(ctx)
	if err != nil {
		return err
	}
	defer func() { _ = tx.Rollback(ctx) }()

	// Delete existing interests for this user.
	if _, err := tx.Exec(ctx,
		`DELETE FROM user_interests WHERE user_id = $1`, userID,
	); err != nil {
		return err
	}

	// Insert new interests.
	for _, interest := range interests {
		if _, err := tx.Exec(ctx, `
			INSERT INTO user_interests (user_id, category, subcategory, custom_goal, priority, proficiency, updated_at)
			VALUES ($1, $2, $3, $4, $5, $6, NOW())
		`, userID, interest.Category, interest.Subcategory, interest.CustomGoal, interest.Priority, interest.Proficiency,
		); err != nil {
			return err
		}
	}

	// Mark user as having configured interests.
	if _, err := tx.Exec(ctx,
		`UPDATE users SET interests_configured = TRUE, updated_at = NOW() WHERE id = $1`,
		userID,
	); err != nil {
		return err
	}

	return tx.Commit(ctx)
}

// GetByUser returns all interests for a user, ordered by priority.
func (s *Store) GetByUser(ctx context.Context, userID string) ([]UserInterest, error) {
	rows, err := s.db.Query(ctx, `
		SELECT id, user_id, category, subcategory, custom_goal, priority, proficiency, created_at
		FROM user_interests
		WHERE user_id = $1
		ORDER BY priority ASC, category ASC
	`, userID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var result []UserInterest
	for rows.Next() {
		var i UserInterest
		if err := rows.Scan(&i.ID, &i.UserID, &i.Category, &i.Subcategory,
			&i.CustomGoal, &i.Priority, &i.Proficiency, &i.CreatedAt); err != nil {
			return nil, err
		}
		result = append(result, i)
	}
	return result, rows.Err()
}

// IsConfigured returns whether the user has completed interest onboarding.
func (s *Store) IsConfigured(ctx context.Context, userID string) (bool, error) {
	var configured bool
	err := s.db.QueryRow(ctx,
		`SELECT interests_configured FROM users WHERE id = $1`, userID,
	).Scan(&configured)
	return configured, err
}

// BuildRAGContext converts a user's interests into a prompt section for the RAG service.
// This is injected into every quest generation request.
func BuildRAGContext(interests []UserInterest) string {
	if len(interests) == 0 {
		return ""
	}

	// Build a lookup for quest hints.
	hintMap := map[string]string{}
	for _, cat := range AllCategories {
		for _, sub := range cat.Subcategories {
			hintMap[cat.ID+"/"+sub.ID] = sub.QuestHints
		}
	}

	ctx := "## User Interests & Focus Areas\n\n"
	ctx += "The user has configured the following areas they want to improve. "
	ctx += "ALL quests MUST be relevant to at least one of these areas. "
	ctx += "Distribute quests proportionally across the user's PRIMARY interests first.\n\n"

	for _, interest := range interests {
		priorityLabel := map[int]string{1: "PRIMARY", 2: "SECONDARY", 3: "OPTIONAL"}[interest.Priority]
		if priorityLabel == "" {
			priorityLabel = "SECONDARY"
		}

		ctx += "### [" + priorityLabel + "] " + interest.Category
		if interest.Subcategory != "" {
			ctx += " → " + interest.Subcategory
		}
		ctx += "\n"

		// Add quest generation hints for this subcategory.
		key := interest.Category + "/" + interest.Subcategory
		if hint, ok := hintMap[key]; ok {
			ctx += "Quest guidance: " + hint + "\n"
		}

		// Add user's own custom goal if provided.
		if interest.CustomGoal != "" {
			ctx += "User's own goal: \"" + interest.CustomGoal + "\"\n"
		}

		if interest.Proficiency != "" {
			ctx += "User's Proficiency in this area: " + interest.Proficiency + "\n"
		}
		ctx += "\n"
	}

	ctx += "---\n"
	ctx += "Generate a balanced mix of quests across these areas. "
	ctx += "Make quests specific, actionable, and achievable within 1 day. "
	ctx += "Each quest must have a clear completion criteria.\n"
	ctx += "\nCRITICAL RULES:\n"
	ctx += "1. ONLY generate quests from the categories listed above. Do NOT generate quests outside these areas.\n"
	ctx += "2. Do NOT generate any physical, fitness, exercise, or health quests unless the category 'physical' explicitly appears in the user's interests list above.\n"
	ctx += "3. Calibrate quest difficulty to the user's stated proficiency level for each area (Beginner = foundational tasks, Intermediate = growth challenges, Expert = mastery-level tasks).\n"
	ctx += "4. Every single quest MUST map directly to one of the listed interest categories.\n"

	return ctx
}
