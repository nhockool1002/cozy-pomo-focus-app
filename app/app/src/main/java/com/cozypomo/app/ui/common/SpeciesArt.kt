package com.cozypomo.app.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Bộ sinh hình vẽ cho từng loài — port 1:1 từ backend/src/admin/components/species-art.ts
 * (cùng thuật toán archetype + palette + seed tên riêng dùng ở AdminJS) sang Compose Canvas,
 * để Forest/Shop/Species-detail/Splash trên Android render đúng hình ảnh loài thay vì hình
 * placeholder. Toạ độ giữ nguyên hệ 0..100 như SVG gốc (viewBox 0 0 100 100).
 */

data class SpeciesPalette(val base: Color, val dark: Color, val light: Color)

private fun hex(s: String) = Color(android.graphics.Color.parseColor(s))

// Bảng màu "hầm hố dễ thương" (T-124) — base là màu kẹo bão hoà, dark là 1 tông đậm hơn CÙNG
// gam màu (không còn gần đen như bản nháp đầu), "light" đổi vai trò thành glow: điểm nhấn sáng vui
// (mắt, sparkle) thay vì tông pastel nhạt như bản gốc. Tham khảo hướng creature-collector kiểu
// Coromon/Palia theo yêu cầu Dev1002 (2026-07-27) — không đổi tên field `light` để không phải sửa
// lại mọi chỗ archetype đã dùng `p.light` làm màu điểm nhấn (hoa/nấm/mai...).
val SPECIES_PALETTE = listOf(
    SpeciesPalette(hex("#FF9466"), hex("#B24E32"), hex("#FFD166")),
    SpeciesPalette(hex("#FFDD59"), hex("#C9A227"), hex("#FF8FD1")),
    SpeciesPalette(hex("#8BD17E"), hex("#4F8C45"), hex("#FFEA8A")),
    SpeciesPalette(hex("#6C9EFF"), hex("#3D5AC2"), hex("#7CF2E0")),
    SpeciesPalette(hex("#FF8FAE"), hex("#C24E72"), hex("#FFF07C")),
    SpeciesPalette(hex("#B98FE8"), hex("#7A4FB8"), hex("#7CF2E0")),
    SpeciesPalette(hex("#5FBF52"), hex("#357A2C"), hex("#FFD166")),
    SpeciesPalette(hex("#FF8A65"), hex("#C2502E"), hex("#7CF2E0")),
    SpeciesPalette(hex("#E8C97A"), hex("#B8933D"), hex("#FF8FD1")),
    SpeciesPalette(hex("#2FD9C4"), hex("#1C8C94"), hex("#FF8FD1")),
    SpeciesPalette(hex("#E8559C"), hex("#A8306E"), hex("#7CFFC4")),
    SpeciesPalette(hex("#8FA8E8"), hex("#4F5FA8"), hex("#FFD166")),
    SpeciesPalette(hex("#FFC94A"), hex("#C9860F"), hex("#7CFFC4")),
    SpeciesPalette(hex("#6FE0B8"), hex("#359C78"), hex("#FFD166")),
)
private val LEAF_BASE = hex("#6FCB5A")
private val LEAF_DARK = hex("#3F8C32")
private val INK = hex("#6D594E")
private val MOUND = hex("#E8D4A8")
private val SHINE = hex("#FFFBF3")
private val GLOW_GOLD = hex("#FFD166")
private val GLOW_PINK = hex("#FF8FD1")
private val GLOW_MINT = hex("#7CFFC4")

val RARITY_COLORS = mapOf("B" to hex("#FFE9B8"), "A" to hex("#7CF2E0"), "S" to GLOW_GOLD, "SS" to GLOW_PINK, "SSR" to GLOW_GOLD)
data class RarityBadgeColors(val fg: Color, val bg: Color)
val RARITY_BADGE = mapOf(
    "B" to RarityBadgeColors(hex("#8A7A5E"), hex("#FFF3D8")),
    "A" to RarityBadgeColors(hex("#2E7A5E"), hex("#DFF7EE")),
    "S" to RarityBadgeColors(hex("#8A6A10"), hex("#FFF3D0")),
    "SS" to RarityBadgeColors(hex("#C23E8A"), hex("#FFE3F3")),
    "SSR" to RarityBadgeColors(hex("#C9860F"), hex("#FFF7DD")),
)

/** Hoạ tiết sparkle (✦) dùng chung — 1 điểm nhấn nhỏ trên mỗi loài + vòng hào quang rarity, cho
 * cảm giác "lấp lánh" nhất quán xuyên suốt thay vì mảng màu phẳng. */
private fun sparkleMark(cx: Float, cy: Float, r: Float): Path {
    val r2 = r * 0.35f
    return Path().apply {
        moveTo(cx, cy - r); lineTo(cx + r2, cy - r2); lineTo(cx + r, cy); lineTo(cx + r2, cy + r2)
        lineTo(cx, cy + r); lineTo(cx - r2, cy + r2); lineTo(cx - r, cy); lineTo(cx - r2, cy - r2)
        close()
    }
}

// ---------- PRNG (mulberry32 + fnv-ish hash), y hệt species-art.ts để rotation "jitter" ổn định theo seed ----------
private fun hashStr(s: String): Int {
    var h = 1779033703
    for (ch in s) {
        h = (h xor ch.code) * 3432918353U.toInt()
        h = (h shl 13) or (h ushr 19)
    }
    return h
}

