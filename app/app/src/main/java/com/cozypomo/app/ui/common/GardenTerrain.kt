package com.cozypomo.app.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import kotlin.math.cos
import kotlin.math.sin

/** Sông/nước trong địa hình Khu vườn (T-112) — bảng màu hiện tại (matcha/sun/terracotta/ink)
 * không có tông nào đọc ra được là "nước", nên thêm đúng 1 màu mới chỉ dùng ở đây (đã nêu ở
 * plan.md Nhóm G để Dev1002 duyệt cùng lúc với wireframe). */
private val WaterColor = Color(0xFF8FB8C4)

/** Scatter tỉ lệ vàng — rải N vị trí trong [0f,1f] đều nhau về mặt thị giác mà không cần PRNG có
 * seed riêng (đủ dùng cho đạo cụ trang trí tĩnh, không cần ổn định theo speciesId như icon loài). */
private fun goldenScatter(i: Int, offset: Float = 0f): Float = ((i + offset) * 0.61803398875f) % 1f

/**
 * T-112 — nền địa hình "isometric nhẹ" cho từng khu vực Khu vườn (Nhóm G): mảng nền bo mềm (không
 * phải khối chữ nhật cứng) + đạo cụ trang trí (cây/đá/sóng/hoa/bệ Thần thú) vẽ bằng Canvas theo
 * `category` — không đổi theo dữ liệu người dùng, chỉ đổi theo `density` (số hàng lưới của khu,
 * xem [gardenGridSize]) để khu càng cao (càng nhiều loài sở hữu) càng có nhiều đạo cụ rải đều suốt
 * chiều cao — tránh phần dưới trống trơn mất "nhận diện" là rừng/biển/vườn khi khu giãn cao (phản
 * hồi Dev1002 — xem plan.md Nhóm G). Cùng ngôn ngữ hình ảnh đã thống nhất ở
 * docs/wireframes/my-garden-feature.html.
 */
@Composable
fun GardenZoneBackground(category: String, density: Int = 1, modifier: Modifier = Modifier) {
    val matcha = MaterialTheme.colorScheme.primary
    val matchaDeep = MaterialTheme.colorScheme.tertiary
    val sun = MaterialTheme.colorScheme.secondary
    val bands = density.coerceAtLeast(1)

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Mảng nền bo mềm, tông theo khu vực — không viền hộp cứng.
        val bgTint = when (category) {
            "FOREST" -> matcha
            "SEA" -> WaterColor
            "PLANT" -> sun
            else -> sun
        }
        drawOval(
            color = bgTint.copy(alpha = 0.14f),
            topLeft = Offset(-w * 0.08f, h * 0.04f),
            size = Size(w * 1.16f, h * 0.9f),
        )

        when (category) {
            "FOREST" -> drawForestProps(w, h, bands, trunk = matcha.copy(alpha = 0.55f), crown = matchaDeep)
            "SEA" -> drawSeaProps(w, h, bands, water = WaterColor)
            "PLANT" -> drawPlantProps(w, h, bands, sun = sun, leaf = matcha)
            else -> drawMythicProps(w, h, bands, sun = sun)
        }
    }
}

private fun DrawScope.drawTree(cx: Float, cy: Float, s: Float, trunk: Color, crown: Color) {
    drawOval(color = Color.Black.copy(alpha = 0.08f), topLeft = Offset(cx - 16f * s, cy + 2f * s), size = Size(32f * s, 8f * s))
    drawRect(color = trunk, topLeft = Offset(cx - 2.5f * s, cy - 10f * s), size = Size(5f * s, 14f * s))
    val crownPath = Path().apply {
        moveTo(cx, cy - 44f * s)
        lineTo(cx - 15f * s, cy - 8f * s)
        lineTo(cx + 15f * s, cy - 8f * s)
        close()
    }
    drawPath(crownPath, color = crown)
}

private fun DrawScope.drawRock(cx: Float, cy: Float, s: Float, color: Color) {
    drawOval(color = Color.Black.copy(alpha = 0.08f), topLeft = Offset(cx - 14f * s, cy + 5f * s), size = Size(28f * s, 7f * s))
    val rockPath = Path().apply {
        moveTo(cx - 12f * s, cy)
        lineTo(cx - 2f * s, cy - 12f * s)
        lineTo(cx + 12f * s, cy - 4f * s)
        lineTo(cx + 9f * s, cy + 6f * s)
        lineTo(cx - 8f * s, cy + 7f * s)
        close()
    }
    drawPath(rockPath, color = color)
}

