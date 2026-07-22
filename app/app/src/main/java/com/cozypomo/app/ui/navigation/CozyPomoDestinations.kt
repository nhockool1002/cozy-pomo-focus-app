package com.cozypomo.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 4 tab điều hướng chính — khớp Screen List (S-01 Home, S-04 Forest, S-05 Shop, S-06 Stats)
 * trong docs/technical-spec.md.
 */
enum class CozyPomoDestination(val route: String, val label: String, val icon: ImageVector) {
    Home(route = "home", label = "Trang chủ", icon = Icons.Filled.Home),
    Forest(route = "forest", label = "Khu rừng", icon = Icons.Filled.Park),
    Shop(route = "shop", label = "Cửa hàng", icon = Icons.Filled.Storefront),
    Stats(route = "stats", label = "Thống kê", icon = Icons.Filled.BarChart),
}
