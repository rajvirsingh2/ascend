package com.ascend.app.di

import android.content.Context
import androidx.room.Room
import com.ascend.app.data.local.AscendDatabase
import com.ascend.app.data.local.dao.HabitDao
import com.ascend.app.data.local.dao.QuestDao
import com.ascend.app.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AscendDatabase=
        Room.databaseBuilder(context, AscendDatabase::class.java, "ascend.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideQuestDao(db: AscendDatabase): QuestDao = db.questDao()
    @Provides fun provideHabitDao(db: AscendDatabase): HabitDao = db.habitDao()
    @Provides fun provideUserDao(db: AscendDatabase): UserDao = db.userDao()

}