package mlservice

import "testing"

func TestCleanJSONString(t *testing.T) {
	cases := []struct{ name, in, want string }{
		{"plain", `[{"title":"x"}]`, `[{"title":"x"}]`},
		{"fenced", "```json\n[{\"title\":\"x\"}]\n```", `[{"title":"x"}]`},
		{"fenced no lang", "```\n[1]\n```", `[1]`},
		{"whitespace", "  [1]  ", `[1]`},
		{"unterminated fence", "```json\n[1]", `[1]`},
	}
	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			if got := cleanJSONString(c.in); got != c.want {
				t.Errorf("cleanJSONString(%q) = %q, want %q", c.in, got, c.want)
			}
		})
	}
}

func TestParseTruncatedJSONListValid(t *testing.T) {
	raw := `[{"title":"Run 5k","difficulty":"medium","xp_reward":120},
	         {"title":"Read 20 pages","difficulty":"easy","xp_reward":50}]`
	quests, err := parseTruncatedJSONList(raw)
	if err != nil {
		t.Fatal(err)
	}
	if len(quests) != 2 || quests[0].Title != "Run 5k" || quests[1].XPReward != 50 {
		t.Fatalf("parsed wrong: %+v", quests)
	}
}

func TestParseTruncatedJSONListFenced(t *testing.T) {
	raw := "```json\n[{\"title\":\"Meditate\",\"xp_reward\":30}]\n```"
	quests, err := parseTruncatedJSONList(raw)
	if err != nil {
		t.Fatal(err)
	}
	if len(quests) != 1 || quests[0].Title != "Meditate" {
		t.Fatalf("parsed wrong: %+v", quests)
	}
}

// LLM output cut off mid-object: salvage everything before the last complete '}'.
func TestParseTruncatedJSONListSalvagesTruncation(t *testing.T) {
	raw := `[{"title":"A","xp_reward":10},{"title":"B","xp_reward":20},{"title":"C","xp_rew`
	quests, err := parseTruncatedJSONList(raw)
	if err != nil {
		t.Fatal(err)
	}
	if len(quests) != 2 || quests[0].Title != "A" || quests[1].Title != "B" {
		t.Fatalf("salvage wrong: %+v", quests)
	}
}

func TestParseTruncatedJSONListGarbage(t *testing.T) {
	for _, raw := range []string{"", "not json at all", "[]", "[{", "null"} {
		if _, err := parseTruncatedJSONList(raw); err == nil {
			t.Errorf("expected error for %q", raw)
		}
	}
}
