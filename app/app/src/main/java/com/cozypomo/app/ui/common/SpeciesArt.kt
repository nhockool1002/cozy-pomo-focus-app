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

val SPECIES_PALETTE = listOf(
    SpeciesPalette(hex("#E2965F"), hex("#B9713D"), hex("#F6D9BB")),
    SpeciesPalette(hex("#F0D98C"), hex("#C9A94A"), hex("#FBF0D0")),
    SpeciesPalette(hex("#9CB380"), hex("#6E8455"), hex("#DCE6CC")),
    SpeciesPalette(hex("#9AC0D9"), hex("#5E92AE"), hex("#DCEBF3")),
    SpeciesPalette(hex("#E7A8B0"), hex("#C06E7C"), hex("#F8E0E3")),
    SpeciesPalette(hex("#B58BC4"), hex("#875B9C"), hex("#E9DAF0")),
    SpeciesPalette(hex("#7C9A5A"), hex("#516B37"), hex("#CFE0BC")),
    SpeciesPalette(hex("#E8876B"), hex("#C25A3D"), hex("#F8D5C8")),
    SpeciesPalette(hex("#D9C29A"), hex("#AC8F5C"), hex("#F1E7D2")),
    SpeciesPalette(hex("#6FB6A8"), hex("#3F8778"), hex("#CDE9E2")),
    SpeciesPalette(hex("#C9607A"), hex("#9A3E55"), hex("#F0C7D2")),
    SpeciesPalette(hex("#7E8FB0"), hex("#556487"), hex("#D6DCEA")),
    SpeciesPalette(hex("#E3B04B"), hex("#B3831F"), hex("#F7E2AE")),
    SpeciesPalette(hex("#8FCDB0"), hex("#5A9C80"), hex("#D3EEE0")),
)
private val LEAF_BASE = hex("#8FB36B")
private val LEAF_DARK = hex("#5F7F45")
private val INK = hex("#6D594E")
private val MOUND = hex("#D9C29A")
private val GOLD = hex("#F4D160")
private val FLAME = hex("#FF8A3D")
private val EMBER = hex("#E76F51")

val RARITY_COLORS = mapOf("B" to hex("#B7A896"), "A" to hex("#A8D08D"), "S" to GOLD, "SS" to EMBER, "SSR" to GOLD)
data class RarityBadgeColors(val fg: Color, val bg: Color)
val RARITY_BADGE = mapOf(
    "B" to RarityBadgeColors(hex("#7A6C5C"), hex("#EFE4C8")),
    "A" to RarityBadgeColors(hex("#3F5C2E"), hex("#E9F2E0")),
    "S" to RarityBadgeColors(hex("#8A6A10"), hex("#FBF0CE")),
    "SS" to RarityBadgeColors(hex("#B23F22"), hex("#FBE0D7")),
    "SSR" to RarityBadgeColors(GOLD, hex("#2A1F16")),
)

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
    when (o.ear) {
        "pointy" -> { drawPath(tri(32f, 30f, 26f, 10f, 40f, 26f), p.dark); drawPath(tri(68f, 30f, 74f, 10f, 60f, 26f), p.dark) }
        "round" -> { circleAt(p.dark, 34f, 22f, 8f); circleAt(p.dark, 66f, 22f, 8f) }
        "long" -> { ellipseAt(p.dark, 38f, 10f, 6f, 16f); ellipseAt(p.dark, 62f, 10f, 6f, 16f) }
        "tiny" -> { circleAt(p.dark, 36f, 20f, 4f); circleAt(p.dark, 64f, 20f, 4f) }
        "tuft" -> { circleAt(p.dark, 34f, 22f, 7f); circleAt(p.dark, 66f, 22f, 7f) }
    }
    circleAt(p.base, 50f, 35f, 19f)
    when (o.snout) {
        "fox" -> drawPath(tri(50f, 40f, 42f, 48f, 58f, 48f), p.light)
        "bear" -> ellipseAt(p.light, 50f, 42f, 9f, 7f)
        "beak" -> drawPath(tri(50f, 38f, 41f, 44f, 50f, 46f), p.dark)
    }
    circleAt(INK, 43f, 34f, 2.6f)
    circleAt(INK, 57f, 34f, 2.6f)
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

