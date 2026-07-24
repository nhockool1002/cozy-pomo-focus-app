package com.cozypomo.app.ui.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cozypomo.app.data.network.InventoryItemDto
import com.cozypomo.app.data.network.OwnedEggDto
import com.cozypomo.app.ui.common.EggIcon
import com.cozypomo.app.ui.common.JarMark
import com.cozypomo.app.ui.common.jarTintFor
import com.cozypomo.app.ui.common.parseEggColor

/** T-099 — S-07b Kho đồ (5th tab): xem + trang bị bình/nhạc, xem tiến trình trứng đang ấp —
 * bố trí dạng lưới thẻ theo từng tab danh mục, thay cho danh sách hàng rời rạc trước đây ở
 * Cài đặt/Khu rừng. */
@Composable
fun InventoryScreen(viewModel: InventoryViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text("Kho đồ", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Chọn bình ấp và nhạc nền bạn muốn dùng, xem trứng đang ấp",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            InventoryTab.entries.forEach { tab ->
                FilterChip(
                    selected = uiState.tab == tab,
                    onClick = { viewModel.selectTab(tab) },
                    label = { Text(tab.label) },
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
            when (uiState.tab) {
                InventoryTab.JAR -> ItemCardGrid(
                    items = uiState.jarSkins,
                    emptyMessage = "Chưa sở hữu bình nào — ghé Cửa hàng để mua nhé",
                ) { item ->
                    JarSkinCard(item = item, pending = uiState.pendingEquipId == item.id, onClick = { viewModel.equip(item.id) })
                }

                InventoryTab.MUSIC -> ItemCardGrid(
                    items = uiState.musicTracks,
                    emptyMessage = "Chưa sở hữu nhạc nào — ghé Cửa hàng để mua nhé",
                ) { item ->
                    MusicCard(item = item, pending = uiState.pendingEquipId == item.id, onClick = { viewModel.equip(item.id) })
                }

                InventoryTab.EGG -> EggCardGrid(eggs = uiState.ownedEggs)
            }
        }
    }
}

@Composable
private fun ItemCardGrid(items: List<InventoryItemDto>, emptyMessage: String, card: @Composable (InventoryItemDto) -> Unit) {
    if (items.isEmpty()) {
        EmptyState(emptyMessage)
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { it.id }) { item -> card(item) }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 32.dp),
            textAlign = TextAlign.Center,
        )
    }
}

/** Khung thẻ dùng chung cho Bình/Âm thanh — viền + nền nổi bật khi đang trang bị, badge dấu tick
 * góc trên-phải. Chạm vào món đã trang bị sẵn không làm gì (xem InventoryViewModel.equip).
 * [pending] = đang chờ PATCH /equip của chính món này trả về — mờ thẻ + chặn chạm thêm, tránh
 * chạm nhanh 2 lần lỡ tắt luôn trang bị vừa bật (bug thật đã gặp khi tự kiểm thử). */
@Composable
private fun EquippableCard(name: String, equipped: Boolean, pending: Boolean, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = !pending,
        shape = RoundedCornerShape(20.dp),
        color = if (equipped) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().aspectRatio(0.9f).alpha(if (pending) 0.5f else 1f),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                icon()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    name,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (pending) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(18.dp), strokeWidth = 2.dp)
            } else if (equipped) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Đang dùng",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun JarSkinCard(item: InventoryItemDto, pending: Boolean, onClick: () -> Unit) {
    EquippableCard(name = item.shopItem.name, equipped = item.equipped, pending = pending, onClick = onClick) {
        JarMark(size = 64.dp, eggColor = null, jarTint = jarTintFor(item.shopItem.name))
    }
}

@Composable
private fun MusicCard(item: InventoryItemDto, pending: Boolean, onClick: () -> Unit) {
    EquippableCard(name = item.shopItem.name, equipped = item.equipped, pending = pending, onClick = onClick) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f), modifier = Modifier.size(56.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
private fun EggCardGrid(eggs: List<OwnedEggDto>) {
    if (eggs.isEmpty()) {
        EmptyState("Chưa có trứng nào đang ấp — ghé Cửa hàng để mua nhé")
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(eggs, key = { it.id }) { egg -> EggProgressCard(egg) }
    }
}

@Composable
private fun EggProgressCard(egg: OwnedEggDto) {
    val hatchDuration = egg.eggType.hatchDurationMin.coerceAtLeast(1)
    val progress = (egg.incubatedMin.toFloat() / hatchDuration).coerceIn(0f, 1f)
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().aspectRatio(0.9f),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            EggIcon(color = parseEggColor(egg.eggType.colorHex), size = 44.dp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(egg.eggType.name, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${egg.incubatedMin}/$hatchDuration phút",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
