package com.cozypomo.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 5 tab điều hướng chính — khớp Screen List (S-01 Home, S-04 Forest, S-05 Shop, S-06 Stats)
 * trong docs/technical-spec.md, cộng thêm Kho đồ (T-099, gom bình/trứng/nhạc sở hữu vào 1 màn
 * riêng thay vì rải rác trong Cài đặt/Khu rừng).
 */
enum class CozyPomoDestination(val route: String, val label: String, val icon: ImageVector) {
    Home(route = "home", label = "Trang chủ", icon = Icons.Filled.Home),
    Forest(route = "forest", label = "Khu rừng", icon = Icons.Filled.Park),
    Shop(route = "shop", label = "Cửa hàng", icon = Icons.Filled.Storefront),
    Inventory(route = "inventory", label = "Kho đồ", icon = Icons.Filled.Inventory2),
    Stats(route = "stats", label = "Thống kê", icon = Icons.Filled.BarChart),
}
