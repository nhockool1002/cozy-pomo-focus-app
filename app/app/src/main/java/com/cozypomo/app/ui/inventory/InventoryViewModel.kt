package com.cozypomo.app.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cozypomo.app.data.events.CollectionEventBus
import com.cozypomo.app.data.network.ApiService
import com.cozypomo.app.data.network.CreateEggListingRequest
import com.cozypomo.app.data.network.InventoryItemDto
import com.cozypomo.app.data.network.OwnedEggDto
import com.cozypomo.app.data.network.UseItemRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class InventoryTab(val label: String) {
    JAR("Bình"),
    EGG("Trứng"),
    MUSIC("Âm thanh"),
    BOOST("Hỗ trợ"),
}

data class InventoryUiState(
    val tab: InventoryTab = InventoryTab.JAR,
    val jarSkins: List<InventoryItemDto> = emptyList(),
    val musicTracks: List<InventoryItemDto> = emptyList(),
    val boostItems: List<InventoryItemDto> = emptyList(),
    val ownedEggs: List<OwnedEggDto> = emptyList(),
    val loading: Boolean = true,
    /** id món đang chờ PATCH /equip trả về — chặn chạm thêm (cùng món hay món khác) trong lúc
     * chờ, xem [equip]. */
    val pendingEquipId: String? = null,
    /** T-110 — trứng đang mở dialog "Đăng bán" (đặt giá) — null = không mở. */
    val sellDialogFor: OwnedEggDto? = null,
    val sellMessage: String? = null,
    /** Vật phẩm bổ trợ HATCH_MINUTES đang mở modal chọn trứng để áp dụng — null = không mở. */
    val useItemPickerFor: InventoryItemDto? = null,
    val usingItem: Boolean = false,
    val useMessage: String? = null,
)

/** T-099 — Kho đồ (5th tab): gom bình/trứng/nhạc sở hữu vào 1 màn riêng, trước đây rải rác ở
 * Cài đặt (bình/nhạc, T-039) và Khu rừng (trứng, T-084). Trứng chỉ để XEM tiến trình (không có
 * khái niệm "trang bị" — chọn trứng nào ấp là chuyện của mỗi phiên, xem HomeViewModel/EggPickerDialog),
 * bình/nhạc thì trang bị được (radio 1-chọn-1, xem [toggleEquip]). */
