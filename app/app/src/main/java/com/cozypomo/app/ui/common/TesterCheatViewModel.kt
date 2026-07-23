package com.cozypomo.app.ui.common

import com.cozypomo.app.data.auth.AuthRepository
import com.cozypomo.app.data.events.CollectionEventBus
import com.cozypomo.app.data.network.ApiService
import com.cozypomo.app.data.network.GrantCurrencyRequest
import com.cozypomo.app.data.network.GrantEggRequest
import com.cozypomo.app.data.network.GrantSpeciesRequest
import com.cozypomo.app.data.timer.TimerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private val TESTER_EMAIL_REGEX = Regex("^tester\\d{2}@cozypomo\\.dev$")

data class TesterCheatUiState(
    val isTester: Boolean = false,
    val versionTapCount: Int = 0,
    val bubbleVisible: Boolean = false,
    val showDialog: Boolean = false,
    val cheatMessage: String? = null,
)

/** Bubble cheat kiểu "chat head" Messenger — 1 instance dùng chung toàn app (tạo ở CozyPomoNavHost)
 * để bubble nổi + menu cheat hiện được ở MỌI tab, không chỉ khi đang mở Cài đặt. Chạm 5 lần vào
 * "Phiên bản" ở Cài đặt bật/tắt bubble; chạm vào bubble mở menu. Chặn phía server (DebugService),
 * đây chỉ là gate hiển thị UI phía client. */
@HiltViewModel
class TesterCheatViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val apiService: ApiService,
    private val timerRepository: TimerRepository,
    private val collectionEventBus: CollectionEventBus,
) : ViewModel() {

    private val _localState = MutableStateFlow(TesterCheatUiState())

    val uiState: StateFlow<TesterCheatUiState> = combine(
        authRepository.accountEmail,
        _localState,
    ) { email, local ->
        local.copy(isTester = email != null && TESTER_EMAIL_REGEX.matches(email))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TesterCheatUiState())

    fun onVersionTapped() {
        _localState.update {
            val count = it.versionTapCount + 1
            if (count >= 5) {
                it.copy(versionTapCount = 0, bubbleVisible = !it.bubbleVisible, showDialog = false)
            } else {
                it.copy(versionTapCount = count)
            }
        }
    }

    fun openDialog() = _localState.update { it.copy(showDialog = true) }
    fun closeDialog() = _localState.update { it.copy(showDialog = false) }
    fun dismissMessage() = _localState.update { it.copy(cheatMessage = null) }

    fun cheatGrantCurrency(currency: String, amount: Int, onGranted: () -> Unit) {
        viewModelScope.launch {
            val result = runCatching { apiService.debugGrantCurrency(GrantCurrencyRequest(currency, amount)) }
            reportCheatResult(result.isSuccess, "+$amount ${currencyLabel(currency)}")
            if (result.isSuccess) onGranted()
        }
    }

    fun cheatGrantMysteryEgg() {
        viewModelScope.launch {
            val result = runCatching { apiService.debugGrantEgg(GrantEggRequest()) }
            reportCheatResult(result.isSuccess, "Trứng Bí Ẩn")
            if (result.isSuccess) collectionEventBus.notifyChanged()
        }
    }

    fun cheatGrantSpecies(rarity: String) {
        viewModelScope.launch {
            val result = runCatching { apiService.debugGrantSpecies(GrantSpeciesRequest(rarity)) }
            val name = result.getOrNull()?.species?.name
            reportCheatResult(result.isSuccess, name ?: "loài cấp $rarity")
            if (result.isSuccess) collectionEventBus.notifyChanged()
        }
    }

    fun cheatFastForwardSession() {
        viewModelScope.launch {
            timerRepository.debugFastForwardActiveSession()
            _localState.update { it.copy(cheatMessage = "Đã kéo phiên đang chạy về còn ~5s") }
        }
    }

    private fun reportCheatResult(success: Boolean, label: String) {
        _localState.update {
            it.copy(cheatMessage = if (success) "Đã nhận: $label" else "Không thực hiện được — thử lại")
        }
    }

    private fun currencyLabel(currency: String) = if (currency == "FOCUS_MINUTE") "phút Giờ tích luỹ" else "Xu Lá"
}
