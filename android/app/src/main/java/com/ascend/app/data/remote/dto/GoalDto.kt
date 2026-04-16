package com.ascend.app.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GoalResponse(
    val id: String,
    val title: String,
    val description: String,
    @Json(name = "skill_area") val skillArea: String,
    val priority: Int,
    val status: String,
    val progress: Int
)

@JsonClass(generateAdapter = true)
data class CreateGoalRequest(
    val title: String,
    val description: String,
    @Json(name = "skill_area") val skillArea: String,
    val priority: Int
)

fun GoalResponse.toDomain() = com.ascend.app.domain.model.Goal(
    id = id, title = title, description = description,
    skillArea = skillArea, priority = priority,
    status = status, progress = progress
)