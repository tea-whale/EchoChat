package com.example.echochat.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "likes",
    foreignKeys = [
        ForeignKey(
            entity = MomentEntity::class,
            parentColumns = ["id"],
            childColumns = ["momentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["momentId"])]
)
data class LikeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val momentId: Long,
    val senderId: Long?, // null for user, agentId for agents
    val senderName: String,
    val createdAt: Long = System.currentTimeMillis()
)
