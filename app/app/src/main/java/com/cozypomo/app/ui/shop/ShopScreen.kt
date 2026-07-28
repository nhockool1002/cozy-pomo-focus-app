package com.cozypomo.app.ui.shop

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.cozypomo.app.data.network.ShopItemDto
import com.cozypomo.app.ui.common.CurrencyViewModel
import com.cozypomo.app.ui.common.EggIcon
import com.cozypomo.app.ui.common.FloatingIcon
import com.cozypomo.app.ui.common.JarMark
import com.cozypomo.app.ui.common.MessageDialog
import com.cozypomo.app.ui.common.jarMaterialFor
import com.cozypomo.app.ui.common.jarTintFor
import com.cozypomo.app.ui.common.parseEggColor
import com.cozypomo.app.ui.home.RadianceRays
import com.cozypomo.app.ui.home.SparkleOrbit

/** Cấp bậc hiếm của trứng — suy từ tên/`purchasable` thay vì backend thêm cột riêng (đúng tinh
 * thần [com.cozypomo.app.ui.common.jarMaterialFor] đã làm với chất liệu bình). Quyết định độ
 * "hào quang" quanh icon trứng trong danh sách: THƯỜNG không có, BÍ ẨN có vòng sáng vừa (giữ
 * nguyên như cũ), TRUYỀN THUYẾT có hào quang mạnh hơn hẳn — 2 vòng tia + lớp sáng nhấp nháy +
 * lấp lánh kép, để phân biệt rõ ràng "đẳng cấp" giữa 2 tier (T-125, Dev1002 yêu cầu 2026-07-28). */
enum class EggTier { COMMON, MYSTERY, LEGENDARY }

fun eggTierFor(item: ShopItemDto): EggTier = when {
    !item.purchasable -> EggTier.LEGENDARY
    item.eggType?.name?.contains("Bí Ẩn", ignoreCase = true) == true -> EggTier.MYSTERY
    else -> EggTier.COMMON
}

