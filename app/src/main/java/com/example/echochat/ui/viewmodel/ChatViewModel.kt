package com.example.echochat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echochat.data.local.dao.ChatListItem
import com.example.echochat.data.local.entity.MessageEntity
import com.example.echochat.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository
) : ViewModel() {

    val chatList: StateFlow<List<ChatListItem>> = repository.getChatList()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _contextLimit = MutableStateFlow(20)
    val contextLimit = _contextLimit.asStateFlow()

    private val _currentConversationId = MutableStateFlow<Long?>(null)
    val currentConversationId = _currentConversationId.asStateFlow()

    // 监听特定对话的消息流
    fun getMessages(conversationId: Long): Flow<List<MessageEntity>> {
        return repository.getMessagesByConversationId(conversationId)
    }

    // 初始化或切换对话
    fun initConversation(agentId: Long?, groupId: Long?, conversationId: Long? = null) {
        viewModelScope.launch {
            val id = if (conversationId != null && conversationId > 0) {
                conversationId
            } else {
                repository.getOrCreateLatestConversation(agentId, groupId)
            }
            _currentConversationId.value = id
            // 加载该对话保存的记忆限制
            _contextLimit.value = repository.getConversationContextLimit(id)
        }
    }

    // 创建全新对话实体
    fun startNewConversation(agentId: Long?, groupId: Long?) {
        viewModelScope.launch {
            val id = repository.createNewConversation(agentId, groupId)
            _currentConversationId.value = id
            _contextLimit.value = 20 // 新对话恢复默认值
        }
    }

    fun setContextLimit(limit: Int) {
        val convId = _currentConversationId.value ?: return
        _contextLimit.value = limit
        viewModelScope.launch {
            repository.updateConversationContextLimit(convId, limit)
        }
    }

    fun sendMessage(agentId: Long?, groupId: Long?, content: String) {
        val convId = _currentConversationId.value ?: return
        viewModelScope.launch {
            repository.sendMessage(convId, agentId, groupId, content, _contextLimit.value)
        }
    }

    fun deleteMessage(message: MessageEntity) {
        viewModelScope.launch {
            repository.deleteMessage(message)
        }
    }

    fun deleteCurrentConversation() {
        val id = _currentConversationId.value ?: return
        viewModelScope.launch {
            repository.deleteConversation(id)
            _currentConversationId.value = null
        }
    }
}
