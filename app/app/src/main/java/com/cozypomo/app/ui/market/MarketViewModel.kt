package com.cozypomo.app.ui.market

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cozypomo.app.data.network.ApiService
import com.cozypomo.app.data.network.BuyListingRequest
import com.cozypomo.app.data.network.MarketListingDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class MarketTab(val label: String) {
    ALL("Tất cả"),
    MINE("Của tôi"),
}

data class MarketUiState(
    val tab: MarketTab = MarketTab.ALL,
    val listings: List<MarketListingDto> = emptyList(),
    val loading: Boolean = true,
    val confirmBuyFor: MarketListingDto? = null,
    val buying: Boolean = false,
    val lastMessage: String? = null,
    /** % phí giao dịch (GameSettings.marketFeePercent, mặc định 10) — đọc thật từ backend để hiện
     * đúng số Xu người bán THỰC NHẬN ở tab "Của tôi" (không hardcode 10%, admin có thể đổi). */
    val marketFeePercent: Float = 10f,
)

/** T-109 — S-08 Chợ: danh sách tin đăng chung (thú/thực vật/trứng, không phân biệt Admin/user
 * bằng badge riêng — xem wireframe market-feature.html). Tab "Của tôi" trả TOÀN BỘ trạng thái
 * (kể cả PENDING_APPROVAL/REJECTED/SOLD) để tự theo dõi tin của mình, tab "Tất cả" chỉ ACTIVE. */
@HiltViewModel
class MarketViewModel @Inject constructor(
    private val apiService: ApiService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MarketUiState())
    val uiState: StateFlow<MarketUiState> = _uiState.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            runCatching { apiService.getGameSettings() }.onSuccess { settings ->
                _uiState.update { it.copy(marketFeePercent = settings.marketFeePercent) }
            }
        }
    }

    fun selectTab(tab: MarketTab) {
        _uiState.update { it.copy(tab = tab) }
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            val mine = _uiState.value.tab == MarketTab.MINE
            val listings = runCatching { apiService.getMarketListings(mine = if (mine) true else null) }.getOrDefault(emptyList())
            _uiState.update { it.copy(listings = listings, loading = false) }
        }
    }

    fun requestBuy(listing: MarketListingDto) = _uiState.update { it.copy(confirmBuyFor = listing) }
    fun dismissBuyConfirm() = _uiState.update { it.copy(confirmBuyFor = null) }

    fun confirmBuy() {
        val listing = _uiState.value.confirmBuyFor ?: return
        _uiState.update { it.copy(confirmBuyFor = null, buying = true) }
        viewModelScope.launch {
            val result = runCatching {
                apiService.buyListing(listing.id, BuyListingRequest(clientEventId = UUID.randomUUID().toString()))
            }
            _uiState.update {
                it.copy(
                    buying = false,
                    lastMessage = if (result.isSuccess) "Đã mua thành công!" else "Mua không thành công — kiểm tra lại số dư hoặc tin đăng còn không.",
                )
            }
            load()
        }
    }

    fun cancelListing(listingId: String) {
        viewModelScope.launch {
            runCatching { apiService.cancelListing(listingId) }
            load()
        }
    }

    fun dismissMessage() = _uiState.update { it.copy(lastMessage = null) }
}
