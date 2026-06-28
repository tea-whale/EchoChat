package com.example.echochat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echochat.data.local.dao.ChatDao
import com.example.echochat.data.local.entity.UserEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val chatDao: ChatDao
) : ViewModel() {

    val user: StateFlow<UserEntity?> = chatDao.getUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateUser(name: String, avatar: String?) {
        viewModelScope.launch {
            chatDao.insertUser(UserEntity(id = 1, name = name, avatar = avatar))
        }
    }

    init {
        // 关键修复：先检查是否存在，不存在才插入默认值，防止覆盖用户修改
        viewModelScope.launch {
            val existing = chatDao.getUser().firstOrNull()
            if (existing == null) {
                chatDao.insertUser(UserEntity(id = 1, name = "AI Explorer"))
            }
        }
    }
}
