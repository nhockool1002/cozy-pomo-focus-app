package com.cozypomo.app.ui.forest

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.cozypomo.app.data.network.CollectionEntryDto
import com.cozypomo.app.ui.common.RarityBadge
import com.cozypomo.app.ui.common.SpeciesArtIcon

/** T-036 — S-03 Chi tiết loài/Lore. Popup khi chạm thẻ đã mở khoá ở Khu rừng (S-04).
 * [onSell] chỉ được gọi khi nút "Đăng bán" hiện (T-110, `entry.ownedCount ≥ 1` — không cần "dư",
 * xem plan.md Nhóm F). */
@Composable
fun SpeciesDetailDialog(
    entry: CollectionEntryDto,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSell: () -> Unit,
) {
    val species = entry.species
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    RarityBadge(species.rarity)
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Đóng") }
                }

                SpeciesArtIcon(
                    category = species.category,
                    archetype = species.archetype,
                    paletteIdx = species.paletteIdx,
                    seed = species.name,
                    rarity = species.rarity,
                    size = 120.dp,
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(species.name, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Đã nở ${entry.hatchCount} lần",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (entry.ownedCount > 0) "Bạn đang sở hữu ${entry.ownedCount} bản" else "Đã bán hết — không sở hữu bản nào",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = species.lore?.takeIf { it.isNotBlank() }
                        ?: "Chưa có câu chuyện nào được ghi lại cho loài này — có lẽ nó thích giữ bí mật.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (entry.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Yêu thích",
                            tint = if (entry.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (entry.ownedCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = onSell, modifier = Modifier.weight(1f)) { Text("Đăng bán") }
                    }
                }
            }
        }
    }
}
