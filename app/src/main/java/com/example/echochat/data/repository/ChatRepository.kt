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
        )
    )

    fun getChatList(): Flow<List<ChatListItem>> = chatDao.getChatList()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getMessagesByAgentId(agentId: Long): Flow<List<MessageEntity>> {
        return flow {
            val conv = chatDao.getConversationByAgentId(agentId)
                ?: ConversationEntity(id = chatDao.insertConversation(ConversationEntity(agentId = agentId)), agentId = agentId)
            emit(conv.id)
        }.flatMapLatest { chatDao.getMessagesByConversation(it) }.distinctUntilChanged()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getMessagesByGroupId(groupId: Long): Flow<List<MessageEntity>> {
        return flow {
            val conv = chatDao.getConversationByGroupId(groupId)
                ?: ConversationEntity(id = chatDao.insertConversation(ConversationEntity(groupId = groupId)), groupId = groupId)
            emit(conv.id)
        }.flatMapLatest { chatDao.getMessagesByConversation(it) }.distinctUntilChanged()
    }

    suspend fun sendMessage(agentId: Long, content: String, contextLimit: Int) {
        val conv = chatDao.getConversationByAgentId(agentId)
            ?: ConversationEntity(id = chatDao.insertConversation(ConversationEntity(agentId = agentId)), agentId = agentId)

        chatDao.insertMessage(MessageEntity(conversationId = conv.id, role = "user", content = content))
        processAgentResponse(conv.id, agentId, contextLimit)
    }

    suspend fun sendGroupMessage(groupId: Long, content: String, contextLimit: Int) {
        val conv = chatDao.getConversationByGroupId(groupId)
            ?: ConversationEntity(id = chatDao.insertConversation(ConversationEntity(groupId = groupId)), groupId = groupId)

        chatDao.insertMessage(MessageEntity(conversationId = conv.id, role = "user", content = content))

        val agents = chatDao.getAgentsByGroupId(groupId)
        for (agent in agents) {
            delay(800) // 增加群聊回复间隔，更自然
            processAgentResponse(conv.id, agent.id, contextLimit)
        }
    }

    private suspend fun processAgentResponse(conversationId: Long, agentId: Long, contextLimit: Int) {
        val agent = chatDao.getAgentById(agentId) ?: return
        val model = agent.modelId?.let { chatDao.getModelById(it) } ?: return
        val provider = chatDao.getProviderById(model.providerId) ?: return
        val url = if (provider.baseUrl.endsWith("/")) "${provider.baseUrl}chat/completions" else "${provider.baseUrl}/chat/completions"

        // 1. 获取上下文：判断是否为群聊，并获取成员名单
        val conv = chatDao.getConversationById(conversationId)
        val groupInfo = conv?.groupId?.let { gid ->
            val group = chatDao.getGroupById(gid)
            val members = chatDao.getAgentsByGroupId(gid)
            group to members
        }

        val memories = chatDao.getMemoriesByAgent(agentId)
        var systemPrompt = agent.systemPrompt
        
        if (groupInfo != null) {
            val (group, members) = groupInfo
            systemPrompt += "\n\n你现在处于群聊 \"${group?.name}\" 中。"
            systemPrompt += "\n群聊成员包括: 你(${agent.name}), " + members.filter { it.id != agentId }.joinToString(", ") { it.name } + ", 以及用户。"
            systemPrompt += "\n在群聊中，你收到的消息内容会带有 [姓名]: 前缀。你可以根据这些前缀分辨是谁在说话。"
        }
        
        if (memories.isNotEmpty()) {
            systemPrompt += "\n\n[你的长期记忆]\n" + memories.joinToString("\n") { "- ${it.content}" }
        }

        val recentMessages = chatDao.getRecentMessages(conversationId, if (contextLimit == -1) 50 else contextLimit)
        val apiMessages = mutableListOf(ChatMessage(role = "system", content = systemPrompt))
        
        val allAgentsMap = chatDao.getAllAgents().first().associateBy { it.id }

        // 2. 构建消息历史
        recentMessages.reversed().forEach { msg ->
            val toolCalls = msg.toolCallsJson?.let { 
                try { json.decodeFromString<List<ToolCall>>(it) } catch(e: Exception) { null }
            }
            
            // 确定发送者姓名
            val senderName = when {
                msg.role == "user" -> "User"
                msg.senderId != null -> allAgentsMap[msg.senderId]?.name
                else -> null
            }

            // 对于群聊，在内容中增加发送者标识。增加判重逻辑，防止重复叠名前缀
            val contentWithPrefix = if (groupInfo != null && senderName != null && msg.role != "tool") {
                val prefix = "[$senderName]: "
                if (msg.content.startsWith(prefix)) msg.content else "$prefix${msg.content}"
            } else {
                msg.content
            }

            apiMessages.add(ChatMessage(
                role = msg.role,
                content = if (toolCalls != null || msg.toolCallId != null) {
                    if (msg.content.isEmpty()) null else msg.content
                } else {
                    contentWithPrefix
                },
                toolCallId = msg.toolCallId,
                toolCalls = toolCalls,
                name = senderName?.filter { it.isLetterOrDigit() || it == '_' } // API name 字段校验
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
                    request = ChatRequest(
                        model = model.remoteModelId, 
                        messages = apiMessages, 
                        tools = AVAILABLE_TOOLS
                    )
                )
                
                val assistantMsg = response.choices.firstOrNull()?.message ?: break
                
                if (!assistantMsg.toolCalls.isNullOrEmpty()) {
                    chatDao.insertMessage(MessageEntity(
                        conversationId = conversationId,
                        senderId = agentId,
                        role = "assistant",
                        content = assistantMsg.content ?: "",
                        toolCallsJson = json.encodeToString(assistantMsg.toolCalls)
                    ))
                    apiMessages.add(assistantMsg)

                    for (toolCall in assistantMsg.toolCalls) {
                        val result = executeTool(agentId, toolCall)
                        chatDao.insertMessage(MessageEntity(
                            conversationId = conversationId,
                            senderId = agentId,
                            role = "tool",
                            content = result,
                            toolCallId = toolCall.id
                        ))
                        apiMessages.add(ChatMessage(
                            role = "tool",
                            toolCallId = toolCall.id,
                            name = toolCall.function.name,
                            content = result
                        ))
                    }
                } else {
                    val fullContent = assistantMsg.content ?: ""
                    if (fullContent.isNotBlank()) {
                        // 清理 AI 可能误生成的 [名字]: 前缀。优化正则，支持任意长度名字和中英文冒号
                        val cleanContent = fullContent.replace(Regex("^\\[[^\\]]+\\][:：]\\s*"), "")
                        
                        val parts = cleanContent.split(Regex("(?<=[。！？!?\\n])")).filter { it.isNotBlank() }
                        for (part in parts) {
                            delay(600L + part.length * 40L)
                            chatDao.insertMessage(MessageEntity(
                                conversationId = conversationId,
                                senderId = agentId,
                                role = "assistant",
                                content = part.trim()
                            ))
                        }
                    }
                    shouldContinue = false
                }
            } catch (e: Exception) {
                chatDao.insertMessage(MessageEntity(
                    conversationId = conversationId,
                    senderId = agentId,
                    role = "assistant",
                    content = "API 响应异常: ${e.message}"
                ))
                shouldContinue = false
            }
        }
    }

    private suspend fun executeTool(agentId: Long, toolCall: ToolCall): String {
        return try {
            val args = json.parseToJsonElement(toolCall.function.arguments).jsonObject
            when (toolCall.function.name) {
                "create_memory" -> {
                    val c = args["content"]?.jsonPrimitive?.content ?: ""
                    chatDao.insertMemory(MemoryEntity(agentId = agentId, content = c))
                    "已成功存入你的长期记忆。"
                }
                "post_moment" -> {
                    val c = args["content"]?.jsonPrimitive?.content ?: ""
                    chatDao.insertMoment(MomentEntity(agentId = agentId, content = c))
                    "动态已发布到朋友圈。"
                }
                else -> "执行成功"
            }
        } catch (e: Exception) { "执行工具时出错: ${e.message}" }
    }
}
