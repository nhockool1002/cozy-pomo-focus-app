package com.cozypomo.app.ui.common

import kotlin.math.ceil
import kotlin.math.sqrt

/** Vị trí 1 loài đứng trong Khu vườn — hệ toạ độ tỉ lệ 0f..1f trong khung chứa của khu vực. */
data class GardenSlot(val x: Float, val y: Float, val rotationDeg: Float, val scale: Float)

// ---------- PRNG (mulberry32 + hash) — bản sao riêng của công thức đã dùng ở SpeciesArt.kt
// (hàm gốc `private` trong file đó, không import chéo được) — cùng thuật toán để nhất quán độ
// "tinh tế" của jitter trên toàn app, không phát minh công thức mới. ----------
private fun hashSeed(s: String): Int {
    var h = 1779033703
    for (ch in s) {
        h = (h xor ch.code) * 3432918353U.toInt()
        h = (h shl 13) or (h ushr 19)
    }
    return h
}

private fun rndFor(seed: String): Float {
    var a = hashSeed(seed)
    a += 0x6D2B79F5
    var t = (a xor (a ushr 15)) * (a or 1)
    t = (t + ((t xor (t ushr 7)) * (t or 61))) xor t
    val unsigned = (t xor (t ushr 14)).toLong() and 0xFFFFFFFFL
    return (unsigned.toDouble() / 4294967296.0).toFloat()
}

/** Số cột tối đa của lưới — chặn trần để ô không bị bóp quá hẹp khi 1 khu có nhiều loài (VD user
 * test cheat cấp nhiều loài cùng lúc); dư ra thì lưới cao thêm (nhiều hàng hơn) thay vì hẹp cột. */
private const val MAX_COLS = 5

/** Kích thước lưới (cột, hàng) theo số vật phẩm cần đặt trong 1 khu — dùng chung bởi
 * [computeGardenSlots] (định vị) và `GardenMapView` (tính chiều cao khung để KHÔNG cắt/đè vật phẩm
 * nào — xem yêu cầu "hiển thị hết + bản đồ có thể mở rộng to" trong plan.md Nhóm G). */
fun gardenGridSize(itemCount: Int): Pair<Int, Int> {
    if (itemCount <= 0) return 2 to 1
    val cols = ceil(sqrt(itemCount * 1.8f)).toInt().coerceIn(2, MAX_COLS)
    val rows = ceil(itemCount / cols.toFloat()).toInt().coerceAtLeast(1) + 1
    return cols to rows
}

/**
 * T-112 — "ngẫu nhiên nhưng tinh tế" (Nhóm G): chia khung thành lưới ô ảo thưa hơn số loài cần
 * đặt, xáo thứ tự ô bằng PRNG rồi gán mỗi `speciesId` đúng 1 ô riêng (không trùng, không cần dò va
 * chạm runtime), lệch ngẫu nhiên trong ô + góc xoay/tỉ lệ nhẹ để không trông như lưới cứng. Chỉ
 * phụ thuộc [speciesIds] (không phụ thuộc thứ tự tải dữ liệu từ mạng) — cùng 1 loài luôn đứng
 * đúng 1 chỗ mỗi lần mở lại màn.
 */
fun computeGardenSlots(speciesIds: List<String>): Map<String, GardenSlot> {
    if (speciesIds.isEmpty()) return emptyMap()
    val sorted = speciesIds.sorted()
    val (cols, rows) = gardenGridSize(sorted.size)
    val totalCells = cols * rows

    // Chừa hẳn ô góc trên-trái (index 0) — chỗ nhãn tên khu ("🌲 Khu Rừng"...) luôn neo ở đó —
    // để không có loài nào rớt trúng đè lên nhãn, che mất tên khu (rows luôn dư ≥1 hàng so với số
    // loài nên bỏ 1 ô vẫn còn đủ chỗ, xem [gardenGridSize]).
    val cellOrder = (1 until totalCells).toMutableList()
    val shuffleSeed = sorted.joinToString(",")
    for (i in cellOrder.indices.reversed()) {
        val j = (rndFor("$shuffleSeed:$i") * (i + 1)).toInt().coerceIn(0, i)
        val tmp = cellOrder[i]
        cellOrder[i] = cellOrder[j]
        cellOrder[j] = tmp
    }

    val cellW = 1f / cols
    val cellH = 1f / rows
    return sorted.mapIndexed { index, speciesId ->
        val cell = cellOrder[index % cellOrder.size]
        val cellCol = cell % cols
        val cellRow = cell / cols
        val jitterX = (rndFor("$speciesId:jx") - 0.5f) * 0.6f
        val jitterY = (rndFor("$speciesId:jy") - 0.5f) * 0.6f
        val x = ((cellCol + 0.5f + jitterX) * cellW).coerceIn(0.06f, 0.94f)
        var y = ((cellRow + 0.5f + jitterY) * cellH).coerceIn(0.1f, 0.9f)
        // Icon có kích thước cố định (không co theo cellW khi lưới dày), nên jitter ở ô kề có thể
        // trôi vào đúng góc trên-trái dù đã chừa ô 0 — chặn cứng thêm 1 lớp theo toạ độ tuyệt đối để
        // không loài nào đè lên nhãn tên khu dù rơi vào ô nào.
        if (x < 0.34f && y < 0.22f) y = 0.24f
        val rotation = (rndFor("$speciesId:rot") - 0.5f) * 16f
        val scale = 0.9f + rndFor("$speciesId:scale") * 0.25f
        speciesId to GardenSlot(x, y, rotation, scale)
    }.toMap()
}
