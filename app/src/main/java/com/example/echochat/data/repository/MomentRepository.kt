package com.example.echochat.data.repository

import com.example.echochat.data.local.dao.ChatDao
import com.example.echochat.data.local.dao.MomentWithAgent
import com.example.echochat.data.local.entity.MomentEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MomentRepository @Inject constructor(
    private val chatDao: ChatDao
) {
    fun getAllMoments(): Flow<List<MomentWithAgent>> = chatDao.getAllMomentsWithAgent()

    suspend fun insertMoment(content: String) {
        chatDao.insertMoment(MomentEntity(agentId = null, content = content))
    }
}
