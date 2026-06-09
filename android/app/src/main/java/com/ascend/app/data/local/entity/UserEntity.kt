package com.ascend.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val username: String,
    val level: Int,
    val currentXp: Int,
    val xpToNext: Int,
    val avatarUrl: String?,
    val strength: Int = 0,
    val agility: Int = 0,
    val mana: Int = 0,
    val hp: Int = 100,
    val maxHp: Int = 100,
    val cachedAt: Long = System.currentTimeMillis()
)