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
        Tool(type = "function", function = FunctionDef(name = "create_memory", description = "记录关于用户的长期记忆。", parameters = buildJsonObject { put("type", "object"); putJsonObject("properties") { putJsonObject("content") { put("type", "string") } }; putJsonArray("required") { add("content") } })),
        Tool(type = "function", function = FunctionDef(name = "post_moment", description = "在朋友圈发布一条动态。", parameters = buildJsonObject { put("type", "object"); putJsonObject("properties") { putJsonObject("content") { put("type", "string") } }; putJsonArray("required") { add("content") } })),
        Tool(type = "function", function = FunctionDef(name = "get_moments_feed", description = "获取朋友圈最近的动态列表。", parameters = buildJsonObject { put("type", "object") })),
        Tool(type = "function", function = FunctionDef(name = "get_moment_details", description = "获取特定动态的详细内容，包括所有点赞者和评论内容。", parameters = buildJsonObject { put("type", "object"); putJsonObject("properties") { putJsonObject("momentId") { put("type", "integer"); put("description", "动态的唯一 ID") } }; putJsonArray("required") { add("momentId") } })),
        Tool(type = "function", function = FunctionDef(name = "comment_moment", description = "评论朋友圈。", parameters = buildJsonObject { put("type", "object"); putJsonObject("properties") { put("momentId", buildJsonObject { put("type", "integer") }); put("content", buildJsonObject { put("type", "string") }) }; putJsonArray("required") { add("momentId"); add("content") } })),
        Tool(type = "function", function = FunctionDef(name = "like_moment", description = "点赞朋友圈。", parameters = buildJsonObject { put("type", "object"); putJsonObject("properties") { put("momentId", buildJsonObject { put("type", "integer") }) }; putJsonArray("required") { add("momentId") } }))
    )

    fun getChatList(): Flow<List<ChatListItem>> = chatDao.getChatList()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getMessagesByConversationId(conversationId: Long): Flow<List<MessageEntity>> = chatDao.getMessagesByConversation(conversationId).distinctUntilChanged()

    suspend fun getOrCreateLatestConversation(agentId: Long?, groupId: Long?): Long {
        val existing = if (groupId != null) chatDao.getLatestConversationByGroupId(groupId) else chatDao.getLatestConversationByAgentId(agentId ?: 0L)
        return existing?.id ?: chatDao.insertConversation(ConversationEntity(agentId = agentId, groupId = groupId))
    }

    suspend fun createNewConversation(agentId: Long?, groupId: Long?): Long = chatDao.insertConversation(ConversationEntity(agentId = agentId, groupId = groupId))

    suspend fun getConversationContextLimit(conversationId: Long): Int {
        return chatDao.getConversationById(conversationId)?.contextLimit ?: 20
    }

    suspend fun updateConversationContextLimit(conversationId: Long, limit: Int) {
        chatDao.updateContextLimit(conversationId, limit)
    }

    suspend fun sendMessage(conversationId: Long, agentId: Long?, groupId: Long?, content: String, contextLimit: Int) {
        chatDao.insertMessage(MessageEntity(conversationId = conversationId, role = "user", content = content))
        if (groupId != null) {
            val agents = chatDao.getAgentsByGroupId(groupId)
            for (agent in agents) { delay(800); processAgentResponse(conversationId, agent.id, contextLimit) }
        } else if (agentId != null) { processAgentResponse(conversationId, agentId, contextLimit) }
    }

    suspend fun deleteConversation(conversationId: Long) { chatDao.deleteMessagesByConversation(conversationId); chatDao.deleteConversation(conversationId) }
    suspend fun deleteMessage(message: MessageEntity) = chatDao.deleteMessage(message)

    private suspend fun processAgentResponse(conversationId: Long, agentId: Long, contextLimit: Int) {
        val agent = chatDao.getAgentById(agentId) ?: return
        val model = agent.modelId?.let { chatDao.getModelById(it) } ?: return
        val provider = chatDao.getProviderById(model.providerId) ?: return
        val url = if (provider.baseUrl.endsWith("/")) "${provider.baseUrl}chat/completions" else "${provider.baseUrl}/chat/completions"

        val memories = chatDao.getMemoriesByAgent(agentId)
        var systemPrompt = agent.systemPrompt + "\n\n请直接回答内容，严禁在回复中带上任何 [姓名]: 或类似的姓名占位符前缀。"
        
        val conv = chatDao.getConversationById(conversationId)
        val groupInfo = conv?.groupId?.let { gid -> chatDao.getGroupById(gid) to chatDao.getAgentsByGroupId(gid) }
        if (groupInfo != null) {
            systemPrompt += "\n你当前处于群聊中。你会通过 API 的 name 字段识别说话人。请不要代发其他成员的消息。"
        }
        if (memories.isNotEmpty()) systemPrompt += "\n[长期记忆]\n" + memories.joinToString("\n") { "- ${it.content}" }

        val finalLimit = conv?.contextLimit ?: contextLimit
        val limit = if (finalLimit <= 0) 1000 else finalLimit
        
        val recentMessages = chatDao.getRecentMessages(conversationId, limit)
        val apiMessages = mutableListOf(ChatMessage(role = "system", content = systemPrompt))
        val allAgentsMap = chatDao.getAllAgents().first().associateBy { it.id }

        recentMessages.reversed().forEach { msg ->
            val toolCalls = msg.toolCallsJson?.let { try { json.decodeFromString<List<ToolCall>>(it) } catch(e: Exception) { null } }
            val senderName = when {
                msg.role == "user" -> "User"
                msg.senderId != null -> allAgentsMap[msg.senderId]?.name
                else -> null
            }
            apiMessages.add(ChatMessage(
                role = msg.role,
                content = if (toolCalls != null || msg.toolCallId != null) { if (msg.content.isEmpty()) null else msg.content } else { msg.content },
                toolCallId = msg.toolCallId,
                toolCalls = toolCalls,
                name = senderName?.filter { it.isLetterOrDigit() || it == '_' }
            ))
        }

        var turn = 0
        while (turn < 5) {
            turn++
            try {
                val response = apiService.chatCompletions(url = url, auth = "Bearer ${provider.apiKey}", request = ChatRequest(model = model.remoteModelId, messages = apiMessages, tools = AVAILABLE_TOOLS))
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
                        val cleanContent = fullContent.replace(Regex("^\\[[^\\]]+\\][:：]\\s*"), "").trim()
                        val parts = cleanContent.split("\n").filter { it.isNotBlank() }
                        for (part in parts) {
                            chatDao.insertMessage(MessageEntity(conversationId = conversationId, senderId = agentId, role = "assistant", content = part.trim()))
                        }
                    }
                    break
                }
            } catch (e: Exception) {
                chatDao.insertMessage(MessageEntity(conversationId = conversationId, senderId = agentId, role = "assistant", content = "服务异常"))
                break
            }
        }
    }

    private suspend fun executeTool(agentId: Long, agentName: String, toolCall: ToolCall): String {
        return try {
            val args = json.parseToJsonElement(toolCall.function.arguments).jsonObject
            when (toolCall.function.name) {
                "create_memory" -> { chatDao.insertMemory(MemoryEntity(agentId = agentId, content = args["content"]?.jsonPrimitive?.content ?: "")); "记忆已保存" }
                "post_moment" -> { chatDao.insertMoment(MomentEntity(agentId = agentId, content = args["content"]?.jsonPrimitive?.content ?: "")); "动态已发布" }
                "get_moments_feed" -> { chatDao.getAllMomentsWithAgent().first().take(10).joinToString("\n") { "ID:${it.id}|作者:${it.agentName ?: "用户"}|内容:${it.content}" } }
                "get_moment_details" -> {
                    val momentId = args["momentId"]?.jsonPrimitive?.longOrNull ?: 0L
                    val moment = chatDao.getMomentById(momentId)
                    if (moment == null) {
                        "未找到该动态 (ID: $momentId)"
                    } else {
                        val likes = chatDao.getLikesForMoment(momentId).first()
                        val comments = chatDao.getCommentsForMoment(momentId).first()
                        val authorName = if (moment.agentId != null) chatDao.getAgentById(moment.agentId)?.name ?: "未知" else "用户"
                        buildString {
                            append("动态详情 (ID: $momentId):\n")
                            append("作者: $authorName\n")
                            append("内容: ${moment.content}\n")
                            append("点赞: ${if (likes.isEmpty()) "暂无" else likes.joinToString { it.senderName }}\n")
                            append("评论:\n")
                            if (comments.isEmpty()) append("暂无") else comments.forEach { append("- ${it.senderName}: ${it.content}\n") }
                        }
                    }
                }
                "comment_moment" -> { chatDao.insertComment(CommentEntity(momentId = args["momentId"]?.jsonPrimitive?.longOrNull ?: 0L, senderId = agentId, senderName = agentName, content = args["content"]?.jsonPrimitive?.content ?: "")); "评论成功" }
                "like_moment" -> { chatDao.insertLike(LikeEntity(momentId = args["momentId"]?.jsonPrimitive?.longOrNull ?: 0L, senderId = agentId, senderName = agentName)); "点赞成功" }
                else -> "成功"
            }
        } catch (e: Exception) { "错误" }
    }
    
    fun getLikesForMoment(momentId: Long) = chatDao.getLikesForMoment(momentId)
    fun getCommentsForMoment(momentId: Long) = chatDao.getCommentsForMoment(momentId)
    suspend fun deleteMoment(momentId: Long) = chatDao.getMomentById(momentId)?.let { chatDao.deleteMoment(it) }
    suspend fun insertLike(momentId: Long, senderName: String) = chatDao.insertLike(LikeEntity(momentId = momentId, senderId = null, senderName = senderName))
    suspend fun insertComment(momentId: Long, senderName: String, content: String) = chatDao.insertComment(CommentEntity(momentId = momentId, senderId = null, senderName = senderName, content = content))
}
