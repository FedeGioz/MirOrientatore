package com.federicogiordano.mirorientatore.viewmodels

import com.federicogiordano.mirorientatore.api.LoginService
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class LoginViewModel(
    private val loginService: LoginService,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun updateUsername(username: String) {
        _uiState.value = _uiState.value.copy(username = username)
    }

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun login() {
        val currentState = _uiState.value

        if (currentState.username.isEmpty() || currentState.password.isEmpty()) {
            _uiState.value = currentState.copy(
                errorMessage = "Nome utente e password sono obbligatori"
            )
            return
        }

        _uiState.value = currentState.copy(
            isLoading = true,
            errorMessage = null
        )

        scope.launch {
            val success = loginService.login(currentState.username, currentState.password)

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isLoggedIn = success,
                errorMessage = if (!success) "Accesso fallito. Si prega di controllare le credenziali." else null
            )
        }
    }
}

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val errorMessage: String? = null
)