package physique

type Exercise struct {
	Key         string
	Name        string
	MuscleGroup string
	Equipment   string // "none" | "dumbbell" | "barbell" | "machine" | "resistance_band"
	Category    string // "strength" | "cardio" | "flexibility" | "hiit"
	Goals       []string
	// Progression by fitness level
	Beginner     ExerciseSpec
	Intermediate ExerciseSpec
	Advanced     ExerciseSpec
}

type ExerciseSpec struct {
	Sets     int
	Reps     string // e.g. "8-12" or "30 sec"
	Rest     string // e.g. "60s"
	Notes    string
	XPReward int
}

var Library = []Exercise{
	// ── Lean Athletic ─────────────────────────────────────────────
	{
		Key: "bodyweight_squat", Name: "Bodyweight Squat",
		MuscleGroup: "legs", Equipment: "none", Category: "strength",
		Goals:        []string{"lean_athletic", "lose_fat", "maintain", "endurance"},
		Beginner:     ExerciseSpec{3, "10-15", "60s", "Keep chest up, knees over toes", 20},
		Intermediate: ExerciseSpec{4, "20-25", "45s", "Pause at bottom for 2 seconds", 30},
		Advanced:     ExerciseSpec{5, "30", "30s", "Add a jump at the top (jump squats)", 45},
	},
	{
		Key: "push_up", Name: "Push-Up",
		MuscleGroup: "chest/triceps", Equipment: "none", Category: "strength",
		Goals:        []string{"lean_athletic", "lose_fat", "maintain"},
		Beginner:     ExerciseSpec{3, "5-10", "60s", "Knee push-ups are fine", 20},
		Intermediate: ExerciseSpec{4, "15-20", "45s", "Full form, slow descent", 30},
		Advanced:     ExerciseSpec{5, "25-30", "30s", "Diamond push-ups or archer push-ups", 45},
	},
	{
		Key: "plank", Name: "Plank Hold",
		MuscleGroup: "core", Equipment: "none", Category: "strength",
		Goals:        []string{"lean_athletic", "lose_fat", "maintain", "endurance"},
		Beginner:     ExerciseSpec{3, "20 sec", "60s", "Hips level with shoulders", 15},
		Intermediate: ExerciseSpec{3, "45 sec", "45s", "Engage core, breathe steadily", 25},
		Advanced:     ExerciseSpec{4, "90 sec", "30s", "Side planks alternating", 40},
	},
	{
		Key: "mountain_climber", Name: "Mountain Climbers",
		MuscleGroup: "core/cardio", Equipment: "none", Category: "hiit",
		Goals:        []string{"lean_athletic", "lose_fat"},
		Beginner:     ExerciseSpec{3, "20 sec", "60s", "Slow and controlled", 20},
		Intermediate: ExerciseSpec{4, "30 sec", "45s", "Drive knees to chest", 30},
		Advanced:     ExerciseSpec{5, "45 sec", "30s", "Maximum pace", 45},
	},
	{
		Key: "burpee", Name: "Burpee",
		MuscleGroup: "full body", Equipment: "none", Category: "hiit",
		Goals:        []string{"lean_athletic", "lose_fat"},
		Beginner:     ExerciseSpec{3, "5", "90s", "No jump version is fine", 25},
		Intermediate: ExerciseSpec{4, "10", "60s", "Full burpee with jump", 40},
		Advanced:     ExerciseSpec{5, "15", "45s", "Burpee with pull-up if bar available", 55},
	},
	// ── Bulky Muscular ────────────────────────────────────────────
	{
		Key: "barbell_squat", Name: "Barbell Back Squat",
		MuscleGroup: "legs", Equipment: "barbell", Category: "strength",
		Goals:        []string{"bulky_muscular", "powerlifter"},
		Beginner:     ExerciseSpec{4, "8-10", "90s", "Start light — form over weight", 40},
		Intermediate: ExerciseSpec{5, "6-8", "120s", "Progressive overload each session", 55},
		Advanced:     ExerciseSpec{5, "4-6", "180s", "Work up to 80% 1RM", 70},
	},
	{
		Key: "bench_press", Name: "Bench Press",
		MuscleGroup: "chest", Equipment: "barbell", Category: "strength",
		Goals:        []string{"bulky_muscular", "powerlifter"},
		Beginner:     ExerciseSpec{4, "8-12", "90s", "Full range of motion", 40},
		Intermediate: ExerciseSpec{5, "6-8", "120s", "Control the descent", 55},
		Advanced:     ExerciseSpec{5, "3-5", "180s", "Spotter recommended at heavy weights", 70},
	},
	{
		Key: "deadlift", Name: "Deadlift",
		MuscleGroup: "back/legs", Equipment: "barbell", Category: "strength",
		Goals:        []string{"bulky_muscular", "powerlifter"},
		Beginner:     ExerciseSpec{3, "6-8", "120s", "Romanian deadlift with light weight", 45},
		Intermediate: ExerciseSpec{4, "5-6", "150s", "Conventional form, brace core", 60},
		Advanced:     ExerciseSpec{5, "3-5", "180s", "Work up to 85% 1RM", 80},
	},
	{
		Key: "pull_up", Name: "Pull-Up",
		MuscleGroup: "back/biceps", Equipment: "none", Category: "strength",
		Goals:        []string{"bulky_muscular", "lean_athletic"},
		Beginner:     ExerciseSpec{3, "3-5", "90s", "Assisted or band-assisted is fine", 30},
		Intermediate: ExerciseSpec{4, "8-10", "75s", "Full hang at bottom", 45},
		Advanced:     ExerciseSpec{5, "12-15", "60s", "Weighted pull-ups if available", 60},
	},
	{
		Key: "overhead_press", Name: "Overhead Press",
		MuscleGroup: "shoulders", Equipment: "barbell", Category: "strength",
		Goals:        []string{"bulky_muscular"},
		Beginner:     ExerciseSpec{3, "8-10", "90s", "Dumbbell OHP is a good substitute", 35},
		Intermediate: ExerciseSpec{4, "6-8", "120s", "Brace core, don't arch back", 50},
		Advanced:     ExerciseSpec{5, "4-6", "150s", "Push press for extra overload", 65},
	},
	// ── Endurance ─────────────────────────────────────────────────
	{
		Key: "jog_20min", Name: "20-Minute Steady Jog",
		MuscleGroup: "cardio", Equipment: "none", Category: "cardio",
		Goals:        []string{"endurance", "lose_fat"},
		Beginner:     ExerciseSpec{1, "15 min", "—", "Walk/run intervals are fine", 35},
		Intermediate: ExerciseSpec{1, "25 min", "—", "Maintain conversational pace", 50},
		Advanced:     ExerciseSpec{1, "40 min", "—", "Negative split — second half faster", 70},
	},
	{
		Key: "jump_rope", Name: "Jump Rope",
		MuscleGroup: "cardio", Equipment: "resistance_band", Category: "cardio",
		Goals:        []string{"endurance", "lean_athletic", "lose_fat"},
		Beginner:     ExerciseSpec{3, "45 sec", "60s", "Step-jumps if double-unders too hard", 20},
		Intermediate: ExerciseSpec{5, "60 sec", "45s", "Double-unders if possible", 35},
		Advanced:     ExerciseSpec{7, "90 sec", "30s", "Crossovers and speed work", 50},
	},
	{
		Key: "cycling_30min", Name: "Cycling 30 Minutes",
		MuscleGroup: "cardio/legs", Equipment: "none", Category: "cardio",
		Goals:        []string{"endurance", "lose_fat"},
		Beginner:     ExerciseSpec{1, "20 min", "—", "Flat terrain or light resistance", 30},
		Intermediate: ExerciseSpec{1, "35 min", "—", "Include a hill or resistance spike", 45},
		Advanced:     ExerciseSpec{1, "50 min", "—", "HIIT intervals every 5 minutes", 65},
	},
	// ── Lose Fat ──────────────────────────────────────────────────
	{
		Key: "hiit_circuit", Name: "HIIT Circuit",
		MuscleGroup: "full body", Equipment: "none", Category: "hiit",
		Goals:        []string{"lose_fat", "lean_athletic"},
		Beginner:     ExerciseSpec{3, "20 sec on / 40 sec off", "—", "Squat + push-up + jumping jack", 35},
		Intermediate: ExerciseSpec{4, "30 sec on / 30 sec off", "—", "Burpee + mountain climber + plank", 50},
		Advanced:     ExerciseSpec{5, "40 sec on / 20 sec off", "—", "Tabata protocol", 70},
	},
	{
		Key: "walking_brisk", Name: "Brisk Walk",
		MuscleGroup: "cardio", Equipment: "none", Category: "cardio",
		Goals:        []string{"lose_fat", "sedentary", "maintain"},
		Beginner:     ExerciseSpec{1, "20 min", "—", "5km/h pace, swing arms", 20},
		Intermediate: ExerciseSpec{1, "35 min", "—", "Include incline if possible", 30},
		Advanced:     ExerciseSpec{1, "50 min", "—", "Weighted vest or poles for intensity", 40},
	},
	// ── Powerlifter ───────────────────────────────────────────────
	{
		Key: "romanian_deadlift", Name: "Romanian Deadlift",
		MuscleGroup: "hamstrings/glutes", Equipment: "barbell", Category: "strength",
		Goals:        []string{"powerlifter", "bulky_muscular"},
		Beginner:     ExerciseSpec{4, "10-12", "90s", "Feel the hamstring stretch", 35},
		Intermediate: ExerciseSpec{4, "8-10", "120s", "Control the eccentric fully", 50},
		Advanced:     ExerciseSpec{5, "5-8", "150s", "Heavy — close to deadlift weight", 65},
	},
	{
		Key: "dips", Name: "Tricep Dips",
		MuscleGroup: "chest/triceps", Equipment: "none", Category: "strength",
		Goals:        []string{"bulky_muscular", "lean_athletic"},
		Beginner:     ExerciseSpec{3, "6-8", "90s", "Bench dips to start", 25},
		Intermediate: ExerciseSpec{4, "10-12", "75s", "Parallel bars if available", 40},
		Advanced:     ExerciseSpec{4, "15+", "60s", "Add weight belt for overload", 55},
	},
}

// GetExercisesForGoal returns exercises matching a body goal, scaled to fitness level.
func GetExercisesForGoal(bodyGoal, fitnessLevel string, count int) []map[string]any {
	var matching []Exercise
	for _, ex := range Library {
		for _, g := range ex.Goals {
			if g == bodyGoal {
				matching = append(matching, ex)
				break
			}
		}
	}

	// shuffle deterministically and take `count`
	if len(matching) > count {
		matching = matching[:count]
	}

	var result []map[string]any
	for _, ex := range matching {
		spec := ex.Beginner
		switch fitnessLevel {
		case "intermediate":
			spec = ex.Intermediate
		case "advanced":
			spec = ex.Advanced
		}
		result = append(result, map[string]any{
			"key":          ex.Key,
			"name":         ex.Name,
			"muscle_group": ex.MuscleGroup,
			"equipment":    ex.Equipment,
			"category":     ex.Category,
			"sets":         spec.Sets,
			"reps":         spec.Reps,
			"rest":         spec.Rest,
			"notes":        spec.Notes,
			"xp_reward":    spec.XPReward,
		})
	}
	return result
}
