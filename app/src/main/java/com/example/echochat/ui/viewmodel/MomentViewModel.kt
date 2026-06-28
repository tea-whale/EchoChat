package com.example.echochat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echochat.data.local.dao.MomentWithAgent
import com.example.echochat.data.repository.MomentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MomentViewModel @Inject constructor(
    private val repository: MomentRepository
) : ViewModel() {

    val allMoments: StateFlow<List<MomentWithAgent>> = repository.getAllMoments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun postMoment(content: String) {
        viewModelScope.launch {
            repository.insertMoment(content)
        }
    }
}
