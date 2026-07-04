package com.example.echochat.data.local.dao

import androidx.room.*
import com.example.echochat.data.local.entity.*
import kotlinx.coroutines.flow.Flow

data class ChatListItem(
    val conversationId: Long,
    val agentId: Long?,
    val groupId: Long?,
    val contactName: String,
    val contactAvatar: String?,
    val lastMessage: String?,
    val lastMessageTime: Long,
    val isGroup: Boolean
)

data class MomentWithAgent(
    val id: Long,
    val agentId: Long?,
    val agentName: String?,
    val agentAvatar: String?,
    val content: String,
    val createdAt: Long
)

@Dao
interface ChatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE id = 1")
    fun getUser(): Flow<UserEntity?>

    // Provider
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProvider(provider: ProviderEntity): Long

    @Update
    suspend fun updateProvider(provider: ProviderEntity)

    @Delete
    suspend fun deleteProvider(provider: ProviderEntity)

    @Query("SELECT * FROM providers WHERE id = :id")
    suspend fun getProviderById(id: Long): ProviderEntity?

    @Query("SELECT * FROM providers")
    fun getAllProviders(): Flow<List<ProviderEntity>>

    // Model
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModels(models: List<ModelEntity>): List<Long>

    @Query("SELECT * FROM models WHERE providerId = :providerId")
    fun getModelsByProvider(providerId: Long): Flow<List<ModelEntity>>

    @Query("SELECT * FROM models WHERE id = :id")
    suspend fun getModelById(id: Long): ModelEntity?

    @Query("DELETE FROM models WHERE providerId = :providerId")
    suspend fun deleteModelsByProvider(providerId: Long)

    // Agent
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgent(agent: AgentEntity): Long

    @Update
    suspend fun updateAgent(agent: AgentEntity)

    @Delete
    suspend fun deleteAgent(agent: AgentEntity)

    @Query("SELECT * FROM agents")
    fun getAllAgents(): Flow<List<AgentEntity>>

    @Query("SELECT * FROM agents WHERE id = :id")
    suspend fun getAgentById(id: Long): AgentEntity?

    // Group
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupMember(crossRef: GroupMemberCrossRef)

    @Query("SELECT * FROM groups WHERE id = :id")
    suspend fun getGroupById(id: Long): GroupEntity?

    @Query("""
        SELECT a.* FROM agents a
        JOIN group_members gm ON a.id = gm.agentId
        WHERE gm.groupId = :groupId
    """)
    suspend fun getAgentsByGroupId(groupId: Long): List<AgentEntity>

    // Conversation
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversationById(id: Long): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE agentId = :agentId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestConversationByAgentId(agentId: Long): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE groupId = :groupId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestConversationByGroupId(groupId: Long): ConversationEntity?
    
    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesByConversation(conversationId: Long)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversation(id: Long)

    @Query("""
        SELECT 
            c.id as conversationId, 
            c.agentId as agentId, 
            c.groupId as groupId,
            CASE WHEN c.groupId IS NOT NULL THEN g.name ELSE a.name END as contactName,
            CASE WHEN c.groupId IS NOT NULL THEN g.avatar ELSE a.avatar END as contactAvatar,
            m.content as lastMessage, 
            COALESCE(m.createdAt, c.createdAt) as lastMessageTime,
            (c.groupId IS NOT NULL) as isGroup
        FROM conversations c
        LEFT JOIN agents a ON c.agentId = a.id
        LEFT JOIN groups g ON c.groupId = g.id
        LEFT JOIN (
            SELECT conversationId, content, createdAt
            FROM messages
            WHERE id IN (SELECT MAX(id) FROM messages GROUP BY conversationId)
        ) m ON c.id = m.conversationId
        ORDER BY lastMessageTime DESC
    """)
    fun getChatList(): Flow<List<ChatListItem>>

    // Message
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun getMessagesByConversation(conversationId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentMessages(conversationId: Long, limit: Int): List<MessageEntity>

    @Delete
    suspend fun deleteMessage(message: MessageEntity)

    // Memory
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity)

    @Update
    suspend fun updateMemory(memory: MemoryEntity)

    @Delete
    suspend fun deleteMemory(memory: MemoryEntity)

    @Query("SELECT * FROM memories WHERE agentId = :agentId ORDER BY createdAt DESC")
    fun getMemoriesByAgentFlow(agentId: Long): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE agentId = :agentId ORDER BY createdAt DESC")
    suspend fun getMemoriesByAgent(agentId: Long): List<MemoryEntity>

    // Moment
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoment(moment: MomentEntity): Long

    @Query("""
        SELECT m.id, m.agentId, a.name as agentName, a.avatar as agentAvatar, m.content, m.createdAt
        FROM moments m LEFT JOIN agents a ON m.agentId = a.id ORDER BY m.createdAt DESC
    """)
    fun getAllMomentsWithAgent(): Flow<List<MomentWithAgent>>

    @Delete
    suspend fun deleteMoment(moment: MomentEntity)

    @Query("SELECT * FROM moments WHERE id = :id")
    suspend fun getMomentById(id: Long): MomentEntity?

    // Likes & Comments
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLike(like: LikeEntity)

    @Query("SELECT * FROM likes WHERE momentId = :momentId ORDER BY createdAt ASC")
    fun getLikesForMoment(momentId: Long): Flow<List<LikeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Query("SELECT * FROM comments WHERE momentId = :momentId ORDER BY createdAt ASC")
    fun getCommentsForMoment(momentId: Long): Flow<List<CommentEntity>>
}
