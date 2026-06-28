package com.example.echochat.data.repository

import com.example.echochat.data.local.dao.ChatDao
import com.example.echochat.data.local.entity.ModelEntity
import com.example.echochat.data.local.entity.ProviderEntity
import com.example.echochat.data.remote.ApiService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderRepository @Inject constructor(
    private val chatDao: ChatDao,
    private val apiService: ApiService
) {
    fun getAllProviders(): Flow<List<ProviderEntity>> = chatDao.getAllProviders()

    suspend fun insertProvider(provider: ProviderEntity) = chatDao.insertProvider(provider)

    suspend fun updateProvider(provider: ProviderEntity) = chatDao.updateProvider(provider)

    suspend fun deleteProvider(provider: ProviderEntity) = chatDao.deleteProvider(provider)

    suspend fun getProviderById(id: Long) = chatDao.getProviderById(id)

    // Models
    fun getModelsByProvider(providerId: Long): Flow<List<ModelEntity>> = 
        chatDao.getModelsByProvider(providerId)

    suspend fun syncModels(provider: ProviderEntity) {
        val url = if (provider.baseUrl.endsWith("/")) {
            "${provider.baseUrl}models"
        } else {
            "${provider.baseUrl}/models"
        }
        
        val response = apiService.getModels(
            url = url,
            auth = "Bearer ${provider.apiKey}"
        )
        
        val modelEntities = response.data.map { remoteModel ->
            ModelEntity(
                providerId = provider.id,
                remoteModelId = remoteModel.id
            )
        }
        
        // Use a transaction or simple delete-then-insert
        chatDao.deleteModelsByProvider(provider.id)
        chatDao.insertModels(modelEntities)
    }
}
