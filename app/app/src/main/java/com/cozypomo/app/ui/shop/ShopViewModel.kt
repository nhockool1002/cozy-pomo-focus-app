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
    BOOST("Vật phẩm hỗ trợ", "BOOST"),
}

data class ShopUiState(
    val category: ShopCategoryTab = ShopCategoryTab.EGG,
    val items: List<ShopItemDto> = emptyList(),
    val ownedShopItemIds: Set<String> = emptySet(),
    val loading: Boolean = true,
    val purchasing: Boolean = false,
    val lastMessage: String? = null,
    /** Vật phẩm đang mở modal xem chi tiết (chạm vào hàng) — null = không mở. */
    val detailFor: ShopItemDto? = null,
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
                // BOOST loại trừ khỏi "đã sở hữu" — vật phẩm bổ trợ mua được nhiều lần/cộng dồn
                // quantity, không phải mua 1 lần như JAR_SKIN/MUSIC (nếu không, nút "Mua ngay" sẽ
                // bị khoá vĩnh viễn thành "Đã sở hữu" sau lần mua đầu tiên).
                val ownedIds = inventory
                    .filter { it.shopItem.category != "BOOST" }
                    .map(InventoryItemDto::shopItemId)
                    .toSet()
                _uiState.update { it.copy(ownedShopItemIds = ownedIds) }
            }
        }
    }

    /** Vật phẩm không phải EGG chỉ trả bằng Xu Lá, 1 nút "Mua ngay" duy nhất — xem [buyEggWith]
     * cho vật phẩm EGG (2 nút riêng theo loại tiền, không qua dialog trung gian nữa). */
    fun requestPurchase(item: ShopItemDto) = purchase(item, payWith = null)

    /** Nút mua trứng theo đúng 1 loại tiền cụ thể (icon Xu Lá hoặc Giờ tích luỹ) — mua ngay, không
     * còn dialog "Trả bằng gì?" ở giữa (T-125, gộp thẳng lựa chọn tiền vào hàng/modal chi tiết). */
    fun buyEggWith(item: ShopItemDto, payWith: String) = purchase(item, payWith)

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

    fun showDetail(item: ShopItemDto) = _uiState.update { it.copy(detailFor = item) }
    fun dismissDetail() = _uiState.update { it.copy(detailFor = null) }

    /** Gọi từ nút "Mua ngay" trong modal chi tiết (danh mục khác EGG) — đóng modal rồi mua thẳng. */
    fun requestPurchaseFromDetail(item: ShopItemDto) {
        _uiState.update { it.copy(detailFor = null) }
        requestPurchase(item)
    }

    /** Gọi từ 2 nút chọn tiền trong modal chi tiết của vật phẩm EGG — đóng modal rồi mua thẳng. */
    fun buyEggWithFromDetail(item: ShopItemDto, payWith: String) {
        _uiState.update { it.copy(detailFor = null) }
        buyEggWith(item, payWith)
    }
}
