package com.example.echochat.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class OpenAIModelsResponse(
    val data: List<OpenAIModel>
)

@Serializable
data class OpenAIModel(
    val id: String, // 这就是模型名，如 "gpt-4"
    val created: Long? = null
)
