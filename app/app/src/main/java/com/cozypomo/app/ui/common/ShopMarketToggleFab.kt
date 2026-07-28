package com.cozypomo.app.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * T-111 — nút nổi (FAB) "Cửa hàng/Chợ" ở góc phải-dưới Trang chủ (ngay trên Bottom Nav), thay cho
 * việc có tab Bottom Nav riêng cho 2 màn này (đã bỏ, xem [com.cozypomo.app.ui.navigation.CozyPomoDestination]).
 * Bấm nút tròn chính bung dọc lên 2 lựa chọn kèm nhãn chữ — Cửa hàng (`Storefront`, màu chủ đạo,
 * "sạp chính thức") và Chợ (`Handshake`, màu nhấn, "trao đổi giữa người chơi") — 2 icon khác nhau
 * rõ dù cùng cỡ nhỏ. Nút chính dùng màu accent vàng (`secondary`, nổi bật hơn `primaryContainer`
 * cũ) + vòng hào quang thở nhẹ liên tục khi đang thu gọn để thu hút chú ý — tự tắt hào quang lúc
 * bung ra (đỡ rối mắt khi người dùng đang chọn Cửa hàng/Chợ). Animation nảy nhẹ tái dùng easing đã
 * quen thuộc trong app, không phát minh mới.
 */
@Composable
fun ShopMarketToggleFab(onOpenShop: () -> Unit, onOpenMarket: () -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val enterAnim = fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 2 } + scaleIn(tween(220))
    val exitAnim = fadeOut(tween(140)) + slideOutVertically(tween(180)) { it / 2 } + scaleOut(tween(180))

    val attention = rememberInfiniteTransition(label = "fabAttention")
    val breathe by attention.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "fabBreathe",
    )
    val glowAlpha by attention.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "fabGlow",
    )

    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        AnimatedVisibility(visible = expanded, enter = enterAnim, exit = exitAnim) {
            SubFabRow(label = "Chợ", icon = Icons.Filled.Handshake, containerColor = MaterialTheme.colorScheme.tertiary) {
                expanded = false
                onOpenMarket()
            }
        }
        if (expanded) Spacer(modifier = Modifier.height(10.dp))
        AnimatedVisibility(visible = expanded, enter = enterAnim, exit = exitAnim) {
            SubFabRow(label = "Cửa hàng", icon = Icons.Filled.Storefront, containerColor = MaterialTheme.colorScheme.primary) {
                expanded = false
                onOpenShop()
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Box(contentAlignment = Alignment.Center) {
            if (!expanded) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .graphicsLayer { scaleX = breathe; scaleY = breathe }
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = glowAlpha), CircleShape),
                )
            }
            FloatingActionButton(
                onClick = { expanded = !expanded },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                shape = CircleShape,
                modifier = if (!expanded) Modifier.graphicsLayer { scaleX = breathe; scaleY = breathe } else Modifier,
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Filled.Close else Icons.Filled.ShoppingBasket,
                    contentDescription = if (expanded) "Đóng" else "Cửa hàng/Chợ",
                )
            }
        }
    }
}

@Composable
private fun SubFabRow(label: String, icon: ImageVector, containerColor: Color, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest, tonalElevation = 2.dp) {
            Text(label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge)
        }
        SmallFloatingActionButton(onClick = onClick, containerColor = containerColor) {
            Icon(icon, contentDescription = label)
        }
    }
}
