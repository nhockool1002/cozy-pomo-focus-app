package com.cozypomo.app.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Biểu tượng "bình ấp trứng" dùng chung cho Splash/Onboarding/Đăng nhập — vẽ vector đơn giản
 * thay placeholder, vì bộ asset minh hoạ thật (T-021/T-022) mới chỉ có icon app, chưa có
 * illustration riêng cho từng màn hình.
 */
@Composable
fun JarMark(modifier: Modifier = Modifier, size: Dp = 96.dp) {
    val ink = MaterialTheme.colorScheme.onBackground
    val primary = MaterialTheme.colorScheme.primary
    val accent = MaterialTheme.colorScheme.secondary

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

        // Trứng
        drawOval(
            color = accent,
            topLeft = Offset(w * 0.40f, h * 0.54f),
            size = Size(w * 0.20f, h * 0.26f),
        )
        drawOval(
            color = ink,
            topLeft = Offset(w * 0.40f, h * 0.54f),
            size = Size(w * 0.20f, h * 0.26f),
            style = Stroke(width = w * 0.02f),
        )
    }
}
