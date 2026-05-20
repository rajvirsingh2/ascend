package com.ascend.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SavePhysiqueRequest(
    val age: Int,
    val sex: String,
    @Json(name = "height_cm")        val heightCm: Float,
    @Json(name = "weight_kg")        val weightKg: Float,
    @Json(name = "target_weight_kg") val targetWeightKg: Float,
    @Json(name = "body_goal")        val bodyGoal: String,
    @Json(name = "activity_level")   val activityLevel: String,
    @Json(name = "fitness_level")    val fitnessLevel: String,
)

@JsonClass(generateAdapter = true)
data class PhysiqueResponse(
    val age: Int,
    val sex: String,
    @Json(name = "height_cm")       val heightCm: Float,
    @Json(name = "weight_kg")       val weightKg: Float,
    @Json(name = "body_goal")       val bodyGoal: String,
    @Json(name = "fitness_level")   val fitnessLevel: String,
    val bmi: Float,
    @Json(name = "bmi_category")    val bmiCategory: String,
    val bmr: Int,
    val tdee: Int,
    @Json(name = "goal_calories")   val goalCalories: Int,
)