private fun DrawScope.drawBush(cx: Float, cy: Float, s: Float, color: Color) {
    drawCircle(color = color.copy(alpha = 0.5f), radius = 10f * s, center = Offset(cx - 7f * s, cy))
    drawCircle(color = color.copy(alpha = 0.6f), radius = 12f * s, center = Offset(cx + 5f * s, cy - 2f * s))
}

/** Cụm cây/bụi rải theo `bands` (số hàng lưới của khu — xem [gardenGridSize]) để khu càng cao càng
 * đủ cây suốt chiều dài, không chỉ dồn ở phần trên như bản gốc chiều cao cố định 230dp. */
private fun DrawScope.drawForestProps(w: Float, h: Float, bands: Int, trunk: Color, crown: Color) {
    val bandH = h / bands
    for (i in 0 until bands) {
        val yBase = bandH * i + bandH * (0.35f + 0.25f * goldenScatter(i, 0.5f))
        drawTree(w * (0.08f + 0.18f * goldenScatter(i)), yBase, 1.1f + 0.35f * goldenScatter(i, 3f), trunk, crown)
        drawTree(w * (0.55f + 0.35f * goldenScatter(i, 1.7f)), yBase + bandH * 0.22f, 0.9f + 0.3f * goldenScatter(i, 2.3f), trunk, crown)
        drawBush(w * (0.3f + 0.3f * goldenScatter(i, 4.1f)), yBase + bandH * 0.32f, 1f, crown)
        if (i % 2 == 0) drawRock(w * (0.82f + 0.12f * goldenScatter(i, 5f)), yBase + bandH * 0.36f, 0.8f, trunk.copy(alpha = 0.5f))
    }
}

/** Sóng nhỏ hình cung — dấu hiệu "biển" rõ ràng hơn ngoài đường sông, không thể nhầm với khu khác. */
private fun DrawScope.drawWaveDecal(cx: Float, cy: Float, s: Float, color: Color) {
    val path = Path().apply {
        moveTo(cx - 10f * s, cy)
        quadraticTo(cx - 5f * s, cy - 7f * s, cx, cy)
        quadraticTo(cx + 5f * s, cy + 7f * s, cx + 10f * s, cy)
    }
    drawPath(path, color = color.copy(alpha = 0.4f), style = Stroke(width = 2.5f * s, cap = StrokeCap.Round))
}

/** 1 hàng sóng biển thật (chuỗi cung lượn ngang suốt chiều rộng) — khác đường sông (1 dải hẹp uốn
 * lượn dọc khu), hàng sóng này đọc ra ngay là "mặt biển" theo chiều ngang, giống ký hiệu sóng cổ
 * điển (〜〜〜), giúp khu Vùng Biển không bị nhầm là 1 con sông đơn thuần. */
