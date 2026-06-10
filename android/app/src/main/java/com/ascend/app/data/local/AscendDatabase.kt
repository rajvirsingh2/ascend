package com.ascend.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ascend.app.data.local.dao.HabitDao
import com.ascend.app.data.local.dao.PendingOperationDao
import com.ascend.app.data.local.dao.QuestDao
import com.ascend.app.data.local.dao.UserDao
import com.ascend.app.data.local.entity.HabitEntity
import com.ascend.app.data.local.entity.PendingOperationEntity
import com.ascend.app.data.local.entity.QuestEntity
import com.ascend.app.data.local.entity.UserEntity

@Database(
    entities = [QuestEntity::class, HabitEntity::class, UserEntity::class, PendingOperationEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AscendDatabase: RoomDatabase(){
    abstract fun questDao(): QuestDao
    abstract fun habitDao(): HabitDao
    abstract fun userDao(): UserDao
    abstract fun pendingOperationDao(): PendingOperationDao
}