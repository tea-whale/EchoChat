package com.example.echochat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echochat.data.local.dao.MomentWithAgent
import com.example.echochat.data.local.entity.CommentEntity
import com.example.echochat.data.local.entity.LikeEntity
import com.example.echochat.data.repository.ChatRepository
import com.example.echochat.data.repository.MomentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MomentViewModel @Inject constructor(
    private val repository: MomentRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    val allMoments: StateFlow<List<MomentWithAgent>> = repository.getAllMoments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun postMoment(content: String) {
        viewModelScope.launch {
            repository.insertMoment(content)
        }
    }

    fun deleteMoment(momentId: Long) {
        viewModelScope.launch {
            chatRepository.deleteMoment(momentId)
        }
    }

    fun getLikes(momentId: Long): Flow<List<LikeEntity>> = chatRepository.getLikesForMoment(momentId)

    fun getComments(momentId: Long): Flow<List<CommentEntity>> = chatRepository.getCommentsForMoment(momentId)
    
    fun likeMoment(momentId: Long, userName: String) {
        viewModelScope.launch {
            chatRepository.insertLike(momentId, userName)
        }
    }

    fun commentMoment(momentId: Long, userName: String, content: String) {
        viewModelScope.launch {
            chatRepository.insertComment(momentId, userName, content)
        }
    }
}
