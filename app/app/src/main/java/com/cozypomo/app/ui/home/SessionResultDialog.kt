package com.cozypomo.app.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.cozypomo.app.data.timer.SessionCompletionResult
import com.cozypomo.app.ui.common.EggIcon
import com.cozypomo.app.ui.common.SpeciesArtIcon
import kotlin.math.cos
import kotlin.math.sin

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

/** T-117 — cấp bậc "long trọng" của hiệu ứng nở: 0 = bình thường (B/A/S — có thể từ bất kỳ trứng
 * nào), 1 = hiếm (SS — chỉ rơi từ Bí Ẩn/Truyền Thuyết, xem T-116), 2 = huyền thoại (SSR — Thần Thú,
 * chỉ rơi từ Bí Ẩn/Truyền Thuyết). Suy ra thẳng từ cấp loài nở ra — không cần biết loại trứng gốc,
 * vì sau T-116 chỉ 2 nhóm trứng "cao cấp" đó mới có thể cho ra SS/SSR. */
private fun hatchTier(rarity: String?): Int = when (rarity) {
    "SSR" -> 2
    "SS" -> 1
    else -> 0
}

@Composable
private fun HatchedContent(hatch: SessionCompletionResult.Hatched, appeared: Boolean) {
    val ringColor = rarityColor(hatch.speciesRarity)
    val tier = hatchTier(hatch.speciesRarity)
    val infiniteTransition = rememberInfiniteTransition(label = "hatchPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (tier == 2) 1.14f else if (tier == 1) 1.1f else 1.06f,
        animationSpec = infiniteRepeatable(tween(if (tier == 2) 650 else 900), repeatMode = RepeatMode.Reverse),
        label = "pulseScale",
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(if (tier == 2) 4000 else 6000, easing = LinearEasing)),
        label = "auraRotation",
    )
    val animatedCoins by animateIntAsState(
        targetValue = if (appeared) hatch.coinsEarned else 0,
        animationSpec = tween(durationMillis = 700),
        label = "coins",
    )
    val ringSize = when (tier) { 2 -> 132.dp; 1 -> 112.dp; else -> 96.dp }
    val iconSize = when (tier) { 2 -> 84.dp; 1 -> 76.dp; else -> 68.dp }

    Box(
        modifier = Modifier.size(ringSize + 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Tia hào quang toả xoay — chỉ SS/SSR mới có, đúng yêu cầu "hiệu ứng đặc biệt hơn" cho
        // trứng Bí Ẩn/Truyền Thuyết (2 nhóm trứng DUY NHẤT có thể cho kết quả cấp này).
        if (tier >= 1) {
            RadianceRays(
                sizeDp = ringSize + 28.dp,
                rayCount = if (tier == 2) 16 else 10,
                color = ringColor,
                rotationDeg = rotation,
            )
        }
        Box(
            modifier = Modifier
                .size(ringSize)
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
                    size = iconSize,
                )
            } else {
                // Dữ liệu loài không đủ (VD phiên cũ trước bản fix này) — vẫn hiện gì đó thay vì
                // để trống hoàn toàn.
                Box(modifier = Modifier.size(64.dp).background(ringColor, CircleShape))
            }
        }
        // Lấp lánh quay ngược chiều quanh viền — chỉ cấp huyền thoại (SSR/Thần Thú).
        if (tier == 2) {
            SparkleOrbit(sizeDp = ringSize + 20.dp, color = ringColor, rotationDeg = -rotation * 1.4f)
        }
    }

    Spacer(modifier = Modifier.height(20.dp))
    Text(
        text = when (tier) {
            2 -> "🌟 Huyền thoại! Thần Thú xuất hiện!"
            1 -> "✨ Cực hiếm! Trứng đã nở"
            else -> "Chúc mừng! Trứng đã nở"
        },
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
    )
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

/** Tia hào quang xoay quanh vòng loài — độ "long trọng" của hiệu ứng nở (T-117), chỉ dùng cho kết
 * quả SS/SSR. Vẽ trong `Box` kích thước cố định [sizeDp], không bao giờ tràn ra ngoài bounds của
 * chính nó nên không lặp lại lỗi Dialog cao vượt màn hình đã sửa ở `EggPickerDialog`. */
@Composable
private fun RadianceRays(sizeDp: androidx.compose.ui.unit.Dp, rayCount: Int, color: Color, rotationDeg: Float) {
    Canvas(modifier = Modifier.size(sizeDp).graphicsLayer { rotationZ = rotationDeg }) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val innerR = size.minDimension * 0.36f
        val outerR = size.minDimension * 0.5f
        for (i in 0 until rayCount) {
            val angle = (360f / rayCount) * i
            val rad = Math.toRadians(angle.toDouble())
            val dx = cos(rad).toFloat()
            val dy = sin(rad).toFloat()
            drawLine(
                color = color.copy(alpha = 0.35f),
                start = Offset(cx + dx * innerR, cy + dy * innerR),
                end = Offset(cx + dx * outerR, cy + dy * outerR),
                strokeWidth = 3f,
                cap = StrokeCap.Round,
            )
        }
    }
}

/** Chấm lấp lánh xoay quanh viền — chỉ hiện cho kết quả huyền thoại (SSR/Thần Thú, T-117). */
@Composable
private fun SparkleOrbit(sizeDp: androidx.compose.ui.unit.Dp, color: Color, rotationDeg: Float) {
    Canvas(modifier = Modifier.size(sizeDp).graphicsLayer { rotationZ = rotationDeg }) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.minDimension * 0.5f
        val sparkleCount = 6
        for (i in 0 until sparkleCount) {
            val angle = (360f / sparkleCount) * i
            val rad = Math.toRadians(angle.toDouble())
            val sx = cx + cos(rad).toFloat() * r
            val sy = cy + sin(rad).toFloat() * r
            val s = 3.5f
            drawLine(color, Offset(sx - s, sy), Offset(sx + s, sy), strokeWidth = 1.6f, cap = StrokeCap.Round)
            drawLine(color, Offset(sx, sy - s), Offset(sx, sy + s), strokeWidth = 1.6f, cap = StrokeCap.Round)
        }
    }
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
