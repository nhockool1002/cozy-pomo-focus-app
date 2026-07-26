package com.cozypomo.app.ui.forest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cozypomo.app.data.events.CollectionEventBus
import com.cozypomo.app.data.network.ApiService
import com.cozypomo.app.data.network.CollectionEntryDto
import com.cozypomo.app.data.network.CollectionProgressDto
import com.cozypomo.app.data.network.CreateSpeciesListingRequest
import com.cozypomo.app.data.network.SpeciesDto
import com.cozypomo.app.data.sound.SoundManager
import com.cozypomo.app.data.timer.SessionUiState
import com.cozypomo.app.data.timer.TimerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class SpeciesCategoryFilter(val label: String, val backendValue: String?) {
    ALL("Tất cả", null),
    FOREST("Thú rừng", "FOREST"),
    SEA("Sinh vật biển", "SEA"),
    PLANT("Thực vật", "PLANT"),
}

/** T-113 (Nhóm G) — 2 chế độ xem khác mục đích: Lưới là "sổ sưu tập" (hiện cả ô khoá), Khu vườn
 * là "khoe vườn của tôi" (chỉ hiện loài đang sở hữu). Không lưu bền — chỉ là lựa chọn UI phiên
 * hiện tại, giống cách chọn tab category không lưu bền. */
enum class ForestViewMode(val label: String) {
    GRID("Lưới"),
    GARDEN("Khu vườn"),
}

data class ForestUiState(
    val viewMode: ForestViewMode = ForestViewMode.GRID,
    val category: SpeciesCategoryFilter = SpeciesCategoryFilter.ALL,
    val allSpecies: List<SpeciesDto> = emptyList(),
    val collectionBySpeciesId: Map<String, CollectionEntryDto> = emptyMap(),
    val progress: CollectionProgressDto? = null,
    val loading: Boolean = true,
    val selectedSpeciesId: String? = null,
    /** T-110 — loài đang mở dialog "Đăng bán" (đặt giá) — null = không mở. */
    val sellDialogFor: CollectionEntryDto? = null,
    val listingInFlight: Boolean = false,
    val sellMessage: String? = null,
) {
    val visibleSpecies: List<SpeciesDto>
        get() = if (category.backendValue == null) allSpecies else allSpecies.filter { it.category == category.backendValue }
}

/** T-035 — Khu rừng/Bộ sưu tập (S-04). Tải toàn bộ loài + collection 1 lần, lọc theo tab ở client.
 * Trứng sở hữu ("Kho Trứng") đã chuyển sang màn Kho đồ riêng (T-099, `ui/inventory/`) — không
 * còn là 1 tab lọc ở đây nữa. */
