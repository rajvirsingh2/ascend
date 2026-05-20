package physique

import "math"

// BMI categories
type BMICategory string

const (
	BMIUnderweight BMICategory = "underweight"
	BMINormal      BMICategory = "normal"
	BMIOverweight  BMICategory = "overweight"
	BMIObese       BMICategory = "obese"
)

type PhysiqueMetrics struct {
	BMI          float64
	BMICategory  BMICategory
	BMR          int // Basal Metabolic Rate (kcal/day)
	TDEE         int // Total Daily Energy Expenditure
	GoalCalories int
}

// CalculateBMI = weight(kg) / height(m)²
func CalculateBMI(weightKG, heightCM float64) float64 {
	heightM := heightCM / 100.0
	return math.Round((weightKG/(heightM*heightM))*10) / 10
}

func ClassifyBMI(bmi float64) BMICategory {
	switch {
	case bmi < 18.5:
		return BMIUnderweight
	case bmi < 25.0:
		return BMINormal
	case bmi < 30.0:
		return BMIOverweight
	default:
		return BMIObese
	}
}

// CalculateBMR uses Mifflin-St Jeor equation
func CalculateBMR(weightKG, heightCM float64, age int, sex string) int {
	base := 10*weightKG + 6.25*heightCM - 5*float64(age)
	if sex == "male" {
		return int(base + 5)
	}
	return int(base - 161)
}

var activityMultipliers = map[string]float64{
	"sedentary":   1.2,
	"light":       1.375,
	"moderate":    1.55,
	"active":      1.725,
	"very_active": 1.9,
}

func CalculateTDEE(bmr int, activityLevel string) int {
	mult := activityMultipliers[activityLevel]
	if mult == 0 {
		mult = 1.55
	}
	return int(float64(bmr) * mult)
}

// GoalCalories adjusts TDEE based on body goal
func GoalCalories(tdee int, bodyGoal string) int {
	switch bodyGoal {
	case "lose_fat":
		return tdee - 500 // 0.5kg/week deficit
	case "lean_athletic":
		return tdee - 200 // slight deficit
	case "maintain":
		return tdee
	case "bulky_muscular", "powerlifter":
		return tdee + 300 // lean bulk surplus
	case "endurance":
		return tdee + 100 // small surplus for recovery
	default:
		return tdee
	}
}

func Compute(weightKG, heightCM float64, age int, sex, activityLevel, bodyGoal string) PhysiqueMetrics {
	bmi := CalculateBMI(weightKG, heightCM)
	bmr := CalculateBMR(weightKG, heightCM, age, sex)
	tdee := CalculateTDEE(bmr, activityLevel)
	return PhysiqueMetrics{
		BMI:          bmi,
		BMICategory:  ClassifyBMI(bmi),
		BMR:          bmr,
		TDEE:         tdee,
		GoalCalories: GoalCalories(tdee, bodyGoal),
	}
}
