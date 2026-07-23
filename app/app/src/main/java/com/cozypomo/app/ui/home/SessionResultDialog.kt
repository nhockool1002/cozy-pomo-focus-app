package com.cozypomo.app.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.cozypomo.app.data.timer.SessionCompletionResult
import com.cozypomo.app.ui.common.EggIcon
import com.cozypomo.app.ui.common.SpeciesArtIcon

/** Hiện sau khi 1 phiên kết thúc — chúc mừng khi trứng nở/ấp tiếp, chia buồn khi bỏ cuộc. */
@Composable
fun SessionResultDialog(result: SessionResultUi, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        var appeared by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { appeared = true }
        val scale by animateFloatAsState(
            targetValue = if (appeared) 1f else 0.6f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "resultScale",
        )
        val alpha by animateFloatAsState(targetValue = if (appeared) 1f else 0f, label = "resultAlpha")

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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val dismissLabel = when (result) {
                    is SessionResultUi.Completed -> when (result.result) {
                        is SessionCompletionResult.Hatched -> "Tuyệt vời!"
                        is SessionCompletionResult.Incubating -> "Tiếp tục nào"
                        is SessionCompletionResult.NoEgg -> "Đã hiểu"
                    }
                    is SessionResultUi.GaveUp -> "Đóng"
                }

                when (result) {
                    is SessionResultUi.Completed -> when (val r = result.result) {
                        is SessionCompletionResult.Hatched -> HatchedContent(r, appeared)
                        is SessionCompletionResult.Incubating -> IncubatingContent(r, appeared)
                        is SessionCompletionResult.NoEgg -> NoEggContent(r, appeared)
                    }
                    is SessionResultUi.GaveUp -> GaveUpContent(result.eggTypeName)
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text(dismissLabel)
                }
            }
        }
    }
}

