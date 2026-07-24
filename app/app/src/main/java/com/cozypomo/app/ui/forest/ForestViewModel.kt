package com.cozypomo.app.ui.forest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cozypomo.app.data.events.CollectionEventBus
import com.cozypomo.app.data.network.ApiService
import com.cozypomo.app.data.network.CollectionEntryDto
import com.cozypomo.app.data.network.CollectionProgressDto
import com.cozypomo.app.data.network.SpeciesDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SpeciesCategoryFilter(val label: String, val backendValue: String?) {
    ALL("Tất cả", null),
    FOREST("Thú rừng", "FOREST"),
    SEA("Sinh vật biển", "SEA"),
    PLANT("Thực vật", "PLANT"),
}

data class ForestUiState(
    val category: SpeciesCategoryFilter = SpeciesCategoryFilter.ALL,
    val allSpecies: List<SpeciesDto> = emptyList(),
    val collectionBySpeciesId: Map<String, CollectionEntryDto> = emptyMap(),
    val progress: CollectionProgressDto? = null,
    val loading: Boolean = true,
    val selectedSpeciesId: String? = null,
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForestUiState())
    val uiState: StateFlow<ForestUiState> = _uiState.asStateFlow()

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
}