private fun rndFor(seed: String): Float {
    var a = hashStr(seed)
    a += 0x6D2B79F5
    var t = (a xor (a ushr 15)) * (a or 1)
    t = (t + ((t xor (t ushr 7)) * (t or 61))) xor t
    val unsigned = (t xor (t ushr 14)).toLong() and 0xFFFFFFFFL
    return (unsigned.toDouble() / 4294967296.0).toFloat()
}

private fun tri(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) = Path().apply {
    moveTo(x1, y1); lineTo(x2, y2); lineTo(x3, y3); close()
}

private fun DrawScope.ellipseAt(color: Color, cx: Float, cy: Float, rx: Float, ry: Float, alpha: Float = 1f) =
    drawOval(color, topLeft = Offset(cx - rx, cy - ry), size = Size(rx * 2, ry * 2), alpha = alpha)

private fun DrawScope.circleAt(color: Color, cx: Float, cy: Float, r: Float, alpha: Float = 1f) =
    drawCircle(color, radius = r, center = Offset(cx, cy), alpha = alpha)

private fun starPath(cx: Float, cy: Float, rOuter: Float, rInner: Float, points: Int, rot: Float): Path {
    val path = Path()
    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) rOuter else rInner
        val a = rot + i * Math.PI.toFloat() / points
        val x = cx + r * cos(a)
        val y = cy + r * sin(a)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

private data class LandOpts(val ear: String, val tail: String, val snout: String, val pattern: String, val extra: String? = null)
private val LAND_ARCH = mapOf(
    "fox" to LandOpts("pointy", "curl", "fox", "none"),
    "rabbit" to LandOpts("long", "fluffy", "none", "none"),
    "bear" to LandOpts("tiny", "stub", "bear", "none"),
    "cat" to LandOpts("pointy", "curl", "none", "stripe"),
    "bird" to LandOpts("none", "none", "beak", "wing"),
    "hedgehog" to LandOpts("tiny", "none", "bear", "spike"),
    "squirrel" to LandOpts("round", "fluffy", "none", "none"),
    "raccoon" to LandOpts("round", "ringed", "bear", "mask"),
    "deer" to LandOpts("tiny", "stub", "none", "none", "antler"),
    "owl" to LandOpts("tuft", "none", "beak", "none"),
)

private fun DrawScope.landArt(archetype: String, p: SpeciesPalette, rot: Float) = rotate(rot, pivot = Offset(50f, 50f)) {
    val o = LAND_ARCH[archetype] ?: LAND_ARCH.getValue("fox")
    when (o.tail) {
        "fluffy" -> { circleAt(p.base, 76f, 58f, 13f); circleAt(p.light, 81f, 49f, 9f) }
        "curl" -> drawPath(
            Path().apply { moveTo(74f, 66f); quadraticTo(94f, 66f, 90f, 46f); quadraticTo(88f, 32f, 76f, 40f) },
            p.base, style = Stroke(9f, cap = StrokeCap.Round),
        )
        "stub" -> circleAt(p.base, 75f, 64f, 7f)
        "ringed" -> {
            drawRoundRect(p.base, topLeft = Offset(72f, 46f), size = Size(10f, 28f), cornerRadius = CornerRadius(5f))
            drawRect(p.dark, topLeft = Offset(72f, 58f), size = Size(10f, 4f))
        }
    }
    ellipseAt(p.base, 50f, 62f, 26f, 22f)
    ellipseAt(SHINE, 44f, 68f, 8f, 11f, alpha = 0.85f)
    when (o.ear) {
        "pointy" -> {
            drawPath(tri(32f, 30f, 26f, 10f, 40f, 26f), p.dark)
            drawPath(tri(68f, 30f, 74f, 10f, 60f, 26f), p.dark)
            drawPath(tri(33f, 27f, 29f, 15f, 37f, 27f), p.light, alpha = 0.55f)
            drawPath(tri(67f, 27f, 71f, 15f, 63f, 27f), p.light, alpha = 0.55f)
        }
        "round" -> { circleAt(p.dark, 34f, 22f, 8f); circleAt(p.dark, 66f, 22f, 8f) }
        "long" -> { ellipseAt(p.dark, 38f, 10f, 6f, 16f); ellipseAt(p.dark, 62f, 10f, 6f, 16f) }
        "tiny" -> { circleAt(p.dark, 36f, 20f, 4f); circleAt(p.dark, 64f, 20f, 4f) }
        "tuft" -> {
            circleAt(p.dark, 34f, 22f, 7f); circleAt(p.dark, 66f, 22f, 7f)
            circleAt(p.light, 34f, 20f, 2.6f, alpha = 0.55f); circleAt(p.light, 66f, 20f, 2.6f, alpha = 0.55f)
        }
    }
    circleAt(p.base, 50f, 35f, 19f)
    when (o.snout) {
        "fox" -> drawPath(tri(50f, 40f, 42f, 48f, 58f, 48f), SHINE)
        "bear" -> ellipseAt(SHINE, 50f, 42f, 9f, 7f)
        "beak" -> drawPath(tri(50f, 38f, 41f, 44f, 50f, 46f), p.dark)
    }
    circleAt(INK, 43f, 33.5f, 3.2f)
    circleAt(INK, 57f, 33.5f, 3.2f)
    circleAt(SHINE, 41.8f, 32.3f, 1f)
    circleAt(SHINE, 55.8f, 32.3f, 1f)
    drawPath(sparkleMark(50f, 8f, 4f), p.light, alpha = 0.9f)
    when (o.pattern) {
        "stripe" -> drawPath(Path().apply { moveTo(28f, 56f); quadraticTo(50f, 62f, 72f, 56f) }, p.dark, alpha = 0.45f, style = Stroke(3f))
        "mask" -> drawPath(Path().apply { moveTo(34f, 32f); quadraticTo(50f, 40f, 66f, 32f) }, p.dark, alpha = 0.85f, style = Stroke(7f, cap = StrokeCap.Round))
        "spike" -> { drawPath(tri(34f, 46f, 30f, 38f, 38f, 44f), p.dark); drawPath(tri(56f, 40f, 58f, 30f, 62f, 40f), p.dark) }
        "wing" -> rotate(-20f, pivot = Offset(34f, 62f)) { ellipseAt(p.dark, 34f, 62f, 8f, 12f, alpha = 0.45f) }
    }
    if (o.extra == "antler") {
        val stroke = Stroke(2.4f, cap = StrokeCap.Round)
        drawPath(Path().apply { moveTo(42f, 16f); lineTo(38f, 4f) }, p.dark, style = stroke)
        drawPath(Path().apply { moveTo(42f, 16f); lineTo(46f, 6f) }, p.dark, style = stroke)
        drawPath(Path().apply { moveTo(58f, 16f); lineTo(62f, 4f) }, p.dark, style = stroke)
        drawPath(Path().apply { moveTo(58f, 16f); lineTo(54f, 6f) }, p.dark, style = stroke)
    }
}

