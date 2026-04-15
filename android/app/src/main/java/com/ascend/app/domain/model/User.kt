package com.ascend.app.domain.model

data class User(
    val id: String,
    val email: String,
    val username: String,
    val level: Int,
    val currentXp: Int,
    val xpToNext: Int,
    val avatarUrl: String?
){
    val xpFraction: Float
        get() = if (xpToNext == 0) 1f else currentXp.toFloat() / xpToNext.toFloat()
}