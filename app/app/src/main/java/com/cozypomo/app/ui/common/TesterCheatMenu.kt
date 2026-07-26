package com.cozypomo.app.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

private val RARITY_ORDER = listOf("B", "A", "S", "SS", "SSR")

private data class EggCheatOption(val label: String, val eggTypeName: String, val tint: Color)

/** T-117 — trước chỉ cấp được "Trứng Bí Ẩn"; giờ đủ cả 7 loại (kể cả 3 Truyền Thuyết chỉ Admin
 * phát ở luồng thật) để tester tự kiểm thử luồng ấp/hiệu ứng nở từng loại. Tên PHẢI khớp tuyệt đối
 * `EggType.name` trong `prisma/seed.ts` — backend tra theo tên (`debug/grant-egg`). */
private val EGG_CHEAT_OPTIONS = listOf(
    EggCheatOption("Rừng", "Trứng Rừng", Color(0xFF7C9A5A)),
    EggCheatOption("Biển", "Trứng Biển", Color(0xFF6FB6A8)),
    EggCheatOption("Hoa", "Trứng Hoa", Color(0xFFE7A8B0)),
    EggCheatOption("Bí Ẩn", "Trứng Bí Ẩn", Color(0xFFE3B04B)),
    EggCheatOption("TT.Rừng", "Trứng Truyền Thuyết Rừng", Color(0xFF3F5C2E)),
    EggCheatOption("TT.Biển", "Trứng Truyền Thuyết Biển", Color(0xFF2E5A66)),
    EggCheatOption("TT.Hoa", "Trứng Truyền Thuyết Hoa", Color(0xFFB23F6A)),
)

/** Menu cheat cho tester (chỉ mục đích debug/QA) — mở khi chạm bubble nổi (xem CheatBubble).
 * Bố trí dạng lưới thẻ nhỏ (thay LazyColumn nút full-width cũ) — gọn và "tinh tế" hơn, cùng
 * ngôn ngữ hình ảnh với thẻ loài/trứng ở Khu rừng/Cửa hàng. */
@Composable
fun TesterCheatMenu(
    onDismiss: () -> Unit,
    onGrantCoin: () -> Unit,
    onGrantFocusMinute: () -> Unit,
    onFastForwardSession: () -> Unit,
    onGrantEgg: (String) -> Unit,
    onGrantRarity: (String) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
            Column(modifier = Modifier.padding(20.dp).widthIn(max = 340.dp)) {
                Text("Bubble Cheat (Tester)", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Chỉ để debug/QA — không phải tính năng thật",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))

                CheatSectionLabel("Tiền tệ & phiên")
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    CheatActionCard(
                        label = "+1000\nXu Lá",
                        tint = MaterialTheme.colorScheme.secondary,
                        onClick = onGrantCoin,
                        modifier = Modifier.weight(1f),
                    ) { Icon(Icons.Filled.Eco, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) }
                    CheatActionCard(
                        label = "+1000\nGiờ",
                        tint = MaterialTheme.colorScheme.primary,
                        onClick = onGrantFocusMinute,
                        modifier = Modifier.weight(1f),
                    ) { Icon(Icons.Filled.HourglassBottom, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    CheatActionCard(
                        label = "Kéo phiên\n~5s",
                        tint = MaterialTheme.colorScheme.tertiary,
                        onClick = onFastForwardSession,
                        modifier = Modifier.weight(1f),
                    ) { Icon(Icons.Filled.FastForward, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary) }
                }

                Spacer(modifier = Modifier.height(18.dp))
                CheatSectionLabel("Trứng (7 loại — cả Truyền Thuyết)")
                Spacer(modifier = Modifier.height(8.dp))

                // T-117 — LazyVerticalGrid LUÔN cuộn bên trong khi vượt chiều cao cho — khác
                // `Column`+`forEach` không giới hạn đã gây lỗi Dialog cao vượt màn hình ở
                // EggPickerDialog. Đặt trần cố định ở ĐÂY thay vì tính theo số item, để về sau có
                // thêm loại trứng nào cũng không lặp lại lỗi cũ.
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.height(220.dp),
                ) {
                    items(EGG_CHEAT_OPTIONS) { opt ->
                        CheatActionCard(label = opt.label, tint = opt.tint, onClick = { onGrantEgg(opt.eggTypeName) }) {
                            EggIcon(color = opt.tint, size = 26.dp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                CheatSectionLabel("Loài ngẫu nhiên theo cấp")
                Spacer(modifier = Modifier.height(8.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.height(114.dp),
                ) {
                    items(RARITY_ORDER) { rarity ->
                        val colors = RARITY_BADGE.getValue(rarity)
                        CheatActionCard(label = "Ngẫu nhiên", tint = colors.fg, onClick = { onGrantRarity(rarity) }) {
                            Surface(shape = CircleShape, color = colors.bg, modifier = Modifier.size(30.dp)) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text(
                                        text = rarity,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.fg,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Đóng") }
            }
        }
    }
}

@Composable
private fun CheatSectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Thẻ hành động nhỏ vuông (icon/badge + nhãn) — đơn vị bố cục dùng chung cho mọi mục trong menu cheat. */
@Composable
private fun CheatActionCard(
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = tint.copy(alpha = 0.12f),
        modifier = modifier.aspectRatio(1f),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            icon()
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = tint,
                textAlign = TextAlign.Center,
                maxLines = 2,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
