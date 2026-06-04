package com.ascend.app.domain.model

data class Achievement(
    val key: String,
    val title: String,
    val tag: String,
    val icon: String,
    val earned: Boolean,
    val earnedAt: String?
)
