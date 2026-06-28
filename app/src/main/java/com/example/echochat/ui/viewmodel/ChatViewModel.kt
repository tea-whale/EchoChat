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

    // 使用 String key 来区分私聊 (a_id) 和群聊 (g_id)
    private val messagesFlowMap = mutableMapOf<String, StateFlow<List<MessageEntity>>>()

    fun getMessages(agentId: Long?, groupId: Long?): StateFlow<List<MessageEntity>> {
        val key = if (groupId != null) "g_$groupId" else "a_$agentId"
        return messagesFlowMap.getOrPut(key) {
            val flow = if (groupId != null) {
                repository.getMessagesByGroupId(groupId)
            } else {
                repository.getMessagesByAgentId(agentId ?: 0L)
            }
            flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        }
    }

    fun setContextLimit(limit: Int) {
        _contextLimit.value = limit
    }

    fun sendMessage(agentId: Long?, groupId: Long?, content: String) {
        viewModelScope.launch {
            if (groupId != null) {
                repository.sendGroupMessage(groupId, content, _contextLimit.value)
            } else if (agentId != null) {
                repository.sendMessage(agentId, content, _contextLimit.value)
            }
        }
    }
}
