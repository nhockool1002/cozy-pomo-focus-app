package com.cozypomo.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cozypomo.app.ui.theme.CozyError
import com.cozypomo.app.ui.theme.CozyPrimary

/** Chấm tròn xanh (API bình thường) / đỏ (mất mạng hoặc không gọi được API) — xem NetworkMonitor. */
@Composable
fun NetworkStatusDot(isOnline: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(10.dp)
            .background(color = if (isOnline) CozyPrimary else CozyError, shape = CircleShape),
    )
}
