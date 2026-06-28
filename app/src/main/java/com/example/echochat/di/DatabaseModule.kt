package com.example.echochat.di

import android.content.Context
import androidx.room.Room
import com.example.echochat.data.local.AppDatabase
import com.example.echochat.data.local.dao.ChatDao
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "echochat.db"
        )
        .fallbackToDestructiveMigration() // For development simplicity
        .build()
    }

    @Provides
    fun provideChatDao(database: AppDatabase): ChatDao {
        return database.chatDao()
    }
}
