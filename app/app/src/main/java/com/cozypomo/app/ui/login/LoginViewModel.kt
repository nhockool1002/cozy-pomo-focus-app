package com.cozypomo.app.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cozypomo.app.data.auth.AuthRepository
import com.cozypomo.app.data.auth.toAuthErrorMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AuthMode { Login, Register }

data class LoginUiState(
    val mode: AuthMode = AuthMode.Login,
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

/** T-041 — chưa có trong Screen List gốc nhưng bắt buộc trước khi gọi API cần JWT. */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, errorMessage = null) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, errorMessage = null) }
    fun onDisplayNameChange(value: String) = _uiState.update { it.copy(displayName = value) }

    fun toggleMode() = _uiState.update {
        it.copy(
            mode = if (it.mode == AuthMode.Login) AuthMode.Register else AuthMode.Login,
            errorMessage = null,
        )
    }

    fun submit(onSuccess: () -> Unit) {
        val state = _uiState.value
        validate(state)?.let { error ->
            _uiState.update { it.copy(errorMessage = error) }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = if (state.mode == AuthMode.Login) {
                authRepository.login(email = state.email.trim(), password = state.password)
            } else {
                authRepository.register(
                    email = state.email.trim(),
                    password = state.password,
                    displayName = state.displayName.trim(),
                )
            }
            result
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.toAuthErrorMessage()) }
                }
        }
    }

    private fun validate(state: LoginUiState): String? = when {
        state.email.isBlank() || !state.email.contains("@") -> "Email không hợp lệ."
        state.password.length < 6 -> "Mật khẩu cần ít nhất 6 ký tự."
        else -> null
    }
}
