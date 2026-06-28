package com.example.echochat.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val avatar: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "group_members",
    primaryKeys = ["groupId", "agentId"]
)
data class GroupMemberCrossRef(
    val groupId: Long,
    val agentId: Long
)
