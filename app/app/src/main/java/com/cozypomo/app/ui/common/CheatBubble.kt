package com.cozypomo.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

val CheatBubbleSize = 56.dp

/**
 * Bubble nổi kiểu "chat head" Messenger — kéo thả tự do trong phạm vi màn hình, chạm (không kéo)
 * để mở menu cheat. Render 1 lần ở gốc CozyPomoNavHost (ngoài Scaffold) nên hiện được ở MỌI tab.
 */
@Composable
fun CheatBubble(
    onClick: () -> Unit,
    maxOffsetXPx: Float,
    maxOffsetYPx: Float,
    modifier: Modifier = Modifier,
) {
    var offsetX by remember { mutableFloatStateOf(maxOffsetXPx.coerceAtLeast(0f)) }
    var offsetY by remember { mutableFloatStateOf((maxOffsetYPx * 0.35f).coerceAtLeast(0f)) }

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .size(CheatBubbleSize)
            .shadow(8.dp, CircleShape)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            // 2 pointerInput riêng biệt (khuyến nghị của Compose) thay vì tự đếm khoảng cách kéo:
            // detectDragGestures không bắn onDragStart/onDragEnd cho 1 lần chạm đứng yên (chưa vượt
            // touch slop) nên không thể tự suy ra "tap" từ đó — phải dùng detectTapGestures thật.
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() })
            }
            .pointerInput(maxOffsetXPx, maxOffsetYPx) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX = (offsetX + dragAmount.x).coerceIn(0f, maxOffsetXPx.coerceAtLeast(0f))
                    offsetY = (offsetY + dragAmount.y).coerceIn(0f, maxOffsetYPx.coerceAtLeast(0f))
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.BugReport,
            contentDescription = "Menu cheat tester",
            tint = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}
