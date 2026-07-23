package com.cozypomo.app.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Cặp số dư Xu Lá/Giờ tích luỹ — dùng chung 1 chỗ (Xem [CurrencyViewModel]) để hiển thị nhất
 * quán trên mọi tab, không bị kẹt ở "..." vì mỗi màn tự gọi API riêng.
 */
@Composable
fun CurrencyBubble(state: CurrencyUiState, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
        BalancePill(
            value = state.coinBalance,
            icon = Icons.Filled.Eco,
            color = MaterialTheme.colorScheme.secondary,
            tooltip = "Xu Lá — tiêu ở Cửa hàng để mua trứng, bình, nhạc nền",
        )
        BalancePill(
            value = state.focusMinutesBalance,
            icon = Icons.Filled.HourglassBottom,
            color = MaterialTheme.colorScheme.primary,
            suffix = "p",
            tooltip = "Giờ tích luỹ — tích từ mỗi phút tập trung, dùng mua trứng theo giờ thay vì Xu Lá",
        )
    }
}

/** Chạm vào để hiện tooltip giải thích chỉ số. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalancePill(
    value: Int?,
    icon: ImageVector,
    color: Color,
    tooltip: String,
    suffix: String = "",
    compact: Boolean = false,
) {
    val tooltipState = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(tooltip) } },
        state = tooltipState,
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.25f),
            onClick = { scope.launch { tooltipState.show() } },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = if (compact) 10.dp else 14.dp, vertical = if (compact) 6.dp else 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(if (compact) 14.dp else 16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = (value?.toString() ?: "…") + suffix,
                    style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
