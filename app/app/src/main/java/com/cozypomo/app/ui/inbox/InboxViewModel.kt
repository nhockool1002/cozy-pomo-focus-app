package com.cozypomo.app.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cozypomo.app.data.network.ApiService
import com.cozypomo.app.data.network.InboxMessageDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InboxUiState(
    val messages: List<InboxMessageDto> = emptyList(),
    val unreadCount: Int = 0,
    val loading: Boolean = false,
)

private const val POLL_INTERVAL_MS = 30_000L

/**
 * Nguồn sự thật DUY NHẤT cho số chưa đọc — tạo 1 lần ở [com.cozypomo.app.ui.navigation.CozyPomoNavHost]
 * (giống [com.cozypomo.app.ui.common.CurrencyViewModel]) để badge chuông hiện đúng ở MỌI tab.
 * Không có push (FCM) ở v1 nên tự poll `unread-count` mỗi 30s trong lúc app đang mở — đủ dùng vì
 * thư chỉ phát sinh từ hành động chính người dùng vừa làm (mua/ấp trứng) hoặc admin tặng quà thỉnh
 * thoảng, không cần cập nhật tức thời.
 */
@HiltViewModel
class InboxViewModel @Inject constructor(
    private val apiService: ApiService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InboxUiState())
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()

    init {
        refreshUnreadCount()
        viewModelScope.launch {
            while (true) {
                delay(POLL_INTERVAL_MS)
                refreshUnreadCount()
            }
        }
    }

    fun refreshUnreadCount() {
        viewModelScope.launch {
            runCatching { apiService.getInboxUnreadCount() }.onSuccess { response ->
                _uiState.update { it.copy(unreadCount = response.count) }
            }
            // Lỗi mạng/401: giữ nguyên số cũ, giống CurrencyViewModel — tránh badge nhảy về 0 sai.
        }
    }

    fun loadMessages() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            runCatching { apiService.getInbox() }
                .onSuccess { messages -> _uiState.update { it.copy(messages = messages, loading = false) } }
                .onFailure { _uiState.update { it.copy(loading = false) } }
        }
    }

    fun markRead(id: String) {
        val alreadyRead = _uiState.value.messages.find { it.id == id }?.isRead == true
        if (alreadyRead) return
        viewModelScope.launch {
            runCatching { apiService.markInboxRead(id) }.onSuccess {
                _uiState.update { state ->
                    state.copy(
                        messages = state.messages.map { if (it.id == id) it.copy(isRead = true) else it },
                        unreadCount = maxOf(0, state.unreadCount - 1),
                    )
                }
            }
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            runCatching { apiService.markAllInboxRead() }.onSuccess {
                _uiState.update { state ->
                    state.copy(messages = state.messages.map { it.copy(isRead = true) }, unreadCount = 0)
                }
            }
        }
    }
}
