package com.cozypomo.app.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cozypomo.app.data.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CurrencyUiState(
    val coinBalance: Int? = null,
    val focusMinutesBalance: Int? = null,
)

/**
 * Nguồn sự thật DUY NHẤT cho số dư Xu Lá/Giờ tích luỹ — tạo 1 lần ở [CozyPomoNavHost] (scope theo
 * NavBackStackEntry "main", sống suốt khi chuyển tab) thay vì mỗi màn tự gọi GET /currency/balance
 * riêng. Sửa lỗi hiện "..." mãi mãi ở màn khác Home: giữ nguyên giá trị cũ đã tải được thay vì
 * reset về null mỗi lần vào lại màn hình — chỉ cập nhật khi có dữ liệu mới thật sự.
 */
@HiltViewModel
class CurrencyViewModel @Inject constructor(
    private val apiService: ApiService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CurrencyUiState())
    val uiState: StateFlow<CurrencyUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            runCatching { apiService.getBalance() }.onSuccess { response ->
                _uiState.update { it.copy(coinBalance = response.balance, focusMinutesBalance = response.focusMinutes) }
            }
            // Lỗi mạng/401: giữ nguyên giá trị cũ đã có (nếu có) thay vì xoá về null — tránh hiện lại "...".
        }
    }
}
