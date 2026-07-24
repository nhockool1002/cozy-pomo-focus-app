package com.cozypomo.app.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Giai đoạn hiển thị của trứng trong bình ấp, suy ra từ incubatedMin/hatchDurationMin. */
enum class JarEggStage { NONE, NORMAL, CRACKING, ABOUT_TO_HATCH, HATCHED }

fun jarEggStageFor(progress: Float): JarEggStage = when {
    progress >= 1f -> JarEggStage.HATCHED
    progress >= 0.85f -> JarEggStage.ABOUT_TO_HATCH
    progress >= 0.5f -> JarEggStage.CRACKING
    else -> JarEggStage.NORMAL
}

/** Suy màu thân bình ổn định theo tên vật phẩm JAR_SKIN (không cần backend thêm cột màu riêng —
 * ShopItem hiện không có trường màu nào) — cùng 1 tên luôn ra cùng 1 màu, lấy từ [SPECIES_PALETTE]
 * sẵn có để khớp bảng màu chung của game thay vì bịa 1 bảng màu mới. */
fun jarTintFor(shopItemName: String): Color =
    SPECIES_PALETTE[(shopItemName.hashCode() and 0x7FFFFFFF) % SPECIES_PALETTE.size].base

/**
 * Biểu tượng "bình ấp trứng" dùng chung cho Splash/Onboarding/Đăng nhập/Trang chủ.
 * [eggColor] null = bình rỗng (không chọn trứng). [progress] 0..1 = incubatedMin/hatchDurationMin,
 * quyết định giai đoạn hình ảnh: bình thường → nứt → sắp nở → nở. [desaturated] dùng khi bỏ cuộc —
 * trứng vẫn nguyên vẹn (không vỡ/không mất) nhưng hiển thị xám để báo hiệu phiên này không được tính.
 */
@Composable
fun JarMark(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    eggColor: Color? = MaterialTheme.colorScheme.secondary,
    progress: Float = 0f,
    desaturated: Boolean = false,
    animate: Boolean = false,
    /** Màu thân bình theo skin đang trang bị (Kho đồ, T-099) — null = màu mặc định [primary]. */
    jarTint: Color? = null,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val primary = jarTint ?: MaterialTheme.colorScheme.primary
    val stage = if (eggColor == null) JarEggStage.NONE else if (desaturated) JarEggStage.NORMAL else jarEggStageFor(progress)
    val resolvedEggColor = if (desaturated) ink.copy(alpha = 0.35f) else eggColor

    val infinite = rememberInfiniteTransition(label = "jarEgg")
    val wobblePhase by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
        label = "wobble",
    )
    val glowPulse by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "glow",
    )

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeW = w * 0.035f

        // Miệng bình
        drawRoundRect(
            color = ink,
            topLeft = Offset(w * 0.30f, h * 0.18f),
            size = Size(w * 0.40f, h * 0.14f),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f),
            style = Stroke(width = strokeW),
        )

        // Thân bình
        drawRoundRect(
            color = primary.copy(alpha = 0.28f),
            topLeft = Offset(w * 0.14f, h * 0.30f),
            size = Size(w * 0.72f, h * 0.62f),
            cornerRadius = CornerRadius(w * 0.18f, w * 0.18f),
        )
        drawRoundRect(
            color = ink,
            topLeft = Offset(w * 0.14f, h * 0.30f),
            size = Size(w * 0.72f, h * 0.62f),
            cornerRadius = CornerRadius(w * 0.18f, w * 0.18f),
            style = Stroke(width = strokeW),
        )

        // Ụ đất
        drawArc(
            color = ink.copy(alpha = 0.30f),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(w * 0.22f, h * 0.66f),
            size = Size(w * 0.56f, h * 0.32f),
        )

        if (stage == JarEggStage.NONE || resolvedEggColor == null) return@Canvas

        val cx = w * 0.5f
        var cy = h * 0.67f
        val rx = w * 0.10f
        val ry = h * 0.13f
        // Trứng luôn lắc lư nhẹ liên tục khi đang ấp (không chỉ lúc sắp nở) — biên độ tăng dần
        // theo tiến trình để càng gần nở càng "háo hức" rung mạnh hơn.
        val wobbleSigned = wobblePhase * 2f - 1f // -1..1, liên tục qua lại
        val wobbleAmplitude = if (animate) (2f + progress * 10f) else 0f
        val wobbleDeg = wobbleSigned * wobbleAmplitude
        if (animate) cy += wobbleSigned * ry * 0.06f

        rotate(wobbleDeg, pivot = Offset(cx, cy)) {
            when (stage) {
                JarEggStage.HATCHED -> {
                    val glowAlpha = 0.25f + (if (animate) glowPulse else 0.5f) * 0.35f
                    drawCircle(color = primary.copy(alpha = glowAlpha), radius = rx * 2.1f, center = Offset(cx, cy))
                    // 2 mảnh vỏ tách đôi
                    drawOval(resolvedEggColor, topLeft = Offset(cx - rx * 1.1f, cy - ry * 1.3f), size = Size(rx * 2f, ry * 1.1f))
                    drawOval(resolvedEggColor, topLeft = Offset(cx - rx * 0.9f, cy + ry * 0.3f), size = Size(rx * 1.9f, ry * 1.1f))
                }
                else -> {
                    drawOval(color = resolvedEggColor, topLeft = Offset(cx - rx, cy - ry), size = Size(rx * 2, ry * 2))
                    drawOval(color = ink, topLeft = Offset(cx - rx, cy - ry), size = Size(rx * 2, ry * 2), style = Stroke(width = w * 0.02f))
                    // sheen
                    drawOval(
                        color = Color.White.copy(alpha = 0.35f),
                        topLeft = Offset(cx - rx * 0.6f, cy - ry * 0.75f),
                        size = Size(rx * 0.7f, ry * 0.6f),
                    )
                    val crackCount = when (stage) {
                        JarEggStage.CRACKING -> 2
                        JarEggStage.ABOUT_TO_HATCH -> 3
                        else -> 0
                    }
                    if (crackCount > 0) {
                        val cracks = listOf(
                            Path().apply {
                                moveTo(cx - rx * 0.3f, cy - ry * 0.9f)
                                lineTo(cx - rx * 0.05f, cy - ry * 0.3f)
                                lineTo(cx - rx * 0.35f, cy + ry * 0.1f)
                                lineTo(cx - rx * 0.05f, cy + ry * 0.5f)
                            },
                            Path().apply {
                                moveTo(cx + rx * 0.5f, cy - ry * 0.6f)
                                lineTo(cx + rx * 0.15f, cy - ry * 0.05f)
                                lineTo(cx + rx * 0.5f, cy + ry * 0.4f)
                            },
                            Path().apply {
                                moveTo(cx - rx * 0.6f, cy - ry * 0.1f)
                                lineTo(cx + rx * 0.6f, cy + ry * 0.15f)
                            },
                        )
                        for (i in 0 until crackCount) {
                            drawPath(cracks[i], ink, style = Stroke(width = rx * 0.09f, cap = StrokeCap.Round))
                        }
                    }
                }
            }
        }
    }
}