private data class SeaOpts(val shape: String, val shellType: String? = null, val fin: String? = null, val legs: Int = 6, val dome: Boolean = false)
private val SEA_ARCH = mapOf(
    "turtle" to SeaOpts("shell", shellType = "dome"),
    "crab" to SeaOpts("shell", shellType = "claws"),
    "snail" to SeaOpts("shell", shellType = "spiral"),
    "fish" to SeaOpts("fish"),
    "starfish" to SeaOpts("star"),
    "seal" to SeaOpts("blob", fin = "flipper"),
    "dolphin" to SeaOpts("blob", fin = "dorsal"),
    "jellyfish" to SeaOpts("tentacle", legs = 6, dome = true),
    "octopus" to SeaOpts("tentacle", legs = 6, dome = false),
    "seahorse" to SeaOpts("seahorse"),
)

/** Mắt hí nhỏ dạng hạt sáng (thay chấm mực INK) — dùng cho toàn bộ sinh vật biển, tinh nghịch hơn
 * chấm tròn đơn sắc, màu lấy từ `p.light` (glow) nên đổi theo từng palette được gán. */
private fun DrawScope.seaEye(color: Color, cx: Float, cy: Float, r: Float = 2.6f) {
    drawPath(
        Path().apply { moveTo(cx - r, cy); quadraticTo(cx, cy - r, cx + r, cy); quadraticTo(cx, cy + r, cx - r, cy); close() },
        color,
    )
}

