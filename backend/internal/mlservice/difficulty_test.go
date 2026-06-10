package mlservice

import "testing"

func TestMapDifficulty(t *testing.T) {
	cases := map[string]int{
		"easy": 1, "Easy": 1, "1": 1,
		"medium": 2, "Medium": 2, "2": 2,
		"hard": 3, "Hard": 3,
		"expert": 4, "Epic": 4, "epic": 4,
		"legendary": 5, "Legendary": 5,
		"":        1,
		"unknown": 1,
		" hard ":  3,
	}
	for in, want := range cases {
		if got := MapDifficulty(in); got != want {
			t.Errorf("MapDifficulty(%q) = %d, want %d", in, got, want)
		}
	}
}
