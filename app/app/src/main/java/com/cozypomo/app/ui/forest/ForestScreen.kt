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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import com.cozypomo.app.ui.common.RarityBadge
import com.cozypomo.app.ui.common.SpeciesArtIcon

/** T-035 — S-04 Khu rừng/Bộ sưu tập: lưới loài đã/chưa mở khoá, lọc theo nhóm. */
@Composable
fun ForestScreen(viewModel: ForestViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text("Khu rừng của tôi", style = MaterialTheme.typography.headlineSmall)
            uiState.progress?.let { progress ->
                Text(
                    "${progress.unlocked}/${progress.total} đã mở khoá",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

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

        if (uiState.loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
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
            )
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
