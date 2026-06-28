package com.example.echochat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echochat.data.local.entity.ModelEntity
import com.example.echochat.data.local.entity.ProviderEntity
import com.example.echochat.data.repository.ProviderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProviderViewModel @Inject constructor(
    private val repository: ProviderRepository
) : ViewModel() {

    val allProviders: StateFlow<List<ProviderEntity>> = repository.getAllProviders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSyncing = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val isSyncing = _isSyncing.asStateFlow()

    fun addProvider(name: String, baseUrl: String, apiKey: String) {
        viewModelScope.launch {
            repository.insertProvider(
                ProviderEntity(name = name, baseUrl = baseUrl, apiKey = apiKey)
            )
        }
    }

    fun updateProvider(provider: ProviderEntity) {
        viewModelScope.launch {
            repository.updateProvider(provider)
        }
    }

    fun deleteProvider(provider: ProviderEntity) {
        viewModelScope.launch {
            repository.deleteProvider(provider)
        }
    }

    fun syncModels(provider: ProviderEntity) {
        viewModelScope.launch {
            try {
                _isSyncing.value += (provider.id to true)
                repository.syncModels(provider)
            } catch (e: Exception) {
                e.printStackTrace()
                // In a real app, we should show a snackbar or Toast here
            } finally {
                _isSyncing.value += (provider.id to false)
            }
        }
    }

    fun getModelsForProvider(providerId: Long): StateFlow<List<ModelEntity>> {
        return repository.getModelsByProvider(providerId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }
}
