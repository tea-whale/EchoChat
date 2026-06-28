package com.example.echochat.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = false,
    val tools: List<Tool>? = null,
    @SerialName("tool_choice") val toolChoice: String? = null
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String? = null,
    val name: String? = null,
    @SerialName("tool_calls") val toolCalls: List<ToolCall>? = null,
    @SerialName("tool_call_id") val toolCallId: String? = null
)

@Serializable
data class Tool(
    val type: String = "function",
    val function: FunctionDef
)

@Serializable
data class FunctionDef(
    val name: String,
    val description: String,
    val parameters: JsonElement // JSON Schema
)

@Serializable
data class ToolCall(
    val id: String,
    val type: String = "function",
    val function: FunctionCall
)

@Serializable
data class FunctionCall(
    val name: String,
    val arguments: String // JSON string
)

@Serializable
data class ChatResponse(
    val choices: List<ChatChoice>
)

@Serializable
data class ChatChoice(
    val message: ChatMessage,
    @SerialName("finish_reason") val finishReason: String? = null
)