private fun DrawScope.seaArt(archetype: String, p: SpeciesPalette, rot: Float) = rotate(rot, pivot = Offset(50f, 50f)) {
    val o = SEA_ARCH[archetype] ?: SEA_ARCH.getValue("fish")
    when (o.shape) {
        "shell" -> when (o.shellType) {
            "dome" -> {
                circleAt(p.base, 24f, 54f, 10f)
                ellipseAt(p.dark, 54f, 56f, 26f, 19f)
                ellipseAt(SHINE, 54f, 50f, 9f, 6f, alpha = 0.6f)
                seaEye(p.light, 24f, 52f, 2.2f)
                drawPath(sparkleMark(70f, 40f, 3.6f), p.light, alpha = 0.85f)
            }
            "claws" -> {
                ellipseAt(p.base, 50f, 58f, 27f, 15f)
                circleAt(p.dark, 22f, 40f, 8f); circleAt(p.dark, 78f, 40f, 8f)
                ellipseAt(SHINE, 44f, 52f, 8f, 4f, alpha = 0.6f)
                seaEye(p.light, 42f, 54f, 2.2f); seaEye(p.light, 58f, 54f, 2.2f)
                drawPath(sparkleMark(50f, 36f, 3.6f), p.light, alpha = 0.85f)
            }
            else -> {
                circleAt(p.base, 46f, 52f, 17f); circleAt(p.dark, 46f, 52f, 12f, alpha = 0.5f)
                ellipseAt(p.base, 68f, 66f, 14f, 9f)
                seaEye(p.light, 78f, 62f, 2.2f)
                drawPath(sparkleMark(38f, 40f, 3.6f), p.light, alpha = 0.85f)
            }
        }
        "fish" -> {
            drawPath(Path().apply { moveTo(78f, 50f); lineTo(94f, 40f); lineTo(90f, 50f); lineTo(94f, 60f); close() }, p.dark)
            drawPath(Path().apply { moveTo(28f, 50f); cubicTo(28f, 30f, 72f, 30f, 76f, 50f); cubicTo(72f, 70f, 28f, 70f, 28f, 50f); close() }, p.base)
            ellipseAt(SHINE, 38f, 58f, 8f, 5f, alpha = 0.55f)
            seaEye(p.light, 42f, 48f, 2.8f)
            drawPath(sparkleMark(58f, 34f, 3.6f), p.light, alpha = 0.85f)
        }
        "star" -> {
            drawPath(starPath(50f, 54f, 30f, 13f, 5, -Math.PI.toFloat() / 2), p.base)
            circleAt(SHINE, 50f, 44f, 4f, alpha = 0.5f)
            seaEye(p.dark, 44f, 48f, 2.2f); seaEye(p.dark, 56f, 48f, 2.2f)
            drawPath(sparkleMark(50f, 14f, 3.6f), p.light, alpha = 0.9f)
        }
        "blob" -> {
            drawPath(Path().apply { moveTo(80f, 50f); lineTo(94f, 42f); lineTo(90f, 58f); lineTo(94f, 68f); lineTo(80f, 60f); close() }, p.dark)
            ellipseAt(p.base, 48f, 55f, 30f, 21f)
            ellipseAt(SHINE, 38f, 62f, 9f, 6f, alpha = 0.6f)
            if (o.fin == "dorsal") drawPath(tri(58f, 34f, 66f, 16f, 68f, 36f), p.dark)
            else { ellipseAt(p.dark, 24f, 58f, 8f, 4f); ellipseAt(p.dark, 76f, 58f, 8f, 4f) }
            seaEye(p.light, 66f, 48f, 2.6f)
            drawPath(sparkleMark(56f, 30f, 3.6f), p.light, alpha = 0.85f)
        }
        "tentacle" -> {
            val n = o.legs
            for (i in 0 until n) {
                val x = 30f + i * (40f / (n - 1))
                val sway = if (i % 2 == 0) 6f else -6f
                drawPath(Path().apply { moveTo(x, 60f); quadraticTo(x + sway, 74f, x, 88f) }, p.dark, style = Stroke(3.2f, cap = StrokeCap.Round))
            }
            if (o.dome) {
                drawPath(Path().apply { moveTo(24f, 58f); arcTo(Rect(Offset(24f, 32f), Size(52f, 52f)), 180f, 180f, false); close() }, p.base)
            } else circleAt(p.base, 50f, 48f, 24f)
            ellipseAt(SHINE, 40f, 38f, 8f, 5f, alpha = 0.55f)
            seaEye(p.light, 42f, 48f, 2.6f); seaEye(p.light, 58f, 48f, 2.6f)
            drawPath(sparkleMark(50f, 20f, 3.8f), p.light, alpha = 0.9f)
        }
        "seahorse" -> {
            drawPath(
                Path().apply {
                    moveTo(46f, 82f)
                    cubicTo(30f, 82f, 30f, 66f, 42f, 60f)
                    cubicTo(54f, 54f, 40f, 48f, 42f, 38f)
                    cubicTo(44f, 28f, 58f, 24f, 62f, 32f)
                },
                p.base, style = Stroke(13f, cap = StrokeCap.Round),
            )
            ellipseAt(SHINE, 40f, 66f, 4f, 6f, alpha = 0.55f)
            seaEye(p.light, 58f, 30f, 2.4f)
            drawPath(sparkleMark(34f, 44f, 3.4f), p.light, alpha = 0.85f)
        }
    }
}

