package com.cozypomo.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cozypomo.app.ui.common.CheatBubble
import com.cozypomo.app.ui.common.CheatBubbleSize
import com.cozypomo.app.ui.common.CurrencyBubble
import com.cozypomo.app.ui.common.CurrencyViewModel
import com.cozypomo.app.ui.common.MessageDialog
import com.cozypomo.app.ui.common.SessionViewModel
import com.cozypomo.app.ui.common.TesterCheatMenu
import com.cozypomo.app.ui.common.TesterCheatViewModel
import com.cozypomo.app.ui.about.AboutScreen
import com.cozypomo.app.ui.forest.ForestScreen
import com.cozypomo.app.ui.home.HomeScreen
import com.cozypomo.app.ui.inventory.InventoryScreen
import com.cozypomo.app.ui.settings.SettingsScreen
import com.cozypomo.app.ui.shop.ShopScreen
import com.cozypomo.app.ui.stats.StatsScreen

private const val SettingsRoute = "settings"
private const val AboutRoute = "about"

@Composable
fun CozyPomoNavHost(onLogout: () -> Unit) {
    val navController = rememberNavController()
    // Tạo 1 lần duy nhất, scope theo NavBackStackEntry "main" (RootNavHost) — sống suốt khi
    // chuyển giữa 4 tab bên dưới, tránh mỗi tab tự gọi GET /currency/balance riêng và bị kẹt "...".
    val currencyViewModel: CurrencyViewModel = hiltViewModel()
    val currencyState by currencyViewModel.uiState.collectAsState()
    // Cũng tạo 1 lần duy nhất — bubble cheat + menu phải hiện được ở MỌI tab (kiểu chat-head
    // Messenger), không chỉ khi đang mở Cài đặt (nơi bật/tắt nó qua 5 lần chạm "Phiên bản").
    val cheatViewModel: TesterCheatViewModel = hiltViewModel()
    val cheatState by cheatViewModel.uiState.collectAsState()
    val density = LocalDensity.current

    // Refresh token cũng hết hạn (VD lâu ngày không mở app) → TokenAuthenticator tự xoá phiên
    // khỏi DataStore → isLoggedIn chuyển false → tự điều hướng về Login, không cần người dùng
    // tự nhận ra rồi vào Cài đặt bấm Đăng xuất thủ công.
    val sessionViewModel: SessionViewModel = hiltViewModel()
    val isLoggedIn by sessionViewModel.isLoggedIn.collectAsState()
    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) onLogout()
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val bubbleSizePx = with(density) { CheatBubbleSize.toPx() }
        val maxOffsetXPx = with(density) { maxWidth.toPx() } - bubbleSizePx
        val maxOffsetYPx = with(density) { maxHeight.toPx() } - bubbleSizePx

        Scaffold(
            topBar = {
                // 1 hàng DUY NHẤT cho toàn bộ NavHost (icon Cài đặt + bubble số dư) — trước đây
                // Trang chủ tự vẽ thêm 1 hàng icon Cài đặt riêng ngay dưới hàng này, tạo khoảng
                // trống thừa giữa 2 hàng liền kề (Dev1002 phản hồi header "trống trải dư thừa").
                // Gộp lại còn 1 hàng vừa gọn hơn, vừa cho phép mở Cài đặt từ MỌI tab chứ không chỉ
                // Trang chủ. Bubble số dư vẫn chiếm không gian layout THẬT (không overlay nổi tự do)
                // để không bao giờ đè lên nội dung riêng của từng tab dù màn hình đó bố trí thế nào.
                Box(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 4.dp)) {
                    IconButton(
                        onClick = { navController.navigate(SettingsRoute) },
                        modifier = Modifier.align(Alignment.CenterStart),
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = "Cài đặt")
                    }
                    CurrencyBubble(state = currencyState, modifier = Modifier.align(Alignment.CenterEnd))
                }
            },
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                NavigationBar {
                    CozyPomoDestination.entries.forEach { destination ->
                        val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = CozyPomoDestination.Home.route,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable(CozyPomoDestination.Home.route) {
                    HomeScreen(currencyViewModel = currencyViewModel)
                }
                composable(CozyPomoDestination.Forest.route) { ForestScreen() }
                composable(CozyPomoDestination.Shop.route) { ShopScreen(currencyViewModel = currencyViewModel) }
                composable(CozyPomoDestination.Inventory.route) { InventoryScreen() }
                composable(CozyPomoDestination.Stats.route) { StatsScreen() }
                composable(SettingsRoute) {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        onLoggedOut = onLogout,
                        onOpenAbout = { navController.navigate(AboutRoute) },
                        currencyViewModel = currencyViewModel,
                        cheatViewModel = cheatViewModel,
                    )
                }
                composable(AboutRoute) {
                    AboutScreen(onBack = { navController.popBackStack() })
                }
            }
        }

        if (cheatState.isTester && cheatState.bubbleVisible) {
            CheatBubble(
                onClick = cheatViewModel::openDialog,
                maxOffsetXPx = maxOffsetXPx,
                maxOffsetYPx = maxOffsetYPx,
            )
        }
    }

    if (cheatState.isTester && cheatState.showDialog) {
        TesterCheatMenu(
            onDismiss = cheatViewModel::closeDialog,
            onGrantCoin = { cheatViewModel.cheatGrantCurrency("COIN", 1000, currencyViewModel::refresh) },
            onGrantFocusMinute = { cheatViewModel.cheatGrantCurrency("FOCUS_MINUTE", 1000, currencyViewModel::refresh) },
            onFastForwardSession = cheatViewModel::cheatFastForwardSession,
            onGrantMysteryEgg = cheatViewModel::cheatGrantMysteryEgg,
            onGrantRarity = cheatViewModel::cheatGrantSpecies,
        )
    }

    cheatState.cheatMessage?.let { message ->
        MessageDialog(message = message, onDismiss = cheatViewModel::dismissMessage)
    }
}
