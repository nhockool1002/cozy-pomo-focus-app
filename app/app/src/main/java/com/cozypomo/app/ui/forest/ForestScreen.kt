package com.cozypomo.app.ui.forest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cozypomo.app.data.network.SpeciesDto
import com.cozypomo.app.ui.common.MessageDialog
import com.cozypomo.app.ui.common.RarityBadge
import com.cozypomo.app.ui.common.SetPriceDialog
import com.cozypomo.app.ui.common.SpeciesArtIcon

/** T-035 — S-04 Khu rừng/Bộ sưu tập: lưới loài đã/chưa mở khoá, lọc theo nhóm. */
@Composable
fun ForestScreen(viewModel: ForestViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Khu rừng của tôi", style = MaterialTheme.typography.headlineSmall)
                    uiState.progress?.let { progress ->
                        Text(
                            "${progress.unlocked}/${progress.total} đã mở khoá",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                ViewModeToggle(mode = uiState.viewMode, onSelect = viewModel::selectViewMode)
            }
        }

        // T-113 — tab category chỉ có ý nghĩa ở chế độ Lưới; Khu vườn đã tự hiện đủ 4 khu cùng
        // lúc (cuộn dọc), không cần lọc thêm.
        if (uiState.viewMode == ForestViewMode.GRID) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SpeciesCategoryFilter.entries.forEach { category ->
                    FilterChip(
                        selected = uiState.category == category,
                        onClick = { viewModel.selectCategory(category) },
                        label = { Text(category.label, maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                }
            }
        }

        if (uiState.loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.viewMode == ForestViewMode.GARDEN) {
            GardenMapView(
                entries = uiState.collectionBySpeciesId.values.toList(),
                onOpenSpecies = viewModel::openSpecies,
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(uiState.visibleSpecies, key = { it.id }) { species ->
                    val entry = uiState.collectionBySpeciesId[species.id]
                    if (entry != null) {
                        UnlockedSpeciesCard(species = species, hatchCount = entry.hatchCount, onClick = { viewModel.openSpecies(species.id) })
                    } else {
                        LockedSpeciesCard()
                    }
                }
            }
        }
    }

    uiState.selectedSpeciesId?.let { speciesId ->
        uiState.collectionBySpeciesId[speciesId]?.let { entry ->
            SpeciesDetailDialog(
                entry = entry,
                onDismiss = viewModel::closeSpecies,
                onToggleFavorite = { viewModel.toggleFavorite(speciesId) },
                onSell = { viewModel.openSellDialog(entry) },
            )
        }
    }

    uiState.sellDialogFor?.let { entry ->
        SetPriceDialog(
            title = "Đăng bán ${entry.species.name}",
            subtitle = "Bạn đang sở hữu ${entry.ownedCount} bản — đăng bán 1 bản ở Chợ",
            onConfirm = viewModel::confirmSell,
            onDismiss = viewModel::closeSellDialog,
        )
    }

    uiState.sellMessage?.let { message ->
        MessageDialog(message = message, onDismiss = viewModel::dismissSellMessage)
    }
}

/** T-113 — toggle 2 icon Lưới ⇄ Khu vườn cạnh tiêu đề màn. */
@Composable
private fun ViewModeToggle(mode: ForestViewMode, onSelect: (ForestViewMode) -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Row {
            IconButton(onClick = { onSelect(ForestViewMode.GRID) }) {
                Icon(
                    Icons.Filled.GridView,
                    contentDescription = ForestViewMode.GRID.label,
                    tint = if (mode == ForestViewMode.GRID) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { onSelect(ForestViewMode.GARDEN) }) {
                Icon(
                    Icons.Filled.Yard,
                    contentDescription = ForestViewMode.GARDEN.label,
                    tint = if (mode == ForestViewMode.GARDEN) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Chiều cao cố định cho mọi thẻ trong lưới (khoá lẫn đã mở khoá) — tránh lệch hàng khi nội
 * dung thẻ đã mở khoá (badge + icon + tên + số lần nở) cao hơn thẻ khoá (chỉ có 1 vòng tròn "?"). */
private val SpeciesCardHeight = 134.dp
private val SpeciesIconBackdropSize = 56.dp

@Composable
private fun UnlockedSpeciesCard(species: SpeciesDto, hatchCount: Int, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(SpeciesCardHeight),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(top = 10.dp, bottom = 8.dp, start = 4.dp, end = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Nền tròn tương phản phía sau icon — nếu không, vầng hào quang cấp B/A (alpha rất
                // thấp, cùng tông với surfaceContainer của thẻ) gần như vô hình trên nền thẻ.
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest, modifier = Modifier.size(SpeciesIconBackdropSize)) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        SpeciesArtIcon(
                            category = species.category,
                            archetype = species.archetype,
                            paletteIdx = species.paletteIdx,
                            seed = species.name,
                            rarity = species.rarity,
                            size = 42.dp,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    species.name,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "x$hatchCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Badge cấp bậc đặt nổi ở góc thẻ (không chiếm chỗ trong Column) — vừa tinh tế hơn
            // kiểu xếp hàng trên cùng, vừa loại bỏ chênh lệch chiều cao giữa các thẻ khác cấp bậc.
            RarityBadge(
                species.rarity,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 8.dp, top = 8.dp)
                    .shadow(2.dp, RoundedCornerShape(50)),
            )
        }
    }
}

@Composable
private fun LockedSpeciesCard() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth().height(SpeciesCardHeight),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), modifier = Modifier.size(34.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("?", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