private fun DrawScope.plantArt(archetype: String, p: SpeciesPalette, lean: Float) = rotate(lean, pivot = Offset(50f, 78f)) {
    ellipseAt(MOUND, 50f, 86f, 22f, 6f)
    fun stemAndLeaves() {
        drawPath(Path().apply { moveTo(50f, 84f); lineTo(50f, 46f) }, LEAF_BASE, style = Stroke(5f, cap = StrokeCap.Round))
        ellipseAt(LEAF_BASE, 38f, 66f, 10f, 5f)
        ellipseAt(LEAF_BASE, 62f, 58f, 10f, 5f)
    }
    when (archetype) {
        "flowerRound" -> {
            stemAndLeaves()
            for (i in 0 until 6) {
                val a = i * Math.PI.toFloat() / 3
                circleAt(p.base, 50f + 16 * cos(a), 30f + 16 * sin(a), 9f)
            }
            circleAt(p.light, 50f, 30f, 7f)
            drawPath(sparkleMark(70f, 18f, 3.6f), p.light, alpha = 0.85f)
        }
        "flowerStar" -> {
            stemAndLeaves()
            drawPath(starPath(50f, 30f, 15f, 7f, 6, 0f), p.base)
            circleAt(p.light, 50f, 30f, 4f)
            drawPath(sparkleMark(72f, 20f, 3.6f), p.light, alpha = 0.85f)
        }
        "mushroom" -> {
            drawRoundRect(hex("#F1E6D2"), topLeft = Offset(44f, 52f), size = Size(12f, 32f), cornerRadius = CornerRadius(5f), style = androidx.compose.ui.graphics.drawscope.Fill)
            drawRoundRect(p.dark, topLeft = Offset(44f, 52f), size = Size(12f, 32f), cornerRadius = CornerRadius(5f), style = Stroke(1.5f))
            drawPath(Path().apply { moveTo(24f, 52f); arcTo(Rect(Offset(24f, 32f), Size(52f, 40f)), 180f, 180f, false); close() }, p.base)
            circleAt(p.light, 38f, 42f, 3f); circleAt(p.light, 58f, 38f, 3.4f)
            drawPath(sparkleMark(70f, 30f, 3.4f), p.light, alpha = 0.85f)
        }
        "fern" -> {
            for (i in 0 until 3) {
                val dx = (i - 1) * 16f
                drawPath(Path().apply { moveTo(50f, 84f); quadraticTo(50f + dx, 50f, 50f + dx * 1.4f, 24f) }, p.base, style = Stroke(4f, cap = StrokeCap.Round))
                circleAt(p.dark, 50f + dx * 1.4f, 24f, 2.6f)
            }
            drawPath(sparkleMark(68f, 30f, 3.6f), p.light, alpha = 0.85f)
        }
        "succulent" -> {
            for (i in 0 until 7) {
                val a = i * (2 * Math.PI.toFloat() / 7)
                ellipseAt(p.base, 50f + 15 * cos(a), 66f + 15 * sin(a) * 0.6f, 9f, 14f)
            }
            circleAt(p.light, 50f, 66f, 7f)
            drawPath(sparkleMark(70f, 40f, 3.6f), p.light, alpha = 0.85f)
        }
        "cactus" -> {
            drawPath(
                Path().apply { moveTo(36f, 86f); lineTo(34f, 50f); lineTo(40f, 36f); lineTo(60f, 36f); lineTo(66f, 50f); lineTo(64f, 86f); close() },
                p.base,
            )
            drawPath(Path().apply { moveTo(42f, 42f); lineTo(42f, 80f) }, p.dark, alpha = 0.4f, style = Stroke(1.4f))
            drawPath(Path().apply { moveTo(50f, 38f); lineTo(50f, 82f) }, p.dark, alpha = 0.4f, style = Stroke(1.4f))
            drawPath(Path().apply { moveTo(58f, 42f); lineTo(58f, 80f) }, p.dark, alpha = 0.4f, style = Stroke(1.4f))
            for (ty in floatArrayOf(46f, 62f, 74f)) {
                drawPath(tri(34f, ty, 25f, ty - 2f, 34f, ty + 4f), p.dark)
                drawPath(tri(66f, ty, 75f, ty - 2f, 66f, ty + 4f), p.dark)
            }
            drawPath(starPath(50f, 26f, 11f, 4.5f, 5, -Math.PI.toFloat() / 2), p.light)
            circleAt(SHINE, 55f, 24f, 1.6f, alpha = 0.9f)
        }
        "berry" -> {
            ellipseAt(LEAF_BASE, 40f, 60f, 16f, 14f); ellipseAt(LEAF_BASE, 62f, 56f, 15f, 13f)
            circleAt(p.base, 38f, 58f, 3.4f); circleAt(p.base, 52f, 66f, 3.4f); circleAt(p.base, 62f, 54f, 3.4f)
            circleAt(SHINE, 37f, 56.5f, 1f, alpha = 0.8f)
            drawPath(sparkleMark(68f, 34f, 3.6f), p.light, alpha = 0.85f)
        }
        "bamboo" -> {
            for (i in 0 until 3) {
                val x = 38f + i * 12f
                drawRoundRect(p.base, topLeft = Offset(x - 4f, 20f), size = Size(8f, 64f), cornerRadius = CornerRadius(4f))
                drawRect(p.dark, topLeft = Offset(x - 4f, 36f), size = Size(8f, 3f))
                drawRect(p.dark, topLeft = Offset(x - 4f, 54f), size = Size(8f, 3f))
            }
            drawPath(sparkleMark(66f, 26f, 3.6f), p.light, alpha = 0.85f)
        }
        "vine" -> {
            drawPath(
                Path().apply { moveTo(28f, 82f); quadraticTo(50f, 60f, 34f, 44f); quadraticTo(20f, 30f, 40f, 20f) },
                LEAF_BASE, style = Stroke(4f, cap = StrokeCap.Round),
            )
            circleAt(p.base, 40f, 20f, 6f)
            drawPath(sparkleMark(64f, 34f, 3.6f), p.light, alpha = 0.85f)
        }
        "tree" -> {
            drawRoundRect(hex("#B98A5D"), topLeft = Offset(45f, 50f), size = Size(10f, 34f), cornerRadius = CornerRadius(4f))
            circleAt(p.base, 40f, 38f, 16f); circleAt(p.base, 60f, 36f, 14f); circleAt(p.light, 50f, 26f, 15f)
            drawPath(sparkleMark(74f, 20f, 3.6f), p.light, alpha = 0.85f)
        }
    }
}

