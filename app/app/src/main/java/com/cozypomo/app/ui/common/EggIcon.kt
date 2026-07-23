package com.cozypomo.app.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Parse `colorHex` từ EggType (VD "#A8D08D") — trả về xám nếu chuỗi không hợp lệ. */
fun parseEggColor(hex: String): Color = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrDefault(Color.Gray)

/** Hình quả trứng đơn giản tô theo `colorHex` của EggType — dùng ở popup chọn trứng. */
@Composable
fun EggIcon(color: Color, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawOval(color = color, topLeft = Offset(w * 0.12f, h * 0.04f), size = Size(w * 0.76f, h * 0.92f))
        drawOval(
            color = Color.White.copy(alpha = 0.4f),
            topLeft = Offset(w * 0.32f, h * 0.18f),
            size = Size(w * 0.2f, h * 0.26f),
        )
    }
}
