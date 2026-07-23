package com.cozypomo.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cozypomo.app.data.network.ApiService
import com.cozypomo.app.data.network.EggTypeDto
import com.cozypomo.app.data.timer.HatchResult
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

data class HomeUiState(
    val durationMin: Int = 25,
    val eggTypes: List<EggTypeDto> = emptyList(),
    val selectedEggType: EggTypeDto? = null,
    val showEggPicker: Boolean = false,
    val showGiveUpConfirm: Boolean = false,
    val coinBalance: Int? = null,
    val lastHatch: HatchResult? = null,
)

/** S-01 — bọc TimerRepository cho UI (T-031). Trứng/Xu Lá chỉ đọc thô qua ApiService vì
 * EggRepository/CurrencyRepository đầy đủ (chọn/khoá theo sở hữu, ledger) là T-032/T-034. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val timerRepository: TimerRepository,
    private val apiService: ApiService,
) : ViewModel() {

    val sessionState: StateFlow<SessionUiState> = timerRepository.observeActiveSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SessionUiState.Idle)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { timerRepository.ensureServiceRunningIfActive() }
        loadEggTypes()
        loadBalance()
        viewModelScope.launch {
            timerRepository.hatchEvents.collect { result ->
                _uiState.update { it.copy(lastHatch = result) }
                loadBalance()
            }
        }
    }

    private fun loadEggTypes() {
        viewModelScope.launch {
            runCatching { apiService.getEggTypes() }.onSuccess { types ->
                _uiState.update { current ->
                    current.copy(
                        eggTypes = types,
                        selectedEggType = current.selectedEggType ?: types.firstOrNull(),
                    )
                }
            }
        }
    }

    private fun loadBalance() {
        viewModelScope.launch {
            runCatching { apiService.getBalance() }.onSuccess { response ->
                _uiState.update { it.copy(coinBalance = response.balance) }
            }
        }
    }

    fun onDurationChange(minutes: Int) = _uiState.update { it.copy(durationMin = minutes) }

    fun openEggPicker() = _uiState.update { it.copy(showEggPicker = true) }
    fun closeEggPicker() = _uiState.update { it.copy(showEggPicker = false) }
    fun selectEggType(eggType: EggTypeDto) =
        _uiState.update { it.copy(selectedEggType = eggType, showEggPicker = false) }

    fun requestGiveUp() = _uiState.update { it.copy(showGiveUpConfirm = true) }
    fun dismissGiveUp() = _uiState.update { it.copy(showGiveUpConfirm = false) }

    fun confirmGiveUp() {
        val current = sessionState.value
        if (current is SessionUiState.Running) {
            viewModelScope.launch { timerRepository.giveUpSession(current.sessionId) }
        }
        _uiState.update { it.copy(showGiveUpConfirm = false) }
    }

    fun startSession() {
        val eggTypeId = _uiState.value.selectedEggType?.id ?: return
        viewModelScope.launch {
            timerRepository.startSession(_uiState.value.durationMin, eggTypeId, strictMode = true)
        }
    }

    fun consumeHatchResult() = _uiState.update { it.copy(lastHatch = null) }
}