private fun DrawScope.seaArt(archetype: String, p: SpeciesPalette, rot: Float) = rotate(rot, pivot = Offset(50f, 50f)) {
    val o = SEA_ARCH[archetype] ?: SEA_ARCH.getValue("fish")
    when (o.shape) {
        "shell" -> when (o.shellType) {
            "dome" -> { circleAt(p.base, 24f, 54f, 10f); ellipseAt(p.dark, 54f, 56f, 26f, 19f); ellipseAt(p.light, 54f, 52f, 9f, 9f, alpha = 0.55f); circleAt(INK, 24f, 52f, 2.2f) }
            "claws" -> { ellipseAt(p.base, 50f, 58f, 27f, 15f); circleAt(p.dark, 22f, 40f, 8f); circleAt(p.dark, 78f, 40f, 8f); circleAt(INK, 42f, 54f, 2.2f); circleAt(INK, 58f, 54f, 2.2f) }
            else -> { circleAt(p.base, 46f, 52f, 17f); circleAt(p.dark, 46f, 52f, 12f, alpha = 0.5f); ellipseAt(p.base, 68f, 66f, 14f, 9f); circleAt(INK, 78f, 62f, 2.2f) }
        }
        "fish" -> {
            drawPath(Path().apply { moveTo(78f, 50f); lineTo(94f, 40f); lineTo(90f, 50f); lineTo(94f, 60f); close() }, p.dark)
            drawPath(Path().apply { moveTo(28f, 50f); cubicTo(28f, 30f, 72f, 30f, 76f, 50f); cubicTo(72f, 70f, 28f, 70f, 28f, 50f); close() }, p.base)
            circleAt(INK, 42f, 48f, 2.6f)
        }
        "star" -> { drawPath(starPath(50f, 54f, 30f, 13f, 5, -Math.PI.toFloat() / 2), p.base); circleAt(INK, 44f, 48f, 2.2f); circleAt(INK, 56f, 48f, 2.2f) }
        "blob" -> {
            drawPath(Path().apply { moveTo(80f, 50f); lineTo(94f, 42f); lineTo(90f, 58f); lineTo(94f, 68f); lineTo(80f, 60f); close() }, p.dark)
            ellipseAt(p.base, 48f, 55f, 30f, 21f)
            if (o.fin == "dorsal") drawPath(tri(58f, 34f, 66f, 16f, 68f, 36f), p.dark)
            else { ellipseAt(p.dark, 24f, 58f, 8f, 4f); ellipseAt(p.dark, 76f, 58f, 8f, 4f) }
            circleAt(INK, 66f, 48f, 2.4f)
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
            circleAt(INK, 42f, 48f, 2.4f); circleAt(INK, 58f, 48f, 2.4f)
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
            circleAt(INK, 58f, 30f, 2.4f)
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
        }
        "flowerStar" -> {
            stemAndLeaves()
            drawPath(starPath(50f, 30f, 15f, 7f, 6, 0f), p.base)
            circleAt(p.light, 50f, 30f, 4f)
        }
        "mushroom" -> {
            drawRoundRect(hex("#F1E6D2"), topLeft = Offset(44f, 52f), size = Size(12f, 32f), cornerRadius = CornerRadius(5f), style = androidx.compose.ui.graphics.drawscope.Fill)
            drawRoundRect(p.dark, topLeft = Offset(44f, 52f), size = Size(12f, 32f), cornerRadius = CornerRadius(5f), style = Stroke(1.5f))
            drawPath(Path().apply { moveTo(24f, 52f); arcTo(Rect(Offset(24f, 32f), Size(52f, 40f)), 180f, 180f, false); close() }, p.base)
            circleAt(p.light, 38f, 42f, 3f); circleAt(p.light, 58f, 38f, 3.4f)
        }
        "fern" -> for (i in 0 until 3) {
            val dx = (i - 1) * 16f
            drawPath(Path().apply { moveTo(50f, 84f); quadraticTo(50f + dx, 50f, 50f + dx * 1.4f, 24f) }, LEAF_BASE, style = Stroke(4f, cap = StrokeCap.Round))
        }
        "succulent" -> {
            for (i in 0 until 7) {
                val a = i * (2 * Math.PI.toFloat() / 7)
                ellipseAt(p.base, 50f + 15 * cos(a), 66f + 15 * sin(a) * 0.6f, 9f, 14f)
            }
            circleAt(p.light, 50f, 66f, 7f)
        }
        "cactus" -> {
            drawRoundRect(p.base, topLeft = Offset(38f, 30f), size = Size(24f, 54f), cornerRadius = CornerRadius(12f))
            drawRoundRect(p.base, topLeft = Offset(20f, 46f), size = Size(16f, 11f), cornerRadius = CornerRadius(6f))
            drawRoundRect(p.base, topLeft = Offset(64f, 40f), size = Size(16f, 11f), cornerRadius = CornerRadius(6f))
            circleAt(p.light, 50f, 26f, 6f)
        }
        "berry" -> {
            ellipseAt(LEAF_BASE, 40f, 60f, 16f, 14f); ellipseAt(LEAF_BASE, 62f, 56f, 15f, 13f)
            circleAt(p.base, 38f, 58f, 3.4f); circleAt(p.base, 52f, 66f, 3.4f); circleAt(p.base, 62f, 54f, 3.4f)
        }
        "bamboo" -> for (i in 0 until 3) {
            val x = 38f + i * 12f
            drawRoundRect(LEAF_BASE, topLeft = Offset(x - 4f, 20f), size = Size(8f, 64f), cornerRadius = CornerRadius(4f))
            drawRect(LEAF_DARK, topLeft = Offset(x - 4f, 36f), size = Size(8f, 3f))
            drawRect(LEAF_DARK, topLeft = Offset(x - 4f, 54f), size = Size(8f, 3f))
        }
        "vine" -> {
            drawPath(
                Path().apply { moveTo(28f, 82f); quadraticTo(50f, 60f, 34f, 44f); quadraticTo(20f, 30f, 40f, 20f) },
                LEAF_BASE, style = Stroke(4f, cap = StrokeCap.Round),
            )
            circleAt(p.base, 40f, 20f, 6f)
        }
        "tree" -> {
            drawRoundRect(hex("#B98A5D"), topLeft = Offset(45f, 50f), size = Size(10f, 34f), cornerRadius = CornerRadius(4f))
            circleAt(p.base, 40f, 38f, 16f); circleAt(p.base, 60f, 36f, 14f); circleAt(p.light, 50f, 26f, 15f)
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
            circleAt(p.base, 50f, 34f, 12f)
            drawPath(tri(50f, 22f, 45f, 8f, 55f, 14f), p.dark)
            circleAt(INK, 54f, 32f, 2f)
        }
        "qilin" -> {
            ellipseAt(p.dark, 36f, 82f, 5f, 8f); ellipseAt(p.dark, 64f, 82f, 5f, 8f)
            ellipseAt(p.base, 50f, 62f, 24f, 18f)
            drawPath(Path().apply { moveTo(30f, 40f); quadraticTo(18f, 50f, 28f, 62f) }, p.dark, style = Stroke(4f, cap = StrokeCap.Round))
            drawPath(Path().apply { moveTo(70f, 40f); quadraticTo(82f, 50f, 72f, 62f) }, p.dark, style = Stroke(4f, cap = StrokeCap.Round))
            circleAt(p.base, 50f, 38f, 16f)
            drawPath(tri(50f, 20f, 46f, 4f, 54f, 4f), p.light)
            circleAt(INK, 44f, 36f, 2.2f); circleAt(INK, 56f, 36f, 2.2f)
        }
        "dragon" -> {
            drawPath(
                Path().apply { moveTo(18f, 74f); cubicTo(28f, 42f, 50f, 62f, 46f, 40f); cubicTo(42f, 18f, 66f, 16f, 76f, 30f) },
                p.base, style = Stroke(14f, cap = StrokeCap.Round),
            )
            drawPath(tri(76f, 30f, 86f, 25f, 84f, 34f), p.dark)
            circleAt(p.light, 14f, 76f, 3.6f)
            circleAt(INK, 74f, 26f, 2.2f)
        }
        "ninetail" -> {
            ellipseAt(p.base, 46f, 62f, 18f, 16f)
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
            circleAt(INK, 40f, 34f, 2.2f); circleAt(INK, 52f, 34f, 2.2f)
        }
        "crane" -> {
            val legStroke = Stroke(2.4f, cap = StrokeCap.Round)
            drawPath(Path().apply { moveTo(50f, 84f); lineTo(48f, 97f) }, p.dark, style = legStroke)
            drawPath(Path().apply { moveTo(62f, 84f); lineTo(64f, 97f) }, p.dark, style = legStroke)
            ellipseAt(p.base, 56f, 72f, 20f, 14f)
            drawPath(Path().apply { moveTo(40f, 66f); quadraticTo(12f, 54f, 10f, 76f); quadraticTo(34f, 84f, 48f, 72f); close() }, p.base)
            drawPath(Path().apply { moveTo(56f, 70f); quadraticTo(38f, 50f, 54f, 28f) }, p.base, style = Stroke(9f, cap = StrokeCap.Round))
            circleAt(p.base, 54f, 25f, 7f)
            circleAt(EMBER, 57f, 18f, 2.6f)
            circleAt(INK, 52f, 20f, 2f)
        }
    }
}

