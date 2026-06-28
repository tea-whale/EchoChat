package com.example.echochat.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: Long = 1,
    val name: String,
    val avatar: String? = null
)
