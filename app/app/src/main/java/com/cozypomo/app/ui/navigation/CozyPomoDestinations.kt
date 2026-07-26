package com.cozypomo.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Park
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 4 tab điều hướng chính — Cửa hàng/Chợ đã rời khỏi Bottom Nav (T-111, bản sửa lần 2 theo yêu cầu
 * Dev1002) — vào 2 màn đó qua nút nổi [com.cozypomo.app.ui.common.ShopMarketToggleFab] ở Trang chủ
 * thay vì tab riêng, giống cách Cài đặt/Giới thiệu đã sống ngoài Bottom Nav từ trước.
 */
enum class CozyPomoDestination(val route: String, val label: String, val icon: ImageVector) {
    Home(route = "home", label = "Trang chủ", icon = Icons.Filled.Home),
    Forest(route = "forest", label = "Khu rừng", icon = Icons.Filled.Park),
    Inventory(route = "inventory", label = "Kho đồ", icon = Icons.Filled.Inventory2),
    Stats(route = "stats", label = "Thống kê", icon = Icons.Filled.BarChart),
}
