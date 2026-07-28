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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
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

/** Chất liệu bình — suy trực tiếp từ tên vật phẩm (Gốm/Thuỷ Tinh/Pha Lê, xem seed.ts) quyết định
 * mức độ "sang" của hiệu ứng vẽ: GỐM mờ/mộc mạc, THUỶ TINH bóng vừa, PHA LÊ lấp lánh nhất — tên nào
 * không khớp từ khoá nào (VD bình mặc định không skin) rơi về GLASS, mức trung tính. */
enum class JarMaterial { CERAMIC, GLASS, CRYSTAL }

fun jarMaterialFor(shopItemName: String?): JarMaterial = when {
    shopItemName == null -> JarMaterial.GLASS
    shopItemName.contains("Pha Lê", ignoreCase = true) -> JarMaterial.CRYSTAL
    shopItemName.contains("Gốm", ignoreCase = true) -> JarMaterial.CERAMIC
    else -> JarMaterial.GLASS
}

/** Hình bát giác vát góc — dáng "cắt pha lê" riêng cho [JarMaterial.CRYSTAL], khác hẳn hình chữ
 * nhật bo tròn của GỐM/THUỶ TINH (T-125, Dev1002 yêu cầu đổi cả kiểu dáng chứ không chỉ hiệu ứng). */
private fun octagonPath(topLeft: Offset, size: Size, cut: Float = 0.24f): Path = Path().apply {
    val x0 = topLeft.x
    val y0 = topLeft.y
    val w = size.width
    val h = size.height
    moveTo(x0 + w * cut, y0)
    lineTo(x0 + w * (1 - cut), y0)
    lineTo(x0 + w, y0 + h * cut)
    lineTo(x0 + w, y0 + h * (1 - cut))
    lineTo(x0 + w * (1 - cut), y0 + h)
    lineTo(x0 + w * cut, y0 + h)
    lineTo(x0, y0 + h * (1 - cut))
    lineTo(x0, y0 + h * cut)
    close()
}

