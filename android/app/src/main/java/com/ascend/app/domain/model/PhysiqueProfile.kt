package com.ascend.app.domain.model

data class PhysiqueProfile(
    val age: Int = 0,
    val sex: String = "male",
    val heightCm: Float = 170f,
    val weightKg: Float = 70f,
    val targetWeightKg: Float = 70f,
    val bodyGoal: String = "lean_athletic",
    val activityLevel: String = "moderate",
    val fitnessLevel: String = "beginner",
    val bmi: Float = 0f,
    val bmiCategory: String = "",
    val bmr: Int = 0,
    val tdee: Int = 0,
    val goalCalories: Int = 0
)

val bodyGoalOptions = listOf(
    BodyGoalOption("lean_athletic",  "Lean & Athletic",
        "Defined muscles, low body fat, functional strength",
        "Similar to a swimmer or martial artist"),
    BodyGoalOption("bulky_muscular", "Bulky & Muscular",
        "Maximum muscle mass, visible bulk",
        "Similar to a bodybuilder"),
    BodyGoalOption("powerlifter",    "Powerlifter",
        "Raw strength above aesthetics",
        "Similar to a powerlifting competitor"),
    BodyGoalOption("endurance",      "Endurance",
        "Cardiovascular fitness, stamina, lean",
        "Similar to a marathon runner"),
    BodyGoalOption("lose_fat",       "Lose Fat",
        "Reduce body fat, improve health markers",
        "Cut phase — calorie deficit + cardio"),
    BodyGoalOption("maintain",       "Maintain",
        "Keep current physique, build healthy habits",
        "Maintenance calories, balanced training"),
)

data class BodyGoalOption(
    val key: String,
    val title: String,
    val description: String,
    val comparison: String
)

val activityOptions = listOf(
    "sedentary"   to "Sedentary (desk job, no exercise)",
    "light"       to "Light (1-3 days/week exercise)",
    "moderate"    to "Moderate (3-5 days/week exercise)",
    "active"      to "Active (6-7 days/week exercise)",
    "very_active" to "Very Active (2x/day training)"
)