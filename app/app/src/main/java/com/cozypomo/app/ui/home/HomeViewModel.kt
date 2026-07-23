package com.cozypomo.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cozypomo.app.data.auth.AuthRepository
import com.cozypomo.app.data.network.ApiService
import com.cozypomo.app.data.network.OwnedEggDto
import com.cozypomo.app.data.timer.SessionCompletionResult
import com.cozypomo.app.data.timer.SessionUiState
import com.cozypomo.app.data.timer.TimerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Kết quả để hiện modal — bọc thêm case Bỏ cuộc (không đến từ TimerRepository.completionEvents). */
sealed interface SessionResultUi {
    data class Completed(val result: SessionCompletionResult) : SessionResultUi
    /** Bỏ cuộc KHÔNG làm mất/vỡ trứng — chỉ không nhận Xu Lá/Giờ tích luỹ phiên này. */
    data class GaveUp(val eggTypeName: String?) : SessionResultUi
}

data class HomeUiState(
    val durationMin: Int = 25,
    val ownedEggs: List<OwnedEggDto> = emptyList(),
    val selectedOwnedEgg: OwnedEggDto? = null,
    /** % thời gian phiên dành cho ấp trứng — phần còn lại quy đổi theo [rewardCurrency]. Chỉ áp dụng khi có trứng chọn. */
    val incubationRatio: Float = 1f,
    /** "COIN" hoặc "FOCUS_MINUTE" — phần thời gian không dành cho ấp trứng chỉ nhận CHỈ 1 loại tiền này, người dùng chọn trước khi bắt đầu. */
    val rewardCurrency: String = "COIN",
    val showEggPicker: Boolean = false,
    val showGiveUpConfirm: Boolean = false,
    val sessionResult: SessionResultUi? = null,
)

/** S-01 — bọc TimerRepository cho UI (T-031). Trứng sở hữu đọc qua GET /owned-eggs (T-032/Shop
 * đầy đủ vẫn là việc riêng); Xu Lá/Giờ tích luỹ hiện qua bubble dùng chung [com.cozypomo.app.ui.common.CurrencyViewModel]. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val timerRepository: TimerRepository,
    private val authRepository: AuthRepository,
    private val apiService: ApiService,
) : ViewModel() {

    val sessionState: StateFlow<SessionUiState> = timerRepository.observeActiveSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SessionUiState.Idle)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { timerRepository.ensureServiceRunningIfActive() }
        loadOwnedEggs()
        viewModelScope.launch {
            timerRepository.completionEvents.collect { result ->
                _uiState.update { it.copy(sessionResult = SessionResultUi.Completed(result)) }
                loadOwnedEggs()
            }
        }
    }

    private fun loadOwnedEggs() {
        viewModelScope.launch {
            runCatching { apiService.getOwnedEggs(status = "INCUBATING") }.onSuccess { eggs ->
                _uiState.update { current ->
                    val stillValid = current.selectedOwnedEgg?.let { sel -> eggs.any { it.id == sel.id } } == true
                    current.copy(
                        ownedEggs = eggs,
                        selectedOwnedEgg = if (stillValid) current.selectedOwnedEgg else null,
                    )
                }
            }
        }
    }

    fun onDurationChange(minutes: Int) = _uiState.update { it.copy(durationMin = minutes) }
    fun onIncubationRatioChange(ratio: Float) = _uiState.update { it.copy(incubationRatio = ratio) }
    fun onRewardCurrencyChange(currency: String) = _uiState.update { it.copy(rewardCurrency = currency) }

    fun openEggPicker() = _uiState.update { it.copy(showEggPicker = true) }
    fun closeEggPicker() = _uiState.update { it.copy(showEggPicker = false) }

    /** null = tập trung không ấp trứng nào ("hoặc không chọn cũng được"). */
    fun selectOwnedEgg(egg: OwnedEggDto?) =
        _uiState.update { it.copy(selectedOwnedEgg = egg, showEggPicker = false) }

    fun requestGiveUp() = _uiState.update { it.copy(showGiveUpConfirm = true) }
    fun dismissGiveUp() = _uiState.update { it.copy(showGiveUpConfirm = false) }

    fun confirmGiveUp() {
        val current = sessionState.value
        val egg = _uiState.value.selectedOwnedEgg
        _uiState.update { it.copy(showGiveUpConfirm = false) }
        if (current is SessionUiState.Running) {
            viewModelScope.launch {
                timerRepository.giveUpSession(current.sessionId)
                _uiState.update { it.copy(sessionResult = SessionResultUi.GaveUp(egg?.eggType?.name)) }
            }
        }
    }

    fun startSession() {
        val state = _uiState.value
        viewModelScope.launch {
            timerRepository.startSession(
                durationMin = state.durationMin,
                ownedEggId = state.selectedOwnedEgg?.id,
                incubationRatio = state.selectedOwnedEgg?.let { state.incubationRatio },
                rewardCurrency = state.rewardCurrency,
                strictMode = true,
            )
        }
    }

    fun dismissSessionResult() = _uiState.update { it.copy(sessionResult = null) }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onDone()
        }
    }
}
