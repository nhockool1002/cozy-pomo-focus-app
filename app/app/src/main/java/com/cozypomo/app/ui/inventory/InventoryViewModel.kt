package com.cozypomo.app.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cozypomo.app.data.events.CollectionEventBus
import com.cozypomo.app.data.network.ApiService
import com.cozypomo.app.data.network.InventoryItemDto
import com.cozypomo.app.data.network.OwnedEggDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class InventoryTab(val label: String) {
    JAR("Bình"),
    EGG("Trứng"),
    MUSIC("Âm thanh"),
}

data class InventoryUiState(
    val tab: InventoryTab = InventoryTab.JAR,
    val jarSkins: List<InventoryItemDto> = emptyList(),
    val musicTracks: List<InventoryItemDto> = emptyList(),
    val ownedEggs: List<OwnedEggDto> = emptyList(),
    val loading: Boolean = true,
    /** id món đang chờ PATCH /equip trả về — chặn chạm thêm (cùng món hay món khác) trong lúc
     * chờ, xem [equip]. */
    val pendingEquipId: String? = null,
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
}
