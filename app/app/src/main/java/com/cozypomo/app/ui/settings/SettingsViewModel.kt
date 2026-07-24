package com.cozypomo.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cozypomo.app.BuildConfig
import com.cozypomo.app.data.auth.AuthRepository
import com.cozypomo.app.data.network.ApiService
import com.cozypomo.app.data.network.InventoryItemDto
import com.cozypomo.app.data.network.UpdateSettingsRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

val SOUND_THEME_OPTIONS = listOf(
    "default" to "Mặc định",
    "rain" to "Mưa",
    "forest" to "Rừng",
    "lofi" to "Lo-fi",
)

data class SettingsUiState(
    val email: String? = null,
    val userId: String? = null,
    val versionName: String = BuildConfig.VERSION_NAME,
    val settingsLoaded: Boolean = false,
    val focusMinutes: Int = 25,
    val breakMinutes: Int = 5,
    val strictModeEnabled: Boolean = true,
    val soundTheme: String = "default",
    val inventory: List<InventoryItemDto> = emptyList(),
)

/** S-07 — gom các chức năng cài đặt (kể cả đăng xuất) vào 1 màn riêng, tách khỏi Trang chủ.
 * T-039: thêm cấu hình phiên (thời gian mặc định/Strict Mode/âm thanh, `GET`/`PATCH /settings`
 * đã có sẵn từ T-010) + Kho đồ (xem/trang bị bình-nhạc đã mua ở Cửa hàng, `PATCH /inventory/:id/equip`
 * đã có sẵn từ T-008 nhưng chưa có UI nào gọi tới). Không có mục Sao lưu & Đồng bộ (S-07a/`SyncRepository`)
 * theo yêu cầu — không nằm trong phạm vi đợt này.
 * Menu cheat tester (5-tap vào "Phiên bản") sống ở [com.cozypomo.app.ui.common.TesterCheatViewModel]
 * dùng chung toàn app — xem CozyPomoNavHost — vì bubble phải hiện được ở mọi tab, không chỉ ở đây. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val apiService: ApiService,
) : ViewModel() {

    private val _localState = MutableStateFlow(SettingsUiState())

    val uiState: StateFlow<SettingsUiState> = combine(
        authRepository.accountEmail,
        authRepository.accountId,
        _localState,
    ) { email, userId, local -> local.copy(email = email, userId = userId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    init {
        loadSettings()
        loadInventory()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            runCatching { apiService.getSettings() }.onSuccess { dto ->
                _localState.update {
                    it.copy(
                        settingsLoaded = true,
                        focusMinutes = dto.focusMinutes,
                        breakMinutes = dto.breakMinutes,
                        strictModeEnabled = dto.strictModeEnabled,
                        soundTheme = dto.soundTheme,
                    )
                }
            }
        }
    }

    private fun loadInventory() {
        viewModelScope.launch {
            runCatching { apiService.getInventory() }.onSuccess { items ->
                _localState.update { it.copy(inventory = items) }
            }
        }
    }

    /** Gọi khi thả Slider (onValueChangeFinished) — tránh gọi PATCH liên tục khi đang kéo. */
    fun onFocusMinutesChangeFinished(minutes: Int) = saveSettings(UpdateSettingsRequest(focusMinutes = minutes))
    fun onBreakMinutesChangeFinished(minutes: Int) = saveSettings(UpdateSettingsRequest(breakMinutes = minutes))
    fun onStrictModeToggle(enabled: Boolean) = saveSettings(UpdateSettingsRequest(strictModeEnabled = enabled))
    fun onSoundThemeSelected(theme: String) = saveSettings(UpdateSettingsRequest(soundTheme = theme))

    /** Kéo Slider hiển thị ngay giá trị mới (không đợi PATCH) — mượt hơn khi đang thao tác. */
    fun onFocusMinutesDrag(minutes: Int) = _localState.update { it.copy(focusMinutes = minutes) }
    fun onBreakMinutesDrag(minutes: Int) = _localState.update { it.copy(breakMinutes = minutes) }

    private fun saveSettings(request: UpdateSettingsRequest) {
        viewModelScope.launch {
            runCatching { apiService.updateSettings(request) }.onSuccess { dto ->
                _localState.update {
                    it.copy(
                        focusMinutes = dto.focusMinutes,
                        breakMinutes = dto.breakMinutes,
                        strictModeEnabled = dto.strictModeEnabled,
                        soundTheme = dto.soundTheme,
                    )
                }
            }
        }
    }

    fun toggleEquip(inventoryItemId: String) {
        viewModelScope.launch {
            runCatching { apiService.toggleEquip(inventoryItemId) }.onSuccess { loadInventory() }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onDone()
        }
    }
}
