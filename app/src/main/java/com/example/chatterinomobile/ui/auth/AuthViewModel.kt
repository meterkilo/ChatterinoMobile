package com.example.chatterinomobile.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatterinomobile.data.model.TwitchImplicitAuthResult
import com.example.chatterinomobile.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch { refreshSession() }
    }

    fun startLogin() {
        val url = authRepository.buildAuthorizeUrl()
        if (url == null) {
            update {
                copy(
                    isLoading = false,
                    errorMessage = "No Twitch client ID is configured in local.properties."
                )
            }
            return
        }
        update {
            copy(
                isLoading = false,
                isAwaitingAuthorization = true,
                authorizeUrl = url,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun cancelLogin() {
        update {
            copy(
                isAwaitingAuthorization = false,
                authorizeUrl = null
            )
        }
    }

    fun onRedirectIntercepted(redirectUrl: String) {
        viewModelScope.launch {
            update { copy(isLoading = true, authorizeUrl = null) }

            runCatching { authRepository.completeImplicitFlow(redirectUrl) }
                .onSuccess { result ->
                    when (result) {
                        is TwitchImplicitAuthResult.Authorized ->
                            refreshSession(successMessage = "Twitch login complete.")
                        TwitchImplicitAuthResult.Denied -> update {
                            copy(
                                isLoading = false,
                                isAwaitingAuthorization = false,
                                authorizeUrl = null,
                                errorMessage = "Authorization was denied in Twitch."
                            )
                        }
                        is TwitchImplicitAuthResult.Failed -> update {
                            copy(
                                isLoading = false,
                                isAwaitingAuthorization = false,
                                authorizeUrl = null,
                                errorMessage = result.message
                            )
                        }
                    }
                }
                .onFailure { error ->
                    update {
                        copy(
                            isLoading = false,
                            isAwaitingAuthorization = false,
                            authorizeUrl = null,
                            errorMessage = error.message ?: "Twitch auth failed"
                        )
                    }
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            runCatching {
                authRepository.clearSession()
                refreshSession(successMessage = "Logged out.")
            }.onFailure {
                update { copy(errorMessage = it.message ?: "Logout failed") }
            }
        }
    }

    fun consumeMessages() {
        update { copy(successMessage = null, errorMessage = null) }
    }

    private suspend fun refreshSession(successMessage: String? = null) {
        update { copy(isLoading = true, errorMessage = null) }
        val token = authRepository.getAccessToken()
        val login = authRepository.getLogin()
        val userId = authRepository.getUserId()
        update {
            copy(
                isLoading = false,
                isLoggedIn = token != null,
                login = login,
                userId = userId,
                isAwaitingAuthorization = false,
                authorizeUrl = null,
                successMessage = successMessage
            )
        }
    }

    private inline fun update(transform: AuthUiState.() -> AuthUiState) {
        _uiState.value = _uiState.value.transform()
    }
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val isAwaitingAuthorization: Boolean = false,
    val login: String? = null,
    val userId: String? = null,
    val authorizeUrl: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