/** Vẽ vầng hào quang phía sau ảnh loài theo cấp bậc — B/A nhẹ, S 2 vòng, SS 3 vòng dày, SSR rực lửa nhiều lớp. */
private fun DrawScope.rarityAura(rarity: String?, breathe: Float, spin: Float, spinRev: Float, flicker: Float) {
    when (rarity) {
        "SSR" -> {
            val glowBrush = Brush.radialGradient(
                colors = listOf(GOLD, FLAME.copy(alpha = 0.8f), EMBER.copy(alpha = 0f)),
                center = Offset(50f, 50f), radius = 40f,
            )
            drawCircle(brush = glowBrush, radius = 40f, center = Offset(50f, 50f), blendMode = androidx.compose.ui.graphics.BlendMode.Screen)
            val coreBrush = Brush.radialGradient(
                colors = listOf(hex("#FFF6DE"), GOLD.copy(alpha = 0f)),
                center = Offset(50f, 50f), radius = 15f,
            )
            drawCircle(brush = coreBrush, radius = 15f, center = Offset(50f, 50f), blendMode = androidx.compose.ui.graphics.BlendMode.Screen)
            val ringScale = 0.88f + breathe * (1.16f - 0.88f)
            scale(ringScale, pivot = Offset(50f, 50f)) {
                drawCircle(GOLD, radius = 39f, center = Offset(50f, 50f), alpha = 0.7f + breathe * 0.25f, style = Stroke(3.6f))
            }
            rotate(spin, pivot = Offset(50f, 50f)) {
                drawCircle(
                    FLAME, radius = 34f, center = Offset(50f, 50f), alpha = 0.85f,
                    style = Stroke(2.2f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 6f))),
                )
            }
            rotate(spinRev, pivot = Offset(50f, 50f)) {
                drawCircle(
                    EMBER, radius = 25f, center = Offset(50f, 50f), alpha = 0.7f,
                    style = Stroke(1.8f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))),
                )
            }
            val sparkColors = listOf(GOLD, FLAME, EMBER, hex("#FFF3C4"))
            val pts = listOf(26f to 22f, 74f to 26f, 70f to 74f, 22f to 70f, 50f to 14f, 50f to 86f, 12f to 46f, 88f to 54f)
            for (i in 0 until 7) {
                val (x, y) = pts[i % pts.size]
                val r = (3.4f + (i % 3) * 0.7f) * 1.3f
                scale(0.75f + flicker * 0.6f, pivot = Offset(x, y)) {
                    drawPath(tri(x, y - r, x + r * 0.4f, y, x - r * 0.4f, y), sparkColors[i % sparkColors.size], alpha = 0.55f + flicker * 0.45f)
                }
            }
        }
        "SS" -> {
            val c = RARITY_COLORS.getValue("SS")
            val ringScale = 0.92f + breathe * (1.1f - 0.92f)
            scale(ringScale, pivot = Offset(50f, 50f)) {
                drawCircle(c, radius = 40f, center = Offset(50f, 50f), alpha = 0.55f, style = Stroke(2.6f))
                drawCircle(c, radius = 33f, center = Offset(50f, 50f), alpha = 0.4f, style = Stroke(1.7f))
                drawCircle(c, radius = 27f, center = Offset(50f, 50f), alpha = 0.26f, style = Stroke(1.1f))
            }
        }
        "S" -> {
            val c = RARITY_COLORS.getValue("S")
            val ringScale = 0.96f + breathe * (1.05f - 0.96f)
            scale(ringScale, pivot = Offset(50f, 50f)) {
                drawCircle(c, radius = 39f, center = Offset(50f, 50f), alpha = 0.26f, style = Stroke(1.5f))
                drawCircle(c, radius = 32f, center = Offset(50f, 50f), alpha = 0.17f, style = Stroke(0.9f))
            }
        }
        "A", "B" -> {
            val c = RARITY_COLORS.getValue(rarity)
            val ringScale = 0.96f + breathe * (1.05f - 0.96f)
            scale(ringScale, pivot = Offset(50f, 50f)) {
                drawCircle(c, radius = 38f, center = Offset(50f, 50f), alpha = if (rarity == "A") 0.2f else 0.16f, style = Stroke(if (rarity == "A") 1.3f else 1.1f))
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
