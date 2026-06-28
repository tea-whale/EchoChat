package com.example.echochat.data.repository

import com.example.echochat.data.local.dao.ChatDao
import com.example.echochat.data.local.entity.AgentEntity
import com.example.echochat.data.local.entity.ModelEntity
import com.example.echochat.data.local.entity.ProviderEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentRepository @Inject constructor(
    private val chatDao: ChatDao
) {
    fun getAllAgents(): Flow<List<AgentEntity>> = chatDao.getAllAgents()

    suspend fun getAgentById(id: Long): AgentEntity? = chatDao.getAgentById(id)

    suspend fun insertAgent(agent: AgentEntity) = chatDao.insertAgent(agent)

    suspend fun updateAgent(agent: AgentEntity) = chatDao.updateAgent(agent)

    suspend fun deleteAgent(agent: AgentEntity) = chatDao.deleteAgent(agent)

    // 用于创建 Agent 时选择模型
    fun getAllProvidersWithModels(): Flow<List<ProviderEntity>> = chatDao.getAllProviders()
    
    fun getModelsByProvider(providerId: Long): Flow<List<ModelEntity>> = chatDao.getModelsByProvider(providerId)
}