private fun DrawScope.drawWaveRow(y: Float, w: Float, amplitude: Float, waveLen: Float, color: Color, strokeWidth: Float) {
    val path = Path().apply { moveTo(-waveLen * 0.5f, y) }
    var x = -waveLen * 0.5f
    var crestUp = true
    while (x < w + waveLen) {
        val midX = x + waveLen * 0.25f
        val endX = x + waveLen * 0.5f
        path.quadraticTo(midX, y + (if (crestUp) -amplitude else amplitude), endX, y)
        x = endX
        crestUp = !crestUp
    }
    drawPath(path, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
}

private fun DrawScope.drawSeaProps(w: Float, h: Float, bands: Int, water: Color) {
    val river = Path().apply {
        moveTo(w * 0.92f, h * 0.04f)
        cubicTo(w * 0.68f, h * (0.1f + 0.06f), w * 0.78f, h * 0.3f, w * 0.5f, h * 0.36f)
        cubicTo(w * 0.24f, h * 0.42f, w * 0.3f, h * 0.62f, w * 0.1f, h * 0.7f)
        cubicTo(w * -0.02f, h * 0.75f, w * 0.06f, h * 0.9f, w * 0.02f, h * 0.98f)
    }
    drawPath(river, color = water.copy(alpha = 0.55f), style = Stroke(width = 30f, cap = StrokeCap.Round))
    drawPath(river, color = water, style = Stroke(width = 20f, cap = StrokeCap.Round))

    val bandH = h / bands
    for (i in 0 until bands) {
        val yBase = bandH * i + bandH * (0.2f + 0.5f * goldenScatter(i, 0.7f))
        // Hàng sóng ngang thật — 2 lớp (mờ+đậm) mỗi dải, xen kẽ biên độ để trông tự nhiên như mặt
        // biển gợn sóng chứ không phải 1 khối kẻ ngang lặp đều.
        drawWaveRow(bandH * i + bandH * 0.12f, w, 5f + 2f * goldenScatter(i, 6f), 46f, water.copy(alpha = 0.28f), 3f)
        drawWaveRow(bandH * i + bandH * 0.86f, w, 4f + 2f * goldenScatter(i, 7.4f), 38f, water.copy(alpha = 0.24f), 2.5f)
        drawRock(w * (0.78f + 0.16f * goldenScatter(i, 1.3f)), yBase, 0.7f, Color(0xFF9C9284))
        drawWaveDecal(w * (0.14f + 0.14f * goldenScatter(i, 2.9f)), yBase + bandH * 0.3f, 1f, water)
        drawWaveDecal(w * (0.5f + 0.2f * goldenScatter(i, 4.4f)), yBase - bandH * 0.15f, 0.8f, water)
    }
}

private fun DrawScope.drawFlowerCluster(cx: Float, cy: Float, color: Color) {
    for (i in 0 until 4) {
        val angle = i * (Math.PI / 2).toFloat()
        drawCircle(color = color, radius = 5f, center = Offset(cx + cos(angle) * 8f, cy + sin(angle) * 8f))
    }
    drawCircle(color = color.copy(alpha = 0.7f), radius = 3f, center = Offset(cx, cy))
}

private fun DrawScope.drawGrassTuft(cx: Float, cy: Float, color: Color) {
    for (i in -1..1) {
        drawLine(
            color = color.copy(alpha = 0.5f),
            start = Offset(cx + i * 4f, cy),
            end = Offset(cx + i * 4f + i * 2f, cy - 10f),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round,
        )
    }
}

/** 1 cọc rào gỗ (2 thanh ngang + cọc đứng) — vẽ dọc theo mép dưới khu để gợi "vườn được rào lại"
 * thay vì 1 bãi cỏ mở, theo đúng yêu cầu Dev1002. Chỉ rào mép dưới (mép "trước" của khu theo phối
 * cảnh isometric nhẹ đã dùng toàn app) — không rào hết viền oval vì sẽ phức tạp/rối mà không thêm
 * giá trị nhận diện (2 mép trên/dưới đã đủ gợi "khung vườn"). */
private fun DrawScope.drawFenceRow(y: Float, w: Float, color: Color) {
    val left = w * 0.03f
    val right = w * 0.97f
    drawLine(color, Offset(left, y - 11f), Offset(right, y - 11f), strokeWidth = 3f, cap = StrokeCap.Round)
    drawLine(color, Offset(left, y + 4f), Offset(right, y + 4f), strokeWidth = 3f, cap = StrokeCap.Round)
    var x = left
    while (x <= right) {
        drawLine(color, Offset(x, y - 19f), Offset(x, y + 10f), strokeWidth = 3.6f, cap = StrokeCap.Round)
        x += 27f
    }
}

private fun DrawScope.drawPlantProps(w: Float, h: Float, bands: Int, sun: Color, leaf: Color) {
    val fence = leaf.copy(alpha = 0.6f)
    // Rào mép dưới (đủ suốt bề rộng, luôn ở tận cuối khu dù khu giãn cao) + 1 đoạn ngắn góc trên-
    // phải (góc trên-trái đã có nhãn tên khu che sẵn, rào ở đó chỉ phí vẽ) — đóng khung thị giác "1
    // khoảnh vườn được rào" thay vì trải cỏ vô tận, theo đúng yêu cầu Dev1002.
    clipRect(left = w * 0.62f, top = 0f, right = w, bottom = h * 0.06f) { drawFenceRow(h * 0.02f, w, fence) }
    drawFenceRow(h * 0.98f, w, fence)

    val bandH = h / bands
    for (i in 0 until bands) {
        val yBase = bandH * i + bandH * (0.3f + 0.4f * goldenScatter(i, 0.4f))
        drawFlowerCluster(w * (0.16f + 0.22f * goldenScatter(i)), yBase, sun)
        drawFlowerCluster(w * (0.6f + 0.3f * goldenScatter(i, 1.9f)), yBase + bandH * 0.25f, sun)
        drawGrassTuft(w * (0.38f + 0.2f * goldenScatter(i, 3.2f)), yBase + bandH * 0.35f, leaf)
        drawCircle(color = leaf.copy(alpha = 0.32f), radius = 18f, center = Offset(w * (0.82f + 0.1f * goldenScatter(i, 4.6f)), yBase + bandH * 0.12f))
    }
}

private fun DrawScope.drawSparkle(cx: Float, cy: Float, s: Float, color: Color) {
    drawLine(color, Offset(cx - 6f * s, cy), Offset(cx + 6f * s, cy), strokeWidth = 1.5f * s, cap = StrokeCap.Round)
    drawLine(color, Offset(cx, cy - 6f * s), Offset(cx, cy + 6f * s), strokeWidth = 1.5f * s, cap = StrokeCap.Round)
}

/** Vàng kim riêng cho bệ Điện Thần Thú (giống tiền lệ `WaterColor`) — `sun` (secondary) dùng cho
 * hào quang/tia sáng đã đủ, nhưng bản thân mặt bệ cần 1 tông vàng đậm hơn để có chiều sâu, không
 * phẳng như 1 khối oval tô màu đặc trước đây (phản hồi Dev1002 "chưa sang trọng tinh tế"). */
private val ShrineGoldDeep = Color(0xFFC79A3A)
private val ShrineGoldLight = Color(0xFFF6E3A8)

private fun DrawScope.drawGem(cx: Float, cy: Float, s: Float, color: Color) {
    val p = Path().apply {
        moveTo(cx, cy - 4.2f * s)
        lineTo(cx + 3f * s, cy)
        lineTo(cx, cy + 4.2f * s)
        lineTo(cx - 3f * s, cy)
        close()
    }
    drawPath(p, color = Color.White.copy(alpha = 0.9f))
    drawPath(p, color = color, style = Stroke(width = 1f * s))
}

/** Cột đá/đèn lồng linh thiêng đứng chầu 2 bên bệ — gợi "điện thờ" rõ ràng hơn 1 bệ trơ trọi. */
private fun DrawScope.drawGuardianPillar(cx: Float, baseY: Float, ph: Float, color: Color) {
    drawOval(color = Color.Black.copy(alpha = 0.1f), topLeft = Offset(cx - 6f, baseY + 1f), size = Size(12f, 4f))
    drawRect(color = color.copy(alpha = 0.85f), topLeft = Offset(cx - 3.2f, baseY - ph), size = Size(6.4f, ph))
    drawRect(color = color, topLeft = Offset(cx - 6.5f, baseY - ph - 6f), size = Size(13f, 7f))
    drawRect(color = color.copy(alpha = 0.75f), topLeft = Offset(cx - 5.5f, baseY - 4.5f), size = Size(11f, 4.5f))
    drawCircle(color = Color.White.copy(alpha = 0.85f), radius = 2f, center = Offset(cx, baseY - ph - 2.5f))
}

/** Tia hào quang toả quanh bệ — yếu tố "thần linh" rõ rệt hơn hào quang tròn đơn thuần. */
private fun DrawScope.drawRadiance(cx: Float, cy: Float, innerR: Float, outerR: Float, color: Color) {
    val rayCount = 12
    for (i in 0 until rayCount) {
        val rad = Math.toRadians((360f / rayCount * i).toDouble())
        val dx = cos(rad).toFloat()
        val dy = sin(rad).toFloat()
        drawLine(
            color = color.copy(alpha = 0.2f),
            start = Offset(cx + dx * innerR, cy + dy * innerR),
            end = Offset(cx + dx * outerR, cy + dy * outerR),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round,
        )
    }
}

/** Mây linh — 3 gò tròn chồng nhẹ, trôi gần đỉnh khu tạo không khí "cõi thiêng" trên mây. */
private fun DrawScope.drawCloudWisp(cx: Float, cy: Float, s: Float, color: Color) {
    drawOval(color = color, topLeft = Offset(cx - 16f * s, cy - 4f * s), size = Size(22f * s, 9f * s))
    drawOval(color = color, topLeft = Offset(cx - 6f * s, cy - 8f * s), size = Size(16f * s, 9f * s))
    drawOval(color = color, topLeft = Offset(cx + 4f * s, cy - 3f * s), size = Size(14f * s, 7f * s))
}

private fun DrawScope.drawMythicProps(w: Float, h: Float, bands: Int, sun: Color) {
    // Bệ chính đứng trong "1 hàng" đầu tiên (bandH), không co lại khi khu giãn cao — chỉ phần tia
    // sáng lấp lánh bên dưới mới rải thêm theo `bands`.
    val bandH = h / bands.coerceAtLeast(1)
    val cx = w * 0.5f
    val cy = bandH * 0.42f

    // Mây linh trôi gần đỉnh khu (vẽ trước/dưới cùng, làm nền khí quyển) — yếu tố "thần linh".
    drawCloudWisp(w * 0.14f, bandH * 0.1f, 0.8f, sun.copy(alpha = 0.16f))
    drawCloudWisp(w * 0.84f, bandH * 0.14f, 1f, sun.copy(alpha = 0.14f))

    // Tia hào quang toả quanh bệ (dưới hào quang tròn, tạo cảm giác linh khí toả ra).
    drawRadiance(cx, cy, w * 0.1f, w * 0.3f, sun)

    // Hào quang mềm bằng gradient toả dần (thay 2 vòng tròn phẳng cũ) — mượt, không có viền cứng.
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(sun.copy(alpha = 0.32f), sun.copy(alpha = 0f)),
            center = Offset(cx, cy),
            radius = w * 0.26f,
        ),
        radius = w * 0.26f,
        center = Offset(cx, cy),
    )

    // 2 cột đá chầu 2 bên bệ — "điện thờ" rõ ràng hơn 1 bệ đơn độc.
    drawGuardianPillar(cx - w * 0.27f, cy + bandH * 0.1f, bandH * 0.34f, ShrineGoldDeep)
    drawGuardianPillar(cx + w * 0.27f, cy + bandH * 0.1f, bandH * 0.34f, ShrineGoldDeep)

    // Bóng đổ dưới bệ.
    drawOval(color = Color.Black.copy(alpha = 0.12f), topLeft = Offset(cx - w * 0.19f, cy + bandH * 0.11f), size = Size(w * 0.38f, bandH * 0.07f))

    // Viền bệ (lớp dưới, đậm hơn) tạo chiều dày — không còn là 1 khối phẳng.
    drawOval(color = ShrineGoldDeep, topLeft = Offset(cx - w * 0.205f, cy + bandH * 0.05f), size = Size(w * 0.41f, bandH * 0.12f))
    // Mặt bệ (lớp trên, gradient sáng→đậm) tạo hiệu ứng vòm nhẹ.
    drawOval(
        brush = Brush.verticalGradient(colors = listOf(ShrineGoldLight, ShrineGoldDeep)),
        topLeft = Offset(cx - w * 0.2f, cy),
        size = Size(w * 0.4f, bandH * 0.1f),
    )
    // Viền sáng mảnh (sheen) trên mép mặt bệ.
    drawOval(
        color = Color.White.copy(alpha = 0.5f),
        topLeft = Offset(cx - w * 0.15f, cy + bandH * 0.006f),
        size = Size(w * 0.3f, bandH * 0.022f),
        style = Stroke(width = 1.4f),
    )
    // 4 viên ngọc trang trí quanh viền bệ — điểm nhấn "sang trọng" thay vì mặt phẳng trơn.
    for (angleDeg in intArrayOf(0, 90, 180, 270)) {
        val rad = Math.toRadians(angleDeg.toDouble())
        val gx = cx + kotlin.math.cos(rad).toFloat() * w * 0.19f
        val gy = cy + bandH * 0.08f + kotlin.math.sin(rad).toFloat() * bandH * 0.045f
        drawGem(gx, gy, 1f, sun)
    }

    // Điện Thần Thú càng cao (nhiều Thần thú sở hữu) càng rải thêm tia sáng lấp lánh suốt khu —
    // giữ bệ chính cố định gần đỉnh, không lặp lại toàn bộ bệ (tránh trông như nhiều đền).
    for (i in 1 until bands) {
        val yBase = bandH * i + bandH * (0.3f + 0.4f * goldenScatter(i, 0.6f))
        drawSparkle(w * (0.15f + 0.7f * goldenScatter(i, 2.1f)), yBase, 1f + 0.4f * goldenScatter(i, 3.7f), sun)
        drawSparkle(w * (0.15f + 0.7f * goldenScatter(i, 4.9f)), yBase + bandH * 0.3f, 0.7f, sun.copy(alpha = 0.7f))
    }
}