/** T-037 — S-05 Cửa hàng: Trứng mới / Bình thuỷ tinh / Nhạc nền, item có hiệu ứng bồng bềnh nhẹ.
 * Số dư hiện qua bubble nổi dùng chung [CurrencyViewModel] (xem CozyPomoNavHost). Từ T-111, màn
 * này không còn là tab Bottom Nav — vào qua [com.cozypomo.app.ui.common.ShopMarketToggleFab] ở
 * Trang chủ, [onBack] đóng vai trò nút quay lại trên TopAppBar (giống Cài đặt/Giới thiệu). Chạm
 * vào 1 hàng bất kỳ mở modal xem chi tiết đầy đủ ([ShopItemDetailDialog]), tách biệt khỏi việc mua.
 * Vật phẩm EGG mua thẳng bằng 1 trong 2 nút icon Xu Lá/Giờ tích luỹ (T-125) — không còn dialog
 * "Trả bằng gì?" trung gian như trước. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(currencyViewModel: CurrencyViewModel, onBack: () -> Unit, viewModel: ShopViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val currencyState by currencyViewModel.uiState.collectAsState()

    // Số dư đổi sau khi mua — báo cho bubble dùng chung tải lại.
    LaunchedEffect(uiState.lastMessage) {
        if (uiState.lastMessage != null) currencyViewModel.refresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Tiệm Tạp Hóa Rừng Xanh",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
            )
        },
    ) { innerPadding ->
    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ShopCategoryTab.entries.forEach { tab ->
                    FilterChip(
                        selected = uiState.category == tab,
                        onClick = { viewModel.selectCategory(tab) },
                        label = { Text(tab.label) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(uiState.items, key = { it.id }) { item ->
                        ShopItemRow(
                            item = item,
                            owned = uiState.ownedShopItemIds.contains(item.id),
                            coinBalance = currencyState.coinBalance,
                            focusMinutesBalance = currencyState.focusMinutesBalance,
                            onBuy = { viewModel.requestPurchase(item) },
                            onBuyEggWith = { payWith -> viewModel.buyEggWith(item, payWith) },
                            onOpenDetail = { viewModel.showDetail(item) },
                        )
                    }
                }
            }
        }
    }
    } // Scaffold content

    uiState.lastMessage?.let { message ->
        MessageDialog(message = message, onDismiss = viewModel::dismissMessage)
    }

    uiState.detailFor?.let { item ->
        ShopItemDetailDialog(
            item = item,
            owned = uiState.ownedShopItemIds.contains(item.id),
            coinBalance = currencyState.coinBalance,
            focusMinutesBalance = currencyState.focusMinutesBalance,
            onBuy = { viewModel.requestPurchaseFromDetail(item) },
            onBuyEggWith = { payWith -> viewModel.buyEggWithFromDetail(item, payWith) },
            onDismiss = viewModel::dismissDetail,
        )
    }
}

@Composable
private fun ShopItemIcon(item: ShopItemDto, size: Dp = 40.dp) {
    when (item.category) {
        "EGG" -> EggShopIcon(item = item, size = size)
        "JAR_SKIN" -> JarMark(size = size, eggColor = null, jarTint = jarTintFor(item.name), material = jarMaterialFor(item.name))
        "BOOST" -> Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest, modifier = Modifier.size(size)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    if (item.boostType == "HATCH_MINUTES") Icons.Filled.HourglassBottom else Icons.Filled.Bolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
        else -> Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest, modifier = Modifier.size(size)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

@Composable
private fun EggShopIcon(item: ShopItemDto, size: Dp) {
    val tier = eggTierFor(item)
    val color = parseEggColor(item.eggType?.colorHex ?: "#9CB380")
    if (tier == EggTier.COMMON) {
        Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
            EggIcon(color = color, size = size * 0.7f)
        }
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "eggAura")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(if (tier == EggTier.LEGENDARY) 3200 else 6000, easing = LinearEasing)),
        label = "eggAuraRotation",
    )
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        if (tier == EggTier.MYSTERY) {
            // Giữ nguyên như cũ — vòng tia đơn theo màu trứng, không lấp lánh.
            RadianceRays(sizeDp = size, rayCount = 9, color = color, rotationDeg = rotation)
        } else {
            val pulse by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.16f,
                animationSpec = infiniteRepeatable(tween(750, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "eggAuraPulse",
            )
            val auraColor = MaterialTheme.colorScheme.secondary
            Box(
                modifier = Modifier
                    .size(size * 0.85f)
                    .graphicsLayer { scaleX = pulse; scaleY = pulse }
                    .background(auraColor.copy(alpha = 0.28f), CircleShape),
            )
            RadianceRays(sizeDp = size, rayCount = 22, color = auraColor, rotationDeg = rotation)
            RadianceRays(sizeDp = size * 0.78f, rayCount = 14, color = auraColor.copy(alpha = 0.65f), rotationDeg = -rotation * 0.6f)
            SparkleOrbit(sizeDp = size * 0.9f, color = auraColor, rotationDeg = -rotation * 1.6f)
            SparkleOrbit(sizeDp = size * 0.68f, color = Color.White, rotationDeg = rotation * 1.2f)
        }
        EggIcon(color = color, size = size * 0.62f)
    }
}

@Composable
private fun ShopItemRow(
    item: ShopItemDto,
    owned: Boolean,
    coinBalance: Int?,
    focusMinutesBalance: Int?,
    onBuy: () -> Unit,
    onBuyEggWith: (String) -> Unit,
    onOpenDetail: () -> Unit,
) {
    Surface(
        onClick = onOpenDetail,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            FloatingIcon { ShopItemIcon(item = item) }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (item.category == "EGG") {
                    // Trứng (kể cả Truyền Thuyết, nay đã có mô tả thật) luôn hiện ~2 dòng câu
                    // chuyện, cắt "..." nếu dài hơn — giá không còn hiện ở đây (đã dời sang 2 nút
                    // icon Xu Lá/Giờ tích luỹ bên phải).
                    item.description?.let {
                        Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                } else if (item.purchasable) {
                    item.description?.let {
                        Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text("${item.priceCoin} Xu", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            when {
                !item.purchasable -> {} // để trống — không hiện nút, không mua được (không phải "Không bán")
                owned -> OutlinedButton(onClick = {}, enabled = false) { Text("Đã sở hữu") }
                item.category == "EGG" -> EggCurrencyButtons(
                    item = item,
                    coinBalance = coinBalance,
                    focusMinutesBalance = focusMinutesBalance,
                    onBuyWith = onBuyEggWith,
                )
                coinBalance == null || coinBalance < item.priceCoin ->
                    Button(onClick = {}, enabled = false) { Text("Cần thêm Xu") }
                else -> Button(onClick = onBuy, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Text("Mua ngay")
                }
            }
        }
    }
}

/** 2 nút mua trứng nhỏ gọn theo icon tiền tệ (Xu Lá/Giờ tích luỹ) — thay cho 1 nút "Mua ngay" +
 * dòng giá chữ vàng trước đây (T-125). Chạm nút nào mua ngay bằng loại tiền đó, không qua dialog. */
