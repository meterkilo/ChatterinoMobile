package com.example.chatterinomobile.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatterinomobile.data.repository.CacheAdmin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val cacheAdmin: CacheAdmin
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    fun clearCache() {
        if (_uiState.value.isClearingCache) return
        update { copy(isClearingCache = true, statusMessage = null, errorMessage = null) }

        viewModelScope.launch {
            runCatching { cacheAdmin.clearAllCaches() }
                .onSuccess {
                    update {
                        copy(
                            isClearingCache = false,
                            statusMessage = "Cache cleared."
                        )
                    }
                }
                .onFailure {
                    update {
                        copy(
                            isClearingCache = false,
                            errorMessage = it.message ?: "Failed to clear cache"
                        )
                    }
                }
        }
    }

    fun consumeMessages() {
        update { copy(statusMessage = null, errorMessage = null) }
    }

    private inline fun update(transform: SettingsUiState.() -> SettingsUiState) {
        _uiState.value = _uiState.value.transform()
    }
}

data class SettingsUiState(
    val isClearingCache: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)
