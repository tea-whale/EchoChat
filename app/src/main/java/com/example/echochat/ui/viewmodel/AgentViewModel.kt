package com.example.echochat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echochat.data.local.dao.ChatDao
import com.example.echochat.data.local.entity.*
import com.example.echochat.data.repository.AgentRepository
import com.example.echochat.data.repository.ProviderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AgentViewModel @Inject constructor(
    private val agentRepository: AgentRepository,
    private val providerRepository: ProviderRepository,
    private val chatDao: ChatDao
) : ViewModel() {

    val allAgents: StateFlow<List<AgentEntity>> = agentRepository.getAllAgents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProviders: StateFlow<List<ProviderEntity>> = providerRepository.getAllProviders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveAgent(
        id: Long = 0,
        name: String,
        avatar: String?,
        chatBackground: String?,
        description: String,
        systemPrompt: String,
        providerId: Long,
        modelName: String
    ) {
        viewModelScope.launch {
            val existingModels = chatDao.getModelsByProvider(providerId).first()
            var modelId = existingModels.find { it.remoteModelId == modelName }?.id
            
            if (modelId == null) {
                val newModel = ModelEntity(providerId = providerId, remoteModelId = modelName)
                chatDao.insertModels(listOf(newModel))
                val updatedModels = chatDao.getModelsByProvider(providerId).first()
                modelId = updatedModels.find { it.remoteModelId == modelName }?.id
            }

            val agent = AgentEntity(
                id = id,
                name = name,
                avatar = avatar,
                chatBackground = chatBackground,
                description = description,
                systemPrompt = systemPrompt,
                modelId = modelId
            )
            
            if (id == 0L) {
                agentRepository.insertAgent(agent)
            } else {
                agentRepository.updateAgent(agent)
            }
        }
    }

    fun deleteAgent(agent: AgentEntity) {
        viewModelScope.launch {
            agentRepository.deleteAgent(agent)
        }
    }

    fun createGroup(name: String, selectedAgentIds: List<Long>) {
        viewModelScope.launch {
            val groupId = chatDao.insertGroup(GroupEntity(name = name))
            selectedAgentIds.forEach { agentId ->
                chatDao.insertGroupMember(GroupMemberCrossRef(groupId = groupId, agentId = agentId))
            }
            chatDao.insertConversation(ConversationEntity(groupId = groupId))
        }
    }

    // --- 记忆管理 ---
    fun getMemories(agentId: Long): Flow<List<MemoryEntity>> {
        return chatDao.getMemoriesByAgentFlow(agentId)
    }

    fun updateMemory(memory: MemoryEntity) {
        viewModelScope.launch {
            chatDao.updateMemory(memory)
        }
    }

    fun deleteMemory(memory: MemoryEntity) {
        viewModelScope.launch {
            chatDao.deleteMemory(memory)
        }
    }

    suspend fun getModelInfo(modelId: Long?): Pair<ProviderEntity?, String>? {
        if (modelId == null) return null
        val model = chatDao.getModelById(modelId) ?: return null
        val provider = providerRepository.getProviderById(model.providerId)
        return provider to model.remoteModelId
    }
}
