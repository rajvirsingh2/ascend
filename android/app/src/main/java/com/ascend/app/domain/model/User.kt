package com.ascend.app.domain.model

data class User(
    val id: String,
    val email: String,
    val username: String,
    val level: Int,
    val currentXp: Int,
    val xpToNext: Int,
    val avatarUrl: String?,
    val totalXp: Int=0,
    val hp: Int = 100,
    val maxHp: Int = 100
){
    val xpFraction: Float
        get() = when{
            xpToNext <= 0 -> 0f
            currentXp <= 0 -> 0f
            currentXp >= xpToNext -> 1f
            else -> currentXp.toFloat() / xpToNext.toFloat()
        }

    val rankTitle: String
        get() = when{
            level<5 -> "Novice"
            level<15 -> "Apprentice"
            level<25 -> "Hunter"
            level<40 -> "Shadow Walker"
            level<60 -> "Shadow Master"
            level<80 -> "Arcane Knight"
            level<100 -> "Supreme Knight"
            else -> "Shadow Monarch"
        }

    val classTitle: String
        get() = when {
            level < 5  -> "Class I"
            level < 15 -> "Class II"
            level < 30 -> "Class III"
            else       -> "Class IV"
        }
}