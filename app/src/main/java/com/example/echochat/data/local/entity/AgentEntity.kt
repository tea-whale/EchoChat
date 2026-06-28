package com.example.echochat.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "agents",
    foreignKeys = [
        ForeignKey(
            entity = ModelEntity::class,
            parentColumns = ["id"],
            childColumns = ["modelId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["modelId"])]
)
data class AgentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val avatar: String?,
    val chatBackground: String? = null,
    val description: String,
    val systemPrompt: String,
    val modelId: Long?,
    val createdAt: Long = System.currentTimeMillis()
)
