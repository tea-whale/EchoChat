package com.example.echochat.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.echochat.data.local.dao.ChatDao
import com.example.echochat.data.local.entity.*

@Database(
    entities = [
        ProviderEntity::class,
        ModelEntity::class,
        AgentEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        MemoryEntity::class,
        MomentEntity::class,
        UserEntity::class,
        GroupEntity::class,
        GroupMemberCrossRef::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
}