/**
 * Biểu tượng "bình ấp trứng" dùng chung cho Splash/Onboarding/Đăng nhập/Trang chủ/Cửa hàng/Kho đồ
 * — MỌI nơi bình xuất hiện đều qua đúng composable này nên sửa 1 chỗ áp dụng khắp app. Thân bình
 * vẽ bằng gradient chiều dọc (thay vì màu phẳng như trước) + hiệu ứng "tráng gương" (dải sáng chéo
 * cắt theo đúng hình thân bình) cho GLASS/CRYSTAL — GỐM giữ mờ mộc mạc, không có dải sáng, chỉ có
 * 1 chấm sáng dịu mô phỏng ánh sáng hắt lên bề mặt mờ.
 *
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
    /** Chất liệu skin (xem [jarMaterialFor]) — quyết định độ "sang" của hiệu ứng vẽ thân bình. */
    material: JarMaterial = JarMaterial.GLASS,
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
    // Dải sáng "tráng gương" trượt chậm qua thân bình — chỉ khi animate=true (VD Trang chủ đang
    // ấp), danh sách Cửa hàng/Kho đồ tĩnh dùng vị trí cố định để tránh nhiều item cùng nhấp nháy.
    val shimmerPhase by infinite.animateFloat(
        initialValue = -0.3f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(if (material == JarMaterial.CRYSTAL) 2600 else 3400, easing = LinearEasing)),
        label = "shimmer",
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

        // Kiểu dáng thân bình khác nhau theo chất liệu, không chỉ đổi màu/hiệu ứng (T-125): GỐM
        // tròn trịa/mập mạp như hũ sành, THUỶ TINH giữ dáng chữ nhật bo tròn "chuẩn" cũ, PHA LÊ
        // vát bát giác sắc cạnh như đá quý cắt mài.
        val bodyTopLeft: Offset
        val bodySize: Size
        val bodyCorner: CornerRadius
        when (material) {
            JarMaterial.CERAMIC -> {
                bodyTopLeft = Offset(w * 0.10f, h * 0.33f)
                bodySize = Size(w * 0.80f, h * 0.58f)
                bodyCorner = CornerRadius(w * 0.32f, w * 0.32f)
            }
            JarMaterial.GLASS -> {
                bodyTopLeft = Offset(w * 0.14f, h * 0.30f)
                bodySize = Size(w * 0.72f, h * 0.62f)
                bodyCorner = CornerRadius(w * 0.18f, w * 0.18f)
            }
            JarMaterial.CRYSTAL -> {
                bodyTopLeft = Offset(w * 0.15f, h * 0.28f)
                bodySize = Size(w * 0.70f, h * 0.64f)
                bodyCorner = CornerRadius(w * 0.06f, w * 0.06f)
            }
        }
        val bodyPath = if (material == JarMaterial.CRYSTAL) {
            octagonPath(bodyTopLeft, bodySize)
        } else {
            Path().apply { addRoundRect(RoundRect(Rect(bodyTopLeft, bodySize), bodyCorner, bodyCorner, bodyCorner, bodyCorner)) }
        }

        // Thân bình — gradient dọc thay vì màu phẳng (đỡ "phẳng lì"), đậm dần xuống đáy như chất
        // lỏng/thuỷ tinh thật có chiều sâu. GỐM ấm/mờ hơn, PHA LÊ sáng/trong hơn.
        val (topAlpha, bottomAlpha) = when (material) {
            JarMaterial.CERAMIC -> 0.34f to 0.22f
            JarMaterial.GLASS -> 0.22f to 0.34f
            JarMaterial.CRYSTAL -> 0.16f to 0.40f
        }
        drawPath(
            bodyPath,
            brush = Brush.verticalGradient(
                listOf(primary.copy(alpha = topAlpha), primary.copy(alpha = bottomAlpha)),
                startY = bodyTopLeft.y,
                endY = bodyTopLeft.y + bodySize.height,
            ),
        )

        when (material) {
            JarMaterial.CERAMIC -> {
                // Mờ mộc mạc — 1 chấm sáng dịu thay vì dải sáng gương, không lấp lánh.
                drawOval(
                    color = Color.White.copy(alpha = 0.16f),
                    topLeft = Offset(bodyTopLeft.x + bodySize.width * 0.18f, bodyTopLeft.y + bodySize.height * 0.08f),
                    size = Size(bodySize.width * 0.28f, bodySize.height * 0.16f),
                )
            }
            JarMaterial.GLASS, JarMaterial.CRYSTAL -> {
                val shimmerAlpha = if (material == JarMaterial.CRYSTAL) 0.5f else 0.32f
                val bandWidth = bodySize.width * (if (material == JarMaterial.CRYSTAL) 0.30f else 0.22f)
                clipPath(bodyPath) {
                    val bandCenterX = bodyTopLeft.x + bodySize.width * shimmerPhase
                    rotate(22f, pivot = Offset(bandCenterX, bodyTopLeft.y + bodySize.height / 2f)) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0f),
                                    Color.White.copy(alpha = shimmerAlpha),
                                    Color.White.copy(alpha = 0f),
                                ),
                                startX = bandCenterX - bandWidth,
                                endX = bandCenterX + bandWidth,
                            ),
                            topLeft = Offset(bandCenterX - bandWidth, bodyTopLeft.y - h * 0.1f),
                            size = Size(bandWidth * 2f, bodySize.height + h * 0.2f),
                        )
                    }
                }
                if (material == JarMaterial.CRYSTAL) {
                    // Lấp lánh nhỏ quanh vai bình — chỉ Pha Lê mới có, đúng tinh thần "chất liệu cao cấp nhất".
                    val sparkleColor = lerp(primary, Color.White, 0.6f)
                    val sparkles = listOf(0.28f to 0.12f, 0.74f to 0.20f, 0.58f to 0.06f)
                    sparkles.forEachIndexed { i, (fx, fy) ->
                        val sx = bodyTopLeft.x + bodySize.width * fx
                        val sy = bodyTopLeft.y + bodySize.height * fy
                        val s = w * 0.018f * (0.7f + 0.3f * ((shimmerPhase + i * 0.33f).mod(1f)))
                        drawLine(sparkleColor, Offset(sx - s, sy), Offset(sx + s, sy), strokeWidth = 1.4f, cap = StrokeCap.Round)
                        drawLine(sparkleColor, Offset(sx, sy - s), Offset(sx, sy + s), strokeWidth = 1.4f, cap = StrokeCap.Round)
                    }
                }
                // Viền sáng nhẹ quanh mép — mô phỏng ánh sáng bo theo cạnh thuỷ tinh/pha lê.
                drawPath(
                    bodyPath,
                    color = Color.White.copy(alpha = if (material == JarMaterial.CRYSTAL) 0.22f else 0.12f),
                    style = Stroke(width = strokeW * 0.6f),
                )
            }
        }

        drawPath(bodyPath, color = ink, style = Stroke(width = strokeW))

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