private fun DrawScope.mythicArt(archetype: String, p: SpeciesPalette, rot: Float) = rotate(rot, pivot = Offset(50f, 55f)) {
    when (archetype) {
        "phoenix" -> {
            val plumeColors = listOf(p.dark, p.base, p.light)
            for (i in 0 until 3) {
                val dx = -12f + i * 12f
                drawPath(Path().apply { moveTo(50f, 62f); quadraticTo(40f + dx, 80f, 28f + dx, 96f) }, plumeColors[i], style = Stroke(6f, cap = StrokeCap.Round))
            }
            drawPath(Path().apply { moveTo(34f, 50f); quadraticTo(8f, 38f, 6f, 60f); quadraticTo(24f, 68f, 38f, 58f); close() }, p.base)
            drawPath(Path().apply { moveTo(66f, 50f); quadraticTo(92f, 38f, 94f, 60f); quadraticTo(76f, 68f, 62f, 58f); close() }, p.base)
            ellipseAt(p.base, 50f, 56f, 16f, 20f)
            ellipseAt(SHINE, 46f, 60f, 5f, 8f, alpha = 0.5f)
            circleAt(p.base, 50f, 34f, 12f)
            drawPath(tri(50f, 22f, 45f, 8f, 55f, 14f), p.dark)
            seaEye(p.light, 54f, 32f, 2f)
            drawPath(sparkleMark(74f, 44f, 3.6f), p.light, alpha = 0.85f)
        }
        "qilin" -> {
            ellipseAt(p.dark, 36f, 82f, 5f, 8f); ellipseAt(p.dark, 64f, 82f, 5f, 8f)
            ellipseAt(p.base, 50f, 62f, 24f, 18f)
            ellipseAt(SHINE, 42f, 68f, 6f, 8f, alpha = 0.55f)
            drawPath(Path().apply { moveTo(30f, 40f); quadraticTo(18f, 50f, 28f, 62f) }, p.dark, style = Stroke(4f, cap = StrokeCap.Round))
            drawPath(Path().apply { moveTo(70f, 40f); quadraticTo(82f, 50f, 72f, 62f) }, p.dark, style = Stroke(4f, cap = StrokeCap.Round))
            circleAt(p.base, 50f, 38f, 16f)
            drawPath(tri(50f, 20f, 46f, 4f, 54f, 4f), p.light)
            seaEye(p.light, 44f, 36f, 2.2f); seaEye(p.light, 56f, 36f, 2.2f)
            drawPath(sparkleMark(74f, 24f, 3.6f), p.light, alpha = 0.85f)
        }
        "dragon" -> {
            drawPath(
                Path().apply { moveTo(18f, 74f); cubicTo(28f, 42f, 50f, 62f, 46f, 40f); cubicTo(42f, 18f, 66f, 16f, 76f, 30f) },
                p.base, style = Stroke(13f, cap = StrokeCap.Round),
            )
            ellipseAt(SHINE, 28f, 58f, 3.6f, 5.4f, alpha = 0.55f)
            ellipseAt(SHINE, 40f, 38f, 3.2f, 4.6f, alpha = 0.55f)
            drawPath(tri(40f, 42f, 36f, 28f, 46f, 36f), p.dark)
            drawPath(tri(58f, 24f, 56f, 10f, 66f, 20f), p.dark)
            drawPath(tri(74f, 26f, 86f, 19f, 82f, 32f), p.dark)
            drawPath(tri(72f, 22f, 80f, 11f, 78f, 24f), p.dark)
            drawPath(tri(70f, 32f, 78f, 34f, 70f, 40f), SHINE)
            seaEye(p.light, 74f, 27f, 2.4f)
            drawPath(sparkleMark(58f, 46f, 3.6f), p.light, alpha = 0.85f)
            drawPath(tri(10f, 80f, 18f, 72f, 20f, 84f), p.base)
        }
        "ninetail" -> {
            ellipseAt(p.base, 46f, 62f, 18f, 16f)
            ellipseAt(SHINE, 40f, 68f, 5f, 7f, alpha = 0.5f)
            for (i in 0 until 5) {
                val a = -50f + i * 25f
                val color = if (i % 2 == 0) p.base else p.dark
                rotate(a, pivot = Offset(56f, 62f)) {
                    drawPath(Path().apply { moveTo(56f, 62f); quadraticTo(82f, 68f, 86f, 80f + i * 2f) }, color, style = Stroke(5f, cap = StrokeCap.Round))
                }
            }
            drawPath(tri(30f, 34f, 23f, 14f, 38f, 30f), p.dark)
            drawPath(tri(62f, 34f, 71f, 14f, 56f, 30f), p.dark)
            circleAt(p.base, 46f, 36f, 13f)
            seaEye(p.light, 40f, 34f, 2.2f); seaEye(p.light, 52f, 34f, 2.2f)
            drawPath(sparkleMark(70f, 24f, 3.6f), p.light, alpha = 0.85f)
        }
        "crane" -> {
            val legStroke = Stroke(2.4f, cap = StrokeCap.Round)
            drawPath(Path().apply { moveTo(50f, 84f); lineTo(48f, 97f) }, p.dark, style = legStroke)
            drawPath(Path().apply { moveTo(62f, 84f); lineTo(64f, 97f) }, p.dark, style = legStroke)
            ellipseAt(p.base, 56f, 72f, 20f, 14f)
            ellipseAt(SHINE, 50f, 76f, 6f, 8f, alpha = 0.5f)
            drawPath(Path().apply { moveTo(40f, 66f); quadraticTo(12f, 54f, 10f, 76f); quadraticTo(34f, 84f, 48f, 72f); close() }, p.base)
            drawPath(Path().apply { moveTo(56f, 70f); quadraticTo(38f, 50f, 54f, 28f) }, p.base, style = Stroke(9f, cap = StrokeCap.Round))
            circleAt(p.base, 54f, 25f, 7f)
            seaEye(p.light, 57f, 18f, 2.6f)
            circleAt(INK, 52f, 20f, 1.6f)
            drawPath(sparkleMark(76f, 40f, 3.6f), p.light, alpha = 0.85f)
        }
        "sleepyGiant" -> {
            // T-127 — Kapi Ngái Ngủ: port 1:1 từ species-art.ts (xem comment ở đó) — thân to gấp
            // rưỡi các archetype khác, tai nhỏ xíu, mắt nhắm (2 nét cong) + chuỗi 3 bong bóng mơ
            // màng bay lên góc phải thay cho chữ "Zzz" thật (Canvas không có glyph sẵn để giữ port 1:1).
            ellipseAt(p.base, 50f, 64f, 30f, 24f)
            ellipseAt(SHINE, 46f, 72f, 15f, 15f, alpha = 0.9f)
            circleAt(p.dark, 33f, 28f, 6f); circleAt(p.dark, 67f, 28f, 6f)
            circleAt(p.base, 50f, 36f, 17f)
            ellipseAt(SHINE, 50f, 42f, 8f, 6f, alpha = 0.9f)
            drawPath(Path().apply { moveTo(40f, 33f); quadraticTo(44f, 29f, 48f, 33f) }, INK, style = Stroke(2.4f, cap = StrokeCap.Round))
            drawPath(Path().apply { moveTo(52f, 33f); quadraticTo(56f, 29f, 60f, 33f) }, INK, style = Stroke(2.4f, cap = StrokeCap.Round))
            drawPath(Path().apply { moveTo(46f, 46f); quadraticTo(50f, 49f, 54f, 46f) }, INK, style = Stroke(1.8f, cap = StrokeCap.Round))
            circleAt(p.light, 64f, 20f, 2.2f, alpha = 0.8f)
            circleAt(p.light, 72f, 13f, 3f, alpha = 0.65f)
            circleAt(p.light, 80f, 7f, 3.8f, alpha = 0.5f)
            drawPath(sparkleMark(18f, 60f, 3.6f), p.light, alpha = 0.85f)
        }
    }
}

