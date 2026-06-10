package workers

import "testing"

func TestSeededQuestsAreValid(t *testing.T) {
	if len(seededDailyQuests) == 0 {
		t.Fatal("seeded quest list must never be empty — it is the midnight fallback")
	}
	seen := map[string]bool{}
	for _, q := range seededDailyQuests {
		if q.Title == "" || q.Description == "" || q.SkillArea == "" {
			t.Errorf("seeded quest has empty fields: %+v", q)
		}
		if q.Difficulty < 1 || q.Difficulty > 5 {
			t.Errorf("seeded quest %q difficulty out of range: %d", q.Title, q.Difficulty)
		}
		if q.XPReward <= 0 {
			t.Errorf("seeded quest %q has non-positive XP", q.Title)
		}
		if seen[q.Title] {
			t.Errorf("duplicate seeded quest title %q", q.Title)
		}
		seen[q.Title] = true
	}
}
