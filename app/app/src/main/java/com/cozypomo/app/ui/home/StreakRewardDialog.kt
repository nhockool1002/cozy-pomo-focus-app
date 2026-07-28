package com.cozypomo.app.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.cozypomo.app.data.network.StreakRewardDto
import com.cozypomo.app.data.network.StreakRewardItemDto

/**
 * Modal ăn mừng khi đạt mốc streak ngày liên tục (1-7) — cùng khuôn mẫu Dialog + spring
 * scale/alpha và hiệu ứng hào quang/lấp lánh với [SessionResultDialog] để nhất quán phong cách,
 * chỉ hiện SAU KHI người dùng đóng modal kết quả phiên (xem HomeViewModel, tránh 2 modal chồng
 * nhau). Server đã tạo InboxMessage type STREAK_REWARD cùng lúc — không cần gọi API riêng ở đây.
 */
@Composable
fun StreakRewardDialog(reward: StreakRewardDto, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        var appeared by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { appeared = true }
        val scale by animateFloatAsState(
            targetValue = if (appeared) 1f else 0.6f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "streakScale",
        )
        val alpha by animateFloatAsState(targetValue = if (appeared) 1f else 0f, label = "streakAlpha")

        val infiniteTransition = rememberInfiniteTransition(label = "streakGlow")
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
            label = "streakAuraRotation",
        )
        val ringColor = MaterialTheme.colorScheme.secondary

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha },
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(modifier = Modifier.size(124.dp), contentAlignment = Alignment.Center) {
                    RadianceRays(sizeDp = 124.dp, rayCount = 14, color = ringColor, rotationDeg = rotation)
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .background(ringColor.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("🔥", style = MaterialTheme.typography.displaySmall)
                    }
                    SparkleOrbit(sizeDp = 108.dp, color = ringColor, rotationDeg = -rotation * 1.4f)
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    "Streak ${reward.day} ngày liên tiếp!",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Duy trì tập trung mỗi ngày để nhận thêm quà nhé",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(reward.items, key = { it.kind + it.name }) { item -> RewardItemRow(item) }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text("Tuyệt vời!")
                }
            }
        }
    }
}

private fun iconFor(kind: String): ImageVector = when (kind) {
    "SPECIES" -> Icons.Filled.AutoAwesome
    "EGG_TYPE" -> Icons.Filled.Egg
    "COIN" -> Icons.Filled.Eco
    else -> Icons.Filled.CardGiftcard
}

@Composable
private fun RewardItemRow(item: StreakRewardItemDto) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f), modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(iconFor(item.kind), contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (item.kind == "COIN") "${item.quantity} ${item.name}" else item.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            if (item.kind != "COIN") {
                Text("x${item.quantity}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