// 8 điểm sparkle dùng chung cho S/SS/SSR — cùng bộ toạ độ với bản trước, chỉ đổi màu/số lượng/độ
// dày theo cấp bậc để "leo thang" rõ (T-124: đổi ngôn ngữ hình ảnh từ khói/sét/lửa sang lấp lánh).
private val AURA_SPARK_PTS = listOf(26f to 22f, 74f to 26f, 70f to 74f, 22f to 70f, 50f to 14f, 50f to 86f, 12f to 46f, 88f to 54f)

/** Vẽ vầng hào quang phía sau ảnh loài theo cấp bậc — B/A chỉ có vòng nhẹ, S/SS thêm sparkle xoay
 * quanh (4 rồi 8 điểm), SSR thêm quầng sáng vàng-hồng + lõi trắng + 2 vòng nét đứt xoay ngược chiều. */
private fun DrawScope.rarityAura(rarity: String?, breathe: Float, spin: Float, spinRev: Float, flicker: Float) {
    when (rarity) {
        "SSR" -> {
            val glowBrush = Brush.radialGradient(
                colors = listOf(GLOW_GOLD, GLOW_PINK.copy(alpha = 0.6f), GLOW_PINK.copy(alpha = 0f)),
                center = Offset(50f, 50f), radius = 42f,
            )
            drawCircle(brush = glowBrush, radius = 42f, center = Offset(50f, 50f), blendMode = androidx.compose.ui.graphics.BlendMode.Screen)
            val coreBrush = Brush.radialGradient(
                colors = listOf(hex("#FFFDF0"), GLOW_GOLD.copy(alpha = 0f)),
                center = Offset(50f, 50f), radius = 16f,
            )
            drawCircle(brush = coreBrush, radius = 16f, center = Offset(50f, 50f), blendMode = androidx.compose.ui.graphics.BlendMode.Screen)
            val ringScale = 0.88f + breathe * (1.16f - 0.88f)
            scale(ringScale, pivot = Offset(50f, 50f)) {
                drawCircle(GLOW_GOLD, radius = 39f, center = Offset(50f, 50f), alpha = 0.7f + breathe * 0.25f, style = Stroke(3.6f))
            }
            rotate(spin, pivot = Offset(50f, 50f)) {
                drawCircle(
                    GLOW_GOLD, radius = 34f, center = Offset(50f, 50f), alpha = 0.85f,
                    style = Stroke(2.2f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 6f))),
                )
            }
            rotate(spinRev, pivot = Offset(50f, 50f)) {
                drawCircle(
                    GLOW_PINK, radius = 25f, center = Offset(50f, 50f), alpha = 0.7f,
                    style = Stroke(1.8f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))),
                )
            }
            val sparkColors = listOf(GLOW_GOLD, GLOW_PINK, GLOW_MINT, hex("#FFFDF0"))
            for (i in 0 until 7) {
                val (x, y) = AURA_SPARK_PTS[i % AURA_SPARK_PTS.size]
                val r = (3.4f + (i % 3) * 0.7f) * 1.3f
                scale(0.75f + flicker * 0.6f, pivot = Offset(x, y)) {
                    drawPath(sparkleMark(x, y, r), sparkColors[i % sparkColors.size], alpha = 0.55f + flicker * 0.45f)
                }
            }
            circleAt(hex("#FFFDF0"), 50f, 50f, 15f + breathe * 2f, alpha = 0.55f + breathe * 0.3f)
        }
        "SS" -> {
            val ringScale = 0.94f + breathe * (1.08f - 0.94f)
            scale(ringScale, pivot = Offset(50f, 50f)) {
                drawCircle(GLOW_PINK, radius = 39f, center = Offset(50f, 50f), alpha = 0.4f, style = Stroke(2f))
            }
            rotate(spinRev * 0.6f, pivot = Offset(50f, 50f)) {
                for (i in 0 until 8) {
                    val (x, y) = AURA_SPARK_PTS[i]
                    val color = if (i % 2 == 0) GLOW_PINK else GLOW_GOLD
                    drawPath(sparkleMark(x, y, 3.6f + flicker * 0.8f), color, alpha = 0.7f + flicker * 0.3f)
                }
            }
        }
        "S" -> {
            val ringScale = 0.96f + breathe * (1.05f - 0.96f)
            scale(ringScale, pivot = Offset(50f, 50f)) {
                drawCircle(GLOW_GOLD, radius = 38f, center = Offset(50f, 50f), alpha = 0.28f, style = Stroke(1.5f))
            }
            rotate(spin * 0.5f, pivot = Offset(50f, 50f)) {
                for (i in 0 until 4) {
                    val (x, y) = AURA_SPARK_PTS[i * 2]
                    drawPath(sparkleMark(x, y, 3.2f + flicker * 0.5f), GLOW_GOLD, alpha = 0.75f + flicker * 0.25f)
                }
            }
        }
        "A" -> {
            val ringScale = 0.96f + breathe * (1.05f - 0.96f)
            scale(ringScale, pivot = Offset(50f, 50f)) {
                drawCircle(RARITY_COLORS.getValue("A"), radius = 30f, center = Offset(50f, 50f), alpha = 0.5f, style = Stroke(2f))
                drawCircle(RARITY_COLORS.getValue("A"), radius = 24f, center = Offset(50f, 50f), alpha = 0.3f, style = Stroke(1f))
            }
        }
        "B" -> {
            val ringScale = 0.97f + breathe * (1.04f - 0.97f)
            scale(ringScale, pivot = Offset(50f, 50f)) {
                drawCircle(
                    RARITY_COLORS.getValue("B"), radius = 34f, center = Offset(50f, 50f), alpha = 0.4f,
                    style = Stroke(1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 4f))),
                )
            }
        }
    }
}

