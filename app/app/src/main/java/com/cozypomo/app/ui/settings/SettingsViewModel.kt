package com.cozypomo.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cozypomo.app.BuildConfig
import com.cozypomo.app.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val email: String? = null,
    val userId: String? = null,
    val versionName: String = BuildConfig.VERSION_NAME,
)

/** S-07 — gom các chức năng cài đặt (kể cả đăng xuất) vào 1 màn riêng, tách khỏi Trang chủ.
 * Menu cheat tester (5-tap vào "Phiên bản") nay sống ở [com.cozypomo.app.ui.common.TesterCheatViewModel]
 * dùng chung toàn app — xem CozyPomoNavHost — vì bubble phải hiện được ở mọi tab, không chỉ ở đây. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        authRepository.accountEmail,
        authRepository.accountId,
    ) { email, userId -> SettingsUiState(email = email, userId = userId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onDone()
        }
    }
}
