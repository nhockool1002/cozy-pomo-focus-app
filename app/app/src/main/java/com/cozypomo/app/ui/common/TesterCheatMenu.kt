package com.cozypomo.app.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/** Menu cheat cho tester (chỉ mục đích debug/QA) — mở khi chạm bubble nổi (xem CheatBubble). */
@Composable
fun TesterCheatMenu(
    onDismiss: () -> Unit,
    onGrantCoin: () -> Unit,
    onGrantFocusMinute: () -> Unit,
    onFastForwardSession: () -> Unit,
    onGrantMysteryEgg: () -> Unit,
    onGrantRarity: (String) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Bubble Cheat (Tester)", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Chỉ để debug/QA — không phải tính năng thật",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(modifier = Modifier.height(360.dp)) {
                    val actions = listOf(
                        "+1000 Xu Lá" to onGrantCoin,
                        "+1000 Giờ tích luỹ" to onGrantFocusMinute,
                        "Kéo phiên đang chạy về ~5s" to onFastForwardSession,
                        "Nhận Trứng Bí Ẩn" to onGrantMysteryEgg,
                        "Nhận ngẫu nhiên hạng B" to { onGrantRarity("B") },
                        "Nhận ngẫu nhiên hạng A" to { onGrantRarity("A") },
                        "Nhận ngẫu nhiên hạng S" to { onGrantRarity("S") },
                        "Nhận ngẫu nhiên hạng SS" to { onGrantRarity("SS") },
                        "Nhận ngẫu nhiên hạng SSR" to { onGrantRarity("SSR") },
                    )
                    items(actions) { (label, action) ->
                        Button(
                            onClick = action,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer),
                        ) { Text(label) }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Đóng") }
            }
        }
    }
}