/**
 * Icon 1 loài, vẽ động (bồng bềnh nhẹ liên tục "sp-float" + hào quang theo cấp bậc) —
 * dùng ở Khu rừng, Chi tiết loài, danh sách Cửa hàng, và Splash.
 */
@Composable
fun SpeciesArtIcon(
    category: String,
    archetype: String,
    paletteIdx: Int,
    seed: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    rarity: String? = null,
    animate: Boolean = true,
) {
    val infinite = rememberInfiniteTransition(label = "speciesArt")
    val floatY by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3400, easing = androidx.compose.animation.core.FastOutSlowInEasing), RepeatMode.Reverse),
        label = "floatY",
    )
    val breathe by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = androidx.compose.animation.core.FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe",
    )
    val flicker by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "flicker",
    )
    val spin by infinite.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing)),
        label = "spin",
    )
    val spinRev by infinite.animateFloat(
        initialValue = 360f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing)),
        label = "spinRev",
    )
    // Biên độ hơi lớn hơn FloatingIcon (item Cửa hàng) một chút — icon loài vẽ chi tiết hơn nên
    // cần "bồng bềnh" rõ hơn để cảm nhận được, nhất là ở lưới Khu rừng size 42dp.
    val translateYPx = if (animate) (-4.5f + floatY * 9f) else 0f
    val pal = SPECIES_PALETTE[((paletteIdx % SPECIES_PALETTE.size) + SPECIES_PALETTE.size) % SPECIES_PALETTE.size]
    val rot = rndFor(seed) * 8f - 4f

    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer { translationY = translateYPx },
    ) {
        val s = this.size.minDimension / 100f
        scale(s, pivot = Offset(0f, 0f)) {
            rarityAura(rarity, if (animate) breathe else 0.5f, if (animate) spin else 0f, if (animate) spinRev else 0f, if (animate) flicker else 0.5f)
            when (category) {
                "FOREST" -> landArt(archetype, pal, rot)
                "SEA" -> seaArt(archetype, pal, rot)
                "PLANT" -> plantArt(archetype, pal, rot)
                else -> mythicArt(archetype, pal, rot * 0.75f)
            }
        }
    }
}

/** Badge cấp bậc nhỏ (VD "S", "SSR") — dùng làm overlay góc trên-trái thẻ loài. */
@Composable
fun RarityBadge(rarity: String, modifier: Modifier = Modifier) {
    val colors = RARITY_BADGE[rarity] ?: return
    androidx.compose.material3.Surface(
        color = colors.bg, contentColor = colors.fg,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
        modifier = modifier,
    ) {
        androidx.compose.material3.Text(
            text = rarity,
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
        )
    }
}
