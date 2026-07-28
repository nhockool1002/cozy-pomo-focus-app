package com.cozypomo.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import com.cozypomo.app.ui.common.NetworkStatusDot
import com.cozypomo.app.ui.common.NetworkStatusViewModel
import com.cozypomo.app.ui.common.SessionViewModel
import com.cozypomo.app.ui.common.TesterCheatMenu
import com.cozypomo.app.ui.common.TesterCheatViewModel
import com.cozypomo.app.ui.about.AboutScreen
import com.cozypomo.app.ui.forest.ForestScreen
import com.cozypomo.app.ui.home.HomeScreen
import com.cozypomo.app.ui.inbox.InboxScreen
import com.cozypomo.app.ui.inbox.InboxViewModel
import com.cozypomo.app.ui.inventory.InventoryScreen
import com.cozypomo.app.ui.market.MarketScreen
import com.cozypomo.app.ui.settings.SettingsScreen
import com.cozypomo.app.ui.shop.ShopScreen
import com.cozypomo.app.ui.stats.StatsScreen

private const val SettingsRoute = "settings"
private const val AboutRoute = "about"
private const val InboxRoute = "inbox"

// T-111 — Cửa hàng/Chợ rời khỏi Bottom Nav, sống ở đây như route trần (giống Settings/About) —
// mở qua ShopMarketToggleFab ở Trang chủ thay vì tab riêng.
private const val ShopRoute = "shop"
private const val MarketRoute = "market"

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
    // T-124 — cùng lý do với currencyViewModel: 1 instance duy nhất để badge số chưa đọc hiện
    // đúng ở MỌI tab, không chỉ khi đang mở Hộp thư.
    val inboxViewModel: InboxViewModel = hiltViewModel()
    val inboxState by inboxViewModel.uiState.collectAsState()
    // Chấm trạng thái mạng/API (xanh/đỏ) — cũng 1 instance duy nhất, hiện ở MỌI tab.
    val networkStatusViewModel: NetworkStatusViewModel = hiltViewModel()
    val isOnline by networkStatusViewModel.isOnline.collectAsState()
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
                        onClick = { navController.navigate(SettingsRoute) { launchSingleTop = true } },
                        modifier = Modifier.align(Alignment.CenterStart),
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = "Cài đặt")
                    }
                    IconButton(
                        onClick = { navController.navigate(InboxRoute) { launchSingleTop = true } },
                        modifier = Modifier.align(Alignment.CenterStart).padding(start = 48.dp),
                    ) {
                        BadgedBox(badge = {
                            if (inboxState.unreadCount > 0) {
                                Badge { Text(if (inboxState.unreadCount > 99) "99+" else "${inboxState.unreadCount}") }
                            }
                        }) {
                            Icon(Icons.Filled.Notifications, contentDescription = "Hộp thư")
                        }
                    }
                    CurrencyBubble(state = currencyState, modifier = Modifier.align(Alignment.CenterEnd))
                    // Góc trên-trái luôn trống (2 IconButton bên dưới đều căn CenterStart) — không
                    // đè lên Cài đặt/Hộp thư/CurrencyBubble dù màn hình xoay hay đổi mật độ điểm ảnh.
                    NetworkStatusDot(
                        isOnline = isOnline,
                        modifier = Modifier.align(Alignment.TopStart),
                    )
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
                                // KHÔNG dùng saveState/restoreState ở đây: kết hợp popUpTo(startDestination)
                                // + saveState + restoreState là 1 bug thật của Navigation-Compose khi có
                                // route ngoài 5 tab (VD "settings"/"about") từng bị pop-with-saveState — lần
                                // sau đó navigate() về đúng startDestination ("home") sẽ bị restore NHẦM sang
                                // back-stack-entry đã lưu trước đó (settings) thay vì hiện "home" (tái hiện
                                // được 100%: Cài đặt → Khu rừng → Trang chủ lại quay về Cài đặt). Bỏ
                                // saveState/restoreState tránh hẳn registry lỗi này; cái giá phải trả là mỗi
                                // lần đổi tab, ViewModel của tab đó bị tạo lại (gọi lại API) thay vì giữ
                                // nguyên — chấp nhận được vì các màn đã tự refetch khi vào lại (VD T-099).
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id)
                                    launchSingleTop = true
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
                    HomeScreen(
                        currencyViewModel = currencyViewModel,
                        onOpenShop = { navController.navigate(ShopRoute) { launchSingleTop = true } },
                        onOpenMarket = { navController.navigate(MarketRoute) { launchSingleTop = true } },
                    )
                }
                composable(CozyPomoDestination.Forest.route) { ForestScreen() }
                composable(CozyPomoDestination.Inventory.route) { InventoryScreen() }
                composable(CozyPomoDestination.Stats.route) { StatsScreen() }
                composable(ShopRoute) {
                    ShopScreen(currencyViewModel = currencyViewModel, onBack = { navController.popBackStack() })
                }
                composable(MarketRoute) {
                    MarketScreen(currencyViewModel = currencyViewModel, onBack = { navController.popBackStack() })
                }
                composable(SettingsRoute) {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        onLoggedOut = onLogout,
                        onOpenAbout = { navController.navigate(AboutRoute) { launchSingleTop = true } },
                        currencyViewModel = currencyViewModel,
                        cheatViewModel = cheatViewModel,
                    )
                }
                composable(AboutRoute) {
                    AboutScreen(onBack = { navController.popBackStack() })
                }
                composable(InboxRoute) {
                    InboxScreen(onBack = { navController.popBackStack() }, viewModel = inboxViewModel)
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
            onGrantEgg = cheatViewModel::cheatGrantEgg,
            onGrantRarity = cheatViewModel::cheatGrantSpecies,
        )
    }

    cheatState.cheatMessage?.let { message ->
        MessageDialog(message = message, onDismiss = cheatViewModel::dismissMessage)
    }
}
