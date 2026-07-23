package com.cozypomo.app.ui.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cozypomo.app.data.network.ApiService
import com.cozypomo.app.data.network.InventoryItemDto
import com.cozypomo.app.data.network.PurchaseRequest
import com.cozypomo.app.data.network.ShopItemDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class ShopCategoryTab(val label: String, val backendValue: String) {
    EGG("Trứng mới", "EGG"),
    JAR_SKIN("Bình thuỷ tinh", "JAR_SKIN"),
    MUSIC("Nhạc nền", "MUSIC"),
}

data class ShopUiState(
    val category: ShopCategoryTab = ShopCategoryTab.EGG,
    val items: List<ShopItemDto> = emptyList(),
    val ownedShopItemIds: Set<String> = emptySet(),
    val loading: Boolean = true,
    val payWithChoiceFor: ShopItemDto? = null,
    val purchasing: Boolean = false,
    val lastMessage: String? = null,
)

/** T-037 — S-05 Cửa hàng: mua trứng (Xu Lá hoặc Giờ tích luỹ), bình/nhạc (chỉ Xu Lá, 1 lần).
 * Số dư đọc từ [com.cozypomo.app.ui.common.CurrencyViewModel] dùng chung (truyền vào từ ShopScreen), không tự tải riêng. */
@HiltViewModel
class ShopViewModel @Inject constructor(
    private val apiService: ApiService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShopUiState())
    val uiState: StateFlow<ShopUiState> = _uiState.asStateFlow()

    init {
        selectCategory(ShopCategoryTab.EGG)
        loadInventory()
    }

    fun selectCategory(category: ShopCategoryTab) {
        _uiState.update { it.copy(category = category, loading = true) }
        viewModelScope.launch {
            val items = runCatching { apiService.getShopItems(category.backendValue) }.getOrDefault(emptyList())
            _uiState.update { it.copy(items = items, loading = false) }
        }
    }

    private fun loadInventory() {
        viewModelScope.launch {
            runCatching { apiService.getInventory() }.onSuccess { inventory ->
                _uiState.update { it.copy(ownedShopItemIds = inventory.map(InventoryItemDto::shopItemId).toSet()) }
            }
        }
    }

    /** Vật phẩm EGG cho chọn trả bằng Xu Lá hay Giờ tích luỹ trước khi mua — mở dialog xác nhận. */
    fun requestPurchase(item: ShopItemDto) {
        if (item.category == "EGG") {
            _uiState.update { it.copy(payWithChoiceFor = item) }
        } else {
            purchase(item, payWith = null)
        }
    }

    fun dismissPayWithChoice() = _uiState.update { it.copy(payWithChoiceFor = null) }

    fun confirmEggPurchase(item: ShopItemDto, payWith: String) {
        _uiState.update { it.copy(payWithChoiceFor = null) }
        purchase(item, payWith)
    }

    private fun purchase(item: ShopItemDto, payWith: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(purchasing = true) }
            val result = runCatching {
                apiService.purchaseShopItem(item.id, PurchaseRequest(clientEventId = UUID.randomUUID().toString(), payWith = payWith)).close()
            }
            _uiState.update {
                it.copy(
                    purchasing = false,
                    lastMessage = if (result.isSuccess) "Đã mua ${item.name}!" else "Mua không thành công — kiểm tra lại số dư.",
                )
            }
            if (result.isSuccess) {
                loadInventory()
                selectCategory(_uiState.value.category)
            }
        }
    }

    fun dismissMessage() = _uiState.update { it.copy(lastMessage = null) }
}
