package com.example.echochat.data.repository

import com.example.echochat.data.local.dao.ChatDao
import com.example.echochat.data.local.dao.ChatListItem
import com.example.echochat.data.local.entity.*
import com.example.echochat.data.remote.ApiService
import com.example.echochat.data.remote.dto.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val chatDao: ChatDao,
    private val apiService: ApiService,
    private val json: Json
) {
    private val AVAILABLE_TOOLS = listOf(
        Tool(
            type = "function",
            function = FunctionDef(
                name = "create_memory", 
                description = "记录关于用户的长期记忆，以便在以后的对话中记住。", 
                parameters = buildJsonObject {
                    put("type", "object")
                    putJsonObject("properties") { 
                        putJsonObject("content") { 
                            put("type", "string")
                            put("description", "记忆的具体内容")
                        } 
                    }
                    putJsonArray("required") { add("content") }
                }
            )
        ),
        Tool(
            type = "function",
            function = FunctionDef(
                name = "post_moment", 
                description = "在朋友圈发布一条动态。", 
                parameters = buildJsonObject {
                    put("type", "object")
                    putJsonObject("properties") { 
                        putJsonObject("content") { 
                            put("type", "string")
                            put("description", "朋友圈的正文内容")
                        } 
                    }
                    putJsonArray("required") { add("content") }
                }
            )
        ),
        Tool(
            type = "function",
            function = FunctionDef(
                name = "get_moments_feed",
                description = "浏览朋友圈最近的动态列表，获取动态内容和ID以便进行评论或点赞。",
                parameters = buildJsonObject {
                    put("type", "object")
                    putJsonObject("properties") {
                        put("limit", buildJsonObject { put("type", "integer"); put("description", "获取动态的数量，默认10") })
                    }
                }
            )
        ),
        Tool(
            type = "function",
            function = FunctionDef(
                name = "comment_moment",
                description = "评论一条朋友圈动态。",
                parameters = buildJsonObject {
                    put("type", "object")
                    putJsonObject("properties") {
                        put("momentId", buildJsonObject { put("type", "integer"); put("description", "动态的ID") })
                        put("content", buildJsonObject { put("type", "string"); put("description", "评论内容") })
                    }
                    putJsonArray("required") { add("momentId"); add("content") }
                }
            )
        ),
        Tool(
            type = "function",
            function = FunctionDef(
                name = "like_moment",
                description = "给一条朋友圈动态点赞。",
                parameters = buildJsonObject {
                    put("type", "object")
                    putJsonObject("properties") {
                        put("momentId", buildJsonObject { put("type", "integer"); put("description", "动态的ID") })
                    }
                    putJsonArray("required") { add("momentId") }
                }
            )
        ),
        Tool(
            type = "function",
            function = FunctionDef(
                name = "get_moment_interactions",
                description = "查看某条朋友圈的详细点赞和评论信息。",
                parameters = buildJsonObject {
                    put("type", "object")
                    putJsonObject("properties") {
                        put("momentId", buildJsonObject { put("type", "integer"); put("description", "动态的ID") })
                    }
                    putJsonArray("required") { add("momentId") }
                }
            )
        )
    )

    fun getChatList(): Flow<List<ChatListItem>> = chatDao.getChatList()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getMessagesByConversationId(conversationId: Long): Flow<List<MessageEntity>> {
        return chatDao.getMessagesByConversation(conversationId).distinctUntilChanged()
    }

    suspend fun getOrCreateLatestConversation(agentId: Long?, groupId: Long?): Long {
        val existing = if (groupId != null) chatDao.getLatestConversationByGroupId(groupId)
                      else chatDao.getLatestConversationByAgentId(agentId ?: 0L)
        return existing?.id ?: chatDao.insertConversation(ConversationEntity(agentId = agentId, groupId = groupId))
    }

    suspend fun createNewConversation(agentId: Long?, groupId: Long?): Long {
        return chatDao.insertConversation(ConversationEntity(agentId = agentId, groupId = groupId))
    }

    suspend fun sendMessage(conversationId: Long, agentId: Long?, groupId: Long?, content: String, contextLimit: Int) {
        chatDao.insertMessage(MessageEntity(conversationId = conversationId, role = "user", content = content))
        
        if (groupId != null) {
            val agents = chatDao.getAgentsByGroupId(groupId)
            for (agent in agents) {
                delay(800)
                processAgentResponse(conversationId, agent.id, contextLimit)
            }
        } else if (agentId != null) {
            processAgentResponse(conversationId, agentId, contextLimit)
        }
    }

    suspend fun deleteConversation(conversationId: Long) {
        chatDao.deleteMessagesByConversation(conversationId)
        chatDao.deleteConversation(conversationId)
    }

    suspend fun deleteMessage(message: MessageEntity) {
        chatDao.deleteMessage(message)
    }

    private suspend fun processAgentResponse(conversationId: Long, agentId: Long, contextLimit: Int) {
        val agent = chatDao.getAgentById(agentId) ?: return
        val model = agent.modelId?.let { chatDao.getModelById(it) } ?: return
        val provider = chatDao.getProviderById(model.providerId) ?: return
        val url = if (provider.baseUrl.endsWith("/")) "${provider.baseUrl}chat/completions" else "${provider.baseUrl}/chat/completions"

        val memories = chatDao.getMemoriesByAgent(agentId)
        var systemPrompt = agent.systemPrompt
        
        val conv = chatDao.getConversationById(conversationId)
        val groupInfo = conv?.groupId?.let { gid ->
            val group = chatDao.getGroupById(gid)
            val members = chatDao.getAgentsByGroupId(gid)
            group to members
        }
        
        if (groupInfo != null) {
            val (group, members) = groupInfo
            systemPrompt += "\n\n你现在处于群聊 \"${group?.name}\" 中。"
            systemPrompt += "\n群聊成员包括: 你(${agent.name}), " + members.filter { it.id != agentId }.joinToString(", ") { it.name } + ", 以及用户。"
        }
        
        if (memories.isNotEmpty()) {
            systemPrompt += "\n\n[你的长期记忆]\n" + memories.joinToString("\n") { "- ${it.content}" }
        }

        val limit = if (contextLimit <= 0) 1000 else contextLimit
        val recentMessages = chatDao.getRecentMessages(conversationId, limit)
        val apiMessages = mutableListOf(ChatMessage(role = "system", content = systemPrompt))
        
        val allAgentsMap = chatDao.getAllAgents().first().associateBy { it.id }

        recentMessages.reversed().forEach { msg ->
            val toolCalls = msg.toolCallsJson?.let { 
                try { json.decodeFromString<List<ToolCall>>(it) } catch(e: Exception) { null }
            }
            val senderName = when {
                msg.role == "user" -> "User"
                msg.senderId != null -> allAgentsMap[msg.senderId]?.name
                else -> null
            }
            apiMessages.add(ChatMessage(
                role = msg.role,
                content = if (toolCalls != null || msg.toolCallId != null) {
                    if (msg.content.isEmpty()) null else msg.content
                } else {
                    if (groupInfo != null && senderName != null && msg.role != "tool") "[$senderName]: ${msg.content}" else msg.content
                },
                toolCallId = msg.toolCallId,
                toolCalls = toolCalls,
                name = senderName?.filter { it.isLetterOrDigit() || it == '_' }
            ))
        }

        var turn = 0
        var shouldContinue = true
        while (shouldContinue && turn < 5) {
            turn++
            try {
                val response = apiService.chatCompletions(
                    url = url, 
                    auth = "Bearer ${provider.apiKey}", 
                    request = ChatRequest(model = model.remoteModelId, messages = apiMessages, tools = AVAILABLE_TOOLS)
                )
                val assistantMsg = response.choices.firstOrNull()?.message ?: break
                if (!assistantMsg.toolCalls.isNullOrEmpty()) {
                    chatDao.insertMessage(MessageEntity(conversationId = conversationId, senderId = agentId, role = "assistant", content = assistantMsg.content ?: "", toolCallsJson = json.encodeToString(assistantMsg.toolCalls)))
                    apiMessages.add(assistantMsg)
                    for (toolCall in assistantMsg.toolCalls) {
                        val result = executeTool(agentId, agent.name, toolCall)
                        chatDao.insertMessage(MessageEntity(conversationId = conversationId, senderId = agentId, role = "tool", content = result, toolCallId = toolCall.id))
                        apiMessages.add(ChatMessage(role = "tool", toolCallId = toolCall.id, name = toolCall.function.name, content = result))
                    }
                } else {
                    val fullContent = assistantMsg.content ?: ""
                    if (fullContent.isNotBlank()) {
                        val cleanContent = fullContent.replace(Regex("^\\[[^\\]]+\\][:：]\\s*"), "")
                        chatDao.insertMessage(MessageEntity(conversationId = conversationId, senderId = agentId, role = "assistant", content = cleanContent.trim()))
                    }
                    shouldContinue = false
                }
            } catch (e: Exception) {
                chatDao.insertMessage(MessageEntity(conversationId = conversationId, senderId = agentId, role = "assistant", content = "API 异常: ${e.message}"))
                shouldContinue = false
            }
        }
    }

    private suspend fun executeTool(agentId: Long, agentName: String, toolCall: ToolCall): String {
        return try {
            val args = json.parseToJsonElement(toolCall.function.arguments).jsonObject
            when (toolCall.function.name) {
                "create_memory" -> {
                    val c = args["content"]?.jsonPrimitive?.content ?: ""
                    chatDao.insertMemory(MemoryEntity(agentId = agentId, content = c))
                    "已存入长期记忆。"
                }
                "post_moment" -> {
                    val c = args["content"]?.jsonPrimitive?.content ?: ""
                    chatDao.insertMoment(MomentEntity(agentId = agentId, content = c))
                    "动态已发布。"
                }
                "get_moments_feed" -> {
                    val moments = chatDao.getAllMomentsWithAgent().first().take(10)
                    moments.joinToString("\n") { "ID:${it.id} | 作者:${it.agentName ?: "用户"} | 内容:${it.content}" }
                }
                "comment_moment" -> {
                    val mId = args["momentId"]?.jsonPrimitive?.longOrNull ?: return "ID无效"
                    val content = args["content"]?.jsonPrimitive?.content ?: ""
                    chatDao.insertComment(CommentEntity(momentId = mId, senderId = agentId, senderName = agentName, content = content))
                    "已发表评论。"
                }
                "like_moment" -> {
                    val mId = args["momentId"]?.jsonPrimitive?.longOrNull ?: return "ID无效"
                    chatDao.insertLike(LikeEntity(momentId = mId, senderId = agentId, senderName = agentName))
                    "已点赞。"
                }
                "get_moment_interactions" -> {
                    val mId = args["momentId"]?.jsonPrimitive?.longOrNull ?: return "ID无效"
                    val likes = chatDao.getLikesForMoment(mId).first().joinToString { it.senderName }
                    val comments = chatDao.getCommentsForMoment(mId).first().joinToString("\n") { "${it.senderName}: ${it.content}" }
                    "点赞者: $likes \n评论:\n$comments"
                }
                else -> "未知工具"
            }
        } catch (e: Exception) { "错误: ${e.message}" }
    }
    
    fun getLikesForMoment(momentId: Long) = chatDao.getLikesForMoment(momentId)
    fun getCommentsForMoment(momentId: Long) = chatDao.getCommentsForMoment(momentId)
    suspend fun deleteMoment(momentId: Long) = chatDao.getMomentById(momentId)?.let { chatDao.deleteMoment(it) }
    suspend fun insertLike(momentId: Long, senderName: String) = chatDao.insertLike(LikeEntity(momentId = momentId, senderId = null, senderName = senderName))
    suspend fun insertComment(momentId: Long, senderName: String, content: String) = chatDao.insertComment(CommentEntity(momentId = momentId, senderId = null, senderName = senderName, content = content))
}