@Composable
private fun HatchedContent(hatch: SessionCompletionResult.Hatched, appeared: Boolean) {
    val ringColor = rarityColor(hatch.speciesRarity)
    val infiniteTransition = rememberInfiniteTransition(label = "hatchPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(900), repeatMode = RepeatMode.Reverse),
        label = "pulseScale",
    )
    val animatedCoins by animateIntAsState(
        targetValue = if (appeared) hatch.coinsEarned else 0,
        animationSpec = tween(durationMillis = 700),
        label = "coins",
    )

    Box(
        modifier = Modifier
            .size(96.dp)
            .graphicsLayer { scaleX = pulse; scaleY = pulse }
            .background(ringColor.copy(alpha = 0.18f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (hatch.speciesCategory != null && hatch.speciesArchetype != null) {
            SpeciesArtIcon(
                category = hatch.speciesCategory,
                archetype = hatch.speciesArchetype,
                paletteIdx = hatch.speciesPaletteIdx ?: 0,
                seed = hatch.speciesName ?: "?",
                rarity = hatch.speciesRarity,
                size = 68.dp,
            )
        } else {
            // Dữ liệu loài không đủ (VD phiên cũ trước bản fix này) — vẫn hiện gì đó thay vì
            // để trống hoàn toàn.
            Box(modifier = Modifier.size(64.dp).background(ringColor, CircleShape))
        }
    }

    Spacer(modifier = Modifier.height(20.dp))
    Text("Chúc mừng! Trứng đã nở", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = hatch.speciesName ?: "Một loài bí ẩn",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
        textAlign = TextAlign.Center,
    )
    hatch.speciesRarity?.let { rarity ->
        Spacer(modifier = Modifier.height(6.dp))
        Surface(shape = RoundedCornerShape(50), color = ringColor.copy(alpha = 0.2f)) {
            Text(
                text = "Cấp $rarity",
                style = MaterialTheme.typography.labelLarge,
                color = ringColor,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    }
    Spacer(modifier = Modifier.height(14.dp))
    ResultStatsRow(coinsEarned = animatedCoins, minutesAccumulated = hatch.minutesAccumulated)
}

@Composable
private fun IncubatingContent(state: SessionCompletionResult.Incubating, appeared: Boolean) {
    val hatchDuration = state.hatchDurationMin.coerceAtLeast(1)
    val targetProgress = (state.incubatedMin.toFloat() / hatchDuration).coerceIn(0f, 1f)
    val progress by animateFloatAsState(
        targetValue = if (appeared) targetProgress else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "incubateProgress",
    )
    val animatedCoins by animateIntAsState(
        targetValue = if (appeared) state.coinsEarned else 0,
        animationSpec = tween(durationMillis = 700),
        label = "coins",
    )

    EggIcon(color = MaterialTheme.colorScheme.primary, size = 88.dp)
    Spacer(modifier = Modifier.height(20.dp))
    Text("Trứng đang ấp tiếp…", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = state.eggTypeName ?: "Trứng đang sở hữu",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(12.dp))
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.fillMaxWidth().height(8.dp),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = "${state.incubatedMin}/$hatchDuration phút — sắp nở rồi!",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(14.dp))
    ResultStatsRow(coinsEarned = animatedCoins, minutesAccumulated = state.minutesAccumulated)
}

@Composable
private fun NoEggContent(state: SessionCompletionResult.NoEgg, appeared: Boolean) {
    val animatedCoins by animateIntAsState(
        targetValue = if (appeared) state.coinsEarned else 0,
        animationSpec = tween(durationMillis = 700),
        label = "coins",
    )
    EggIcon(color = MaterialTheme.colorScheme.secondary, size = 88.dp)
    Spacer(modifier = Modifier.height(20.dp))
    Text("Hoàn thành phiên tập trung!", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Bạn không ấp trứng nào lần này, toàn bộ thời gian đã vào Giờ tích luỹ.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(14.dp))
    ResultStatsRow(coinsEarned = animatedCoins, minutesAccumulated = state.minutesAccumulated)
}

/** Phần thưởng giờ chỉ thuộc CHỈ 1 loại tiền (người dùng chọn trước khi bắt đầu) — chỉ hiện dòng khác 0. */
@Composable
private fun ResultStatsRow(coinsEarned: Int, minutesAccumulated: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (coinsEarned > 0) {
            Text(
                text = "+$coinsEarned Xu Lá",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        if (minutesAccumulated > 0) {
            Text(
                text = "+$minutesAccumulated phút Giờ tích luỹ",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * Bỏ cuộc KHÔNG làm vỡ/mất trứng — chỉ không nhận Xu Lá/Giờ tích luỹ phiên này. Trứng (nếu có
 * chọn) vẫn hiển thị nguyên hình dạng, chỉ đổi màu xám để báo hiệu phiên vừa rồi không được tính.
 */
@Composable
private fun GaveUpContent(eggTypeName: String?) {
    val shakeOffset = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        shakeOffset.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
                durationMillis = 400
                0f at 0
                -10f at 80
                10f at 160
                -5f at 240
                5f at 320
                0f at 400
            },
        )
    }

    Box(
        modifier = Modifier
            .size(96.dp)
            .offset(x = shakeOffset.value.dp),
        contentAlignment = Alignment.Center,
    ) {
        EggIcon(
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
            size = 72.dp,
        )
    }

    Spacer(modifier = Modifier.height(20.dp))
    Text("Đã dừng phiên tập trung", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = buildString {
            append("Lần này bạn sẽ không nhận Xu Lá hay Giờ tích luỹ. ")
            if (eggTypeName != null) {
                append("$eggTypeName vẫn an toàn, giữ nguyên tiến trình ấp cũ — không hề bị mất hay vỡ.")
            } else {
                append("Thử lại phiên tập trung tiếp theo nhé!")
            }
        },
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun rarityColor(rarity: String?): Color = when (rarity) {
    "SSR" -> MaterialTheme.colorScheme.secondary
    "SS" -> MaterialTheme.colorScheme.tertiary
    "S" -> MaterialTheme.colorScheme.onPrimaryContainer
    "A" -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