@HiltViewModel
class ForestViewModel @Inject constructor(
    private val apiService: ApiService,
    private val collectionEventBus: CollectionEventBus,
    private val soundManager: SoundManager,
    private val timerRepository: TimerRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForestUiState())
    val uiState: StateFlow<ForestUiState> = _uiState.asStateFlow()

    /** T-114 — chỉ true nếu CHÍNH chế độ Khu vườn là bên đã bật ambient — tránh lỡ tắt nhạc nền
     * của 1 phiên tập trung đang chạy độc lập (xem [updateAmbientSound]). */
    private var gardenAmbientActive = false

    init {
        load()
        // Tự tải lại khi có thay đổi từ nơi khác (VD bubble cheat cấp loài/trứng) — nếu không,
        // loài/trứng vừa được cấp không hiện ngay, phải rời màn rồi quay lại mới thấy.
        collectionEventBus.changes.onEach { load() }.launchIn(viewModelScope)
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            val speciesResult = runCatching { apiService.getSpecies() }
            val collectionResult = runCatching { apiService.getCollection() }
            val progressResult = runCatching { apiService.getCollectionProgress() }
            _uiState.update {
                it.copy(
                    allSpecies = speciesResult.getOrDefault(it.allSpecies),
                    collectionBySpeciesId = collectionResult.getOrNull()?.associateBy { entry -> entry.speciesId } ?: it.collectionBySpeciesId,
                    progress = progressResult.getOrNull() ?: it.progress,
                    loading = false,
                )
            }
        }
    }

    fun selectCategory(category: SpeciesCategoryFilter) = _uiState.update { it.copy(category = category) }

    fun selectViewMode(mode: ForestViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
        updateAmbientSound(mode)
    }

    /** T-114 — phát nhạc nền khi vào chế độ Khu vườn (tái dùng SoundManager từ T-042, dùng lại
     * `ambient_forest.ogg` đã có sẵn), tự dừng khi rời chế độ đó. Không phát chồng nếu đang có 1
     * phiên tập trung RUNNING (đã tự phát ambient theo `soundTheme` riêng của phiên đó rồi). */
    private fun updateAmbientSound(mode: ForestViewMode) {
        viewModelScope.launch {
            if (mode == ForestViewMode.GARDEN) {
                val sessionRunning = timerRepository.observeActiveSession().first() is SessionUiState.Running
                if (!sessionRunning) {
                    soundManager.playAmbientTrack("forest")
                    gardenAmbientActive = true
                }
            } else if (gardenAmbientActive) {
                soundManager.stopAmbientTrack()
                gardenAmbientActive = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (gardenAmbientActive) {
            soundManager.stopAmbientTrack()
            gardenAmbientActive = false
        }
    }

    fun openSpecies(speciesId: String) {
        if (_uiState.value.collectionBySpeciesId.containsKey(speciesId)) {
            _uiState.update { it.copy(selectedSpeciesId = speciesId) }
        }
    }

    fun closeSpecies() = _uiState.update { it.copy(selectedSpeciesId = null) }

    fun toggleFavorite(speciesId: String) {
        viewModelScope.launch {
            runCatching { apiService.toggleFavorite(speciesId) }.onSuccess { updated ->
                _uiState.update { state ->
                    val existing = state.collectionBySpeciesId[speciesId] ?: return@update state
                    val merged = existing.copy(hatchCount = updated.hatchCount, isFavorite = updated.isFavorite)
                    state.copy(collectionBySpeciesId = state.collectionBySpeciesId + (speciesId to merged))
                }
            }
        }
    }

    /** T-110 — Đăng bán (thú/thực vật) từ Chi tiết loài. Hiện bất cứ khi nào `ownedCount ≥ 1`
     * (không cần "dư" — có thể bán tới bản cuối cùng, xem plan.md Nhóm F). */
    fun openSellDialog(entry: CollectionEntryDto) = _uiState.update { it.copy(sellDialogFor = entry, selectedSpeciesId = null) }
    fun closeSellDialog() = _uiState.update { it.copy(sellDialogFor = null) }

    fun confirmSell(priceCoin: Int) {
        val entry = _uiState.value.sellDialogFor ?: return
        _uiState.update { it.copy(sellDialogFor = null, listingInFlight = true) }
        viewModelScope.launch {
            val result = runCatching {
                apiService.createSpeciesListing(
                    CreateSpeciesListingRequest(speciesId = entry.speciesId, priceCoin = priceCoin, clientEventId = UUID.randomUUID().toString()),
                )
            }
            _uiState.update {
                it.copy(
                    listingInFlight = false,
                    sellMessage = result.fold(
                        onSuccess = { listing ->
                            if (listing.status == "PENDING_APPROVAL") {
                                "Đã gửi tin đăng — loài hiếm cần Admin duyệt trước khi hiện ở Chợ."
                            } else {
                                "Đã đăng bán ${entry.species.name} ở Chợ!"
                            }
                        },
                        onFailure = { "Không đăng bán được — có thể bạn đã đăng hết số bản sở hữu." },
                    ),
                )
            }
        }
    }

    fun dismissSellMessage() = _uiState.update { it.copy(sellMessage = null) }
}
