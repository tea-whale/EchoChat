package com.example.echochat.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "moments",
    foreignKeys = [
        ForeignKey(
            entity = AgentEntity::class,
            parentColumns = ["id"],
            childColumns = ["agentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["agentId"])]
)
data class MomentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val agentId: Long?, // Nullable: null means the User posted it
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)
