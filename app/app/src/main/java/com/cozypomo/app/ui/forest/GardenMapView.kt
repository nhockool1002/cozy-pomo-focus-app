package com.cozypomo.app.ui.forest

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cozypomo.app.data.network.CollectionEntryDto
import com.cozypomo.app.ui.common.GardenZoneBackground
import com.cozypomo.app.ui.common.SpeciesArtIcon
import com.cozypomo.app.ui.common.computeGardenSlots
import com.cozypomo.app.ui.common.gardenGridSize

private data class GardenZoneSpec(val category: String, val label: String, val emoji: String)

private val GARDEN_ZONES = listOf(
    GardenZoneSpec("FOREST", "Khu Rừng", "🌲"),
    GardenZoneSpec("SEA", "Vùng Biển", "🌊"),
    GardenZoneSpec("PLANT", "Vườn Cây", "🌿"),
    GardenZoneSpec("MYTHIC", "Điện Thần Thú", "✨"),
)

private val MinZoneHeight = 230.dp

/** Chiều cao "1 hàng" trong lưới của khu — nhân với số hàng ([gardenGridSize]) để khung khu vực
 * luôn đủ chỗ cho MỌI vật phẩm sở hữu, không đè/cắt loài nào khi 1 khu có nhiều loài (yêu cầu
 * "hiển thị hết, bản đồ có thể mở rộng to" — plan.md Nhóm G). Cuộn dọc toàn trang (đã có sẵn) chính
 * là thao tác "kéo" để xem hết khu vực đã cao hơn. */
private val RowHeightUnit = 96.dp

/**
 * T-112/T-113 — chế độ xem "Khu vườn" của Khu rừng (S-04, Nhóm G): CHỈ hiện loài đang thực sự sở
 * hữu (`ownedCount > 0` — khác chế độ Lưới vẫn hiện ô khoá "?" để thôi thúc sưu tầm), đặt trên nền
 * địa hình theo từng khu vực (T-112 `GardenZoneBackground`), vị trí "ngẫu nhiên nhưng tinh tế" ổn
 * định theo speciesId (`computeGardenSlots`). Cuộn dọc qua cả 4 khu (kéo để xem hết khu cao hơn),
 * không pan ngang/zoom tự do. Bấm 1 sinh vật mở đúng `SpeciesDetailDialog` (S-03) đã có.
 */
@Composable
fun GardenMapView(entries: List<CollectionEntryDto>, onOpenSpecies: (String) -> Unit) {
    val ownedEntries = entries.filter { it.ownedCount > 0 }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        GARDEN_ZONES.forEach { zone ->
            val zoneEntries = ownedEntries.filter { it.species.category == zone.category }
            val slots = computeGardenSlots(zoneEntries.map { it.speciesId })
            val (_, rows) = gardenGridSize(zoneEntries.size)
            val zoneHeight = maxOf(MinZoneHeight, RowHeightUnit * rows)

            BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(zoneHeight)) {
                GardenZoneBackground(category = zone.category, density = rows, modifier = Modifier.fillMaxSize())

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), RoundedCornerShape(10.dp)),
                ) {
                    Text(
                        "${zone.emoji} ${zone.label}",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }

                zoneEntries.forEach { entry ->
                    val slot = slots[entry.speciesId] ?: return@forEach
                    val iconSize = 44.dp * slot.scale
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = maxWidth * slot.x - iconSize / 2, y = maxHeight * slot.y - iconSize / 2)
                            .size(iconSize)
                            .clickable { onOpenSpecies(entry.speciesId) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(iconSize)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f), CircleShape),
                        )
                        SpeciesArtIcon(
                            category = entry.species.category,
                            archetype = entry.species.archetype,
                            paletteIdx = entry.species.paletteIdx,
                            seed = entry.species.name,
                            rarity = entry.species.rarity,
                            size = iconSize * 0.82f,
                        )
                    }
                }
            }
        }
    }
}