@Composable
private fun EggCurrencyButtons(item: ShopItemDto, coinBalance: Int?, focusMinutesBalance: Int?, onBuyWith: (String) -> Unit) {
    val priceHours = item.eggType?.priceHours ?: 0
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        MiniCurrencyButton(
            icon = Icons.Filled.Eco,
            label = "${item.priceCoin}",
            enabled = coinBalance == null || coinBalance >= item.priceCoin,
            onClick = { onBuyWith("COIN") },
        )
        MiniCurrencyButton(
            icon = Icons.Filled.HourglassBottom,
            label = "${priceHours}p",
            enabled = focusMinutesBalance == null || focusMinutesBalance >= priceHours,
            onClick = { onBuyWith("FOCUS_MINUTE") },
        )
    }
}

@Composable
private fun MiniCurrencyButton(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(50),
        color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

@Composable
private fun LargeCurrencyButton(modifier: Modifier = Modifier, icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Modal xem chi tiết 1 vật phẩm — chạm vào hàng bất kỳ trong danh sách mở ra, có thể mua thẳng
 * từ đây (trừ vật phẩm không bán, hiện lời giải thích thay vì nút mua). */
@Composable
private fun ShopItemDetailDialog(
    item: ShopItemDto,
    owned: Boolean,
    coinBalance: Int?,
    focusMinutesBalance: Int?,
    onBuy: () -> Unit,
    onBuyEggWith: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp, shadowElevation = 8.dp) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Đóng") }
                }
                ShopItemIcon(item = item, size = 96.dp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(item.name, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)

                if (item.category == "EGG") {
                    val tier = eggTierFor(item)
                    val tierLabel = when (tier) {
                        EggTier.LEGENDARY -> "Huyền thoại"
                        EggTier.MYSTERY -> "Bí ẩn"
                        EggTier.COMMON -> "Thường"
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)) {
                        Text(
                            tierLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }
                    item.description?.let {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                    item.eggType?.let { egg ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Thời gian ấp: ${egg.hatchDurationMin} phút",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    item.description?.let {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                when {
                    !item.purchasable -> Text(
                        "Vật phẩm đặc biệt — chỉ nhận được qua sự kiện hoặc phần thưởng, không bán ở Cửa hàng.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    owned -> OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("Đã sở hữu") }
                    item.category == "EGG" -> {
                        val priceHours = item.eggType?.priceHours ?: 0
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            LargeCurrencyButton(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Filled.Eco,
                                label = "${item.priceCoin} Xu",
                                enabled = coinBalance == null || coinBalance >= item.priceCoin,
                                onClick = { onBuyEggWith("COIN") },
                            )
                            LargeCurrencyButton(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Filled.HourglassBottom,
                                label = "$priceHours phút",
                                enabled = focusMinutesBalance == null || focusMinutesBalance >= priceHours,
                                onClick = { onBuyEggWith("FOCUS_MINUTE") },
                            )
                        }
                    }
                    else -> {
                        Text(
                            "${item.priceCoin} Xu Lá",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onBuy,
                            enabled = coinBalance == null || coinBalance >= item.priceCoin,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        ) { Text("Mua ngay") }
                    }
                }
            }
        }
    }
}
