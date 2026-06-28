package com.example.echochat.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["conversationId"])]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long,
    val senderId: Long? = null, // null for user, agentId for agents
    val role: String, // "user", "assistant", "system", "tool"
    val content: String,
    val toolCallId: String? = null,
    val toolCallsJson: String? = null, // JSON string of List<ToolCall>
    val createdAt: Long = System.currentTimeMillis()
)