@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val apiService: ApiService,
    private val collectionEventBus: CollectionEventBus,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    init {
        load()
        // Trứng cấp/ấp thay đổi từ nơi khác (cheat bubble, hoàn thành phiên ở Trang chủ) — tự
        // tải lại để tab Trứng luôn khớp thực tế, không cần rời rồi quay lại tab.
        collectionEventBus.changes.onEach { load() }.launchIn(viewModelScope)
    }

    fun selectTab(tab: InventoryTab) = _uiState.update { it.copy(tab = tab) }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            val inventoryResult = runCatching { apiService.getInventory() }
            val ownedEggsResult = runCatching { apiService.getOwnedEggs(status = "INCUBATING") }
            val inventory = inventoryResult.getOrNull()
            _uiState.update {
                it.copy(
                    jarSkins = inventory?.filter { item -> item.shopItem.category == "JAR_SKIN" } ?: it.jarSkins,
                    musicTracks = inventory?.filter { item -> item.shopItem.category == "MUSIC" } ?: it.musicTracks,
                    boostItems = inventory?.filter { item -> item.shopItem.category == "BOOST" } ?: it.boostItems,
                    ownedEggs = ownedEggsResult.getOrDefault(it.ownedEggs),
                    loading = false,
                )
            }
        }
    }

    /** Chạm vào món đã trang bị sẵn thì không làm gì (đã là lựa chọn hiện tại) — chỉ món CHƯA
     * trang bị mới gọi API, tránh việc chạm nhầm làm tắt hẳn (0 món nào được trang bị). Đọc lại
     * trạng thái MỚI NHẤT từ [_uiState] theo [itemId] thay vì nhận thẳng `InventoryItemDto` làm
     * tham số — nếu không, lambda `onClick` giữ closure của item cũ (chụp tại lúc render), chạm
     * nhanh 2 lần liên tiếp trong lúc PATCH đầu tiên chưa trả về sẽ đọc nhầm `equipped=false` cũ,
     * gọi thêm 1 lần toggle nữa và tắt luôn về "0 món nào được trang bị" — bug thật đã gặp khi
     * tự kiểm thử. `pendingEquipId` chặn hẳn tap thứ 2 (dù là món nào) trong lúc còn request bay. */
    fun equip(itemId: String) {
        val state = _uiState.value
        if (state.pendingEquipId != null) return
        val target = (state.jarSkins + state.musicTracks).firstOrNull { it.id == itemId } ?: return
        if (target.equipped) return

        _uiState.update { it.copy(pendingEquipId = itemId) }
        viewModelScope.launch {
            runCatching { apiService.toggleEquip(itemId) }.onSuccess {
                collectionEventBus.notifyChanged()
            }
            load()
            _uiState.update { it.copy(pendingEquipId = null) }
        }
    }

    /** T-110 — Đăng bán trứng đang ấp ở Chợ. Backend tự chặn nếu trứng đang gắn 1 phiên RUNNING
     * (client không biết trước điều này nên chỉ hiện lỗi từ server nếu có, không tự đoán). */
    fun openSellDialog(egg: OwnedEggDto) = _uiState.update { it.copy(sellDialogFor = egg) }
    fun closeSellDialog() = _uiState.update { it.copy(sellDialogFor = null) }

    fun confirmSell(priceCoin: Int) {
        val egg = _uiState.value.sellDialogFor ?: return
        _uiState.update { it.copy(sellDialogFor = null) }
        viewModelScope.launch {
            val result = runCatching {
                apiService.createEggListing(
                    CreateEggListingRequest(ownedEggId = egg.id, priceCoin = priceCoin, clientEventId = UUID.randomUUID().toString()),
                )
            }
            _uiState.update {
                it.copy(
                    sellMessage = if (result.isSuccess) "Đã đăng bán ${egg.eggType.name} ở Chợ!" else "Không đăng bán được — trứng có thể đang dùng trong 1 phiên tập trung.",
                )
            }
            if (result.isSuccess) load()
        }
    }

    fun dismissSellMessage() = _uiState.update { it.copy(sellMessage = null) }

    /** Chạm "Dùng" 1 vật phẩm bổ trợ — FOCUS_MINUTES áp dụng ngay (không cần chọn trứng),
     * HATCH_MINUTES mở modal chọn 1 trứng đang ấp trước ([EggPickerDialog], xem [confirmUseItem]). */
    fun requestUseItem(item: InventoryItemDto) {
        if (_uiState.value.usingItem) return
        if (item.shopItem.boostType == "HATCH_MINUTES") {
            _uiState.update { it.copy(useItemPickerFor = item) }
        } else {
            confirmUseItem(item, ownedEggId = null)
        }
    }

    fun dismissUseItemPicker() = _uiState.update { it.copy(useItemPickerFor = null) }

    fun confirmUseItem(item: InventoryItemDto, ownedEggId: String?) {
        _uiState.update { it.copy(useItemPickerFor = null, usingItem = true) }
        viewModelScope.launch {
            val result = runCatching { apiService.useInventoryItem(item.id, UseItemRequest(ownedEggId)) }
            val message = result.fold(
                onSuccess = { res ->
                    when {
                        res.hatched -> "${item.shopItem.name}: trứng vừa nở ra ${res.resultSpecies?.name}!"
                        res.kind == "FOCUS_MINUTES" -> "+${res.amount} phút Giờ tích luỹ!"
                        else -> "Đã ấp thêm ${res.amount} phút cho trứng."
                    }
                },
                onFailure = { "Không dùng được vật phẩm này — thử lại sau." },
            )
            _uiState.update { it.copy(usingItem = false, useMessage = message) }
            if (result.isSuccess) {
                collectionEventBus.notifyChanged()
                load()
            }
        }
    }

    fun dismissUseMessage() = _uiState.update { it.copy(useMessage = null) }
}
