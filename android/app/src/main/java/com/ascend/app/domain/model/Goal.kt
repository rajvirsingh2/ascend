package com.ascend.app.domain.model

data class Goal(
    val id: String,
    val title: String,
    val description: String,
    val skillArea: String,
    val priority: Int,
    val status: String,
    val progress: Int
)