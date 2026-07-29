package com.cozypomo.app.ui.shop

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlin.math.cos
import kotlin.math.sin

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

private val LEGENDARY_GOLD = Color(0xFFF4D160)

/** Pha sáng 1 màu (amt trong 0..1) — port 1:1 công thức `shade()` bên `egg-art.ts` (Admin), giữ
 * đúng cùng phép tính để 2 nền tảng ra cùng 1 sắc "glow" từ cùng 1 `colorHex`. */
private fun lightenColor(color: Color, amt: Float): Color {
    fun mix(v: Float) = v + (1f - v) * amt
    return Color(mix(color.red), mix(color.green), mix(color.blue), color.alpha)
}

/** Cấp bậc vật phẩm bổ trợ (T-128, port 1:1 từ `item-art.ts#boostTierFor` — xem comment ở đó) —
 * quyết định số tia sét quanh Túi Thời Gian + có hào quang/lấp lánh hay không. */
enum class BoostTier { SMALL, MEDIUM, LARGE, GIANT, LEGENDARY }

fun boostTierFor(item: ShopItemDto): BoostTier = when {
    !item.purchasable -> BoostTier.LEGENDARY
    item.boostType == "HATCH_MINUTES" -> BoostTier.MEDIUM
    (item.boostAmount ?: 0) <= 10 -> BoostTier.SMALL
    (item.boostAmount ?: 0) <= 20 -> BoostTier.MEDIUM
    (item.boostAmount ?: 0) <= 50 -> BoostTier.LARGE
    else -> BoostTier.GIANT
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
        "BOOST" -> BoostShopIcon(item = item, size = size)
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
            // LEGENDARY (T-128, Dev1002 yêu cầu tối hơn/cổ hơn/mãn nhãn hơn) — hào quang lấy màu
            // TỪ CHÍNH `colorHex` của trứng (pha sáng ra `glow`), không dùng `colorScheme.secondary`
            // cố định như trước (mọi Trứng Truyền Thuyết phát sáng cùng 1 màu vàng nhạt — không
            // "cổ", không phân biệt được loại nào với loại nào). Port 1:1 với
            // `egg-art.ts#legendaryEggAura` (quầng glow mix-blend + lõi sáng + 2 lớp tia xoay ngược
            // chiều + sparkle), chỉ khác công cụ vẽ do khác nền tảng (SVG dash-ring ↔ RadianceRays).
            val glow = lightenColor(color, 0.45f)
            val pulse by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.16f,
                animationSpec = infiniteRepeatable(tween(750, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "eggAuraPulse",
            )
            Canvas(modifier = Modifier.size(size * 0.95f)) {
                drawCircle(
                    brush = Brush.radialGradient(listOf(glow.copy(alpha = 0.85f), glow.copy(alpha = 0f)), radius = this.size.width * 0.46f),
                    radius = this.size.width * 0.46f,
                    blendMode = BlendMode.Screen,
                )
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color(0xFFFFFDF0), glow.copy(alpha = 0f)), radius = this.size.width * 0.17f),
                    radius = this.size.width * 0.17f,
                    blendMode = BlendMode.Screen,
                )
            }
            Box(
                modifier = Modifier
                    .size(size * 0.85f)
                    .graphicsLayer { scaleX = pulse; scaleY = pulse }
                    .background(glow.copy(alpha = 0.22f), CircleShape),
            )
            RadianceRays(sizeDp = size, rayCount = 22, color = glow, rotationDeg = rotation)
            RadianceRays(sizeDp = size * 0.78f, rayCount = 14, color = LEGENDARY_GOLD.copy(alpha = 0.75f), rotationDeg = -rotation * 0.6f)
            SparkleOrbit(sizeDp = size * 0.9f, color = LEGENDARY_GOLD, rotationDeg = -rotation * 1.6f)
            SparkleOrbit(sizeDp = size * 0.68f, color = glow, rotationDeg = rotation * 1.2f)
        }
        EggIcon(color = color, size = size * 0.62f)
    }
}

private val BOOST_AMBER = Color(0xFFF4B942)
private val BOOST_AMBER_DARK = Color(0xFFB9761F)
private val BOOST_AMBER_LIGHT = Color(0xFFFFE9B8)
private val BOOST_MOON_BASE = Color(0xFF8FA8E8)
private val BOOST_MOON_DARK = Color(0xFF4F5FA8)
private val BOOST_DEW_BASE = Color(0xFF6FD8D0)
private val BOOST_DEW_LIGHT = Color(0xFFE0FFFC)
private val BOOST_GOLD = Color(0xFFF4D160)

private val BOOST_BOLT_COUNT = mapOf(
    BoostTier.SMALL to 1, BoostTier.MEDIUM to 2, BoostTier.LARGE to 3, BoostTier.GIANT to 4, BoostTier.LEGENDARY to 4,
)

/** Hoạ tiết sparkle (✦) 4 cánh — bản sao riêng của `SpeciesArt.kt#sparkleMark` (private ở đó,
 * không export được) chỉ dùng cho [MoonBody]. */
private fun sparkleMark(cx: Float, cy: Float, r: Float): Path {
    val r2 = r * 0.35f
    return Path().apply {
        moveTo(cx, cy - r); lineTo(cx + r2, cy - r2); lineTo(cx + r, cy); lineTo(cx + r2, cy + r2)
        lineTo(cx, cy + r); lineTo(cx - r2, cy + r2); lineTo(cx - r, cy); lineTo(cx - r2, cy - r2)
        close()
    }
}

/** Icon vật phẩm bổ trợ (T-128) — port 1:1 từ `item-art.ts` (xem comment ở đó cho ý nghĩa 2 chủ
 * đề Túi Thời Gian/Giọt Sương-Ánh Trăng). Toạ độ hình vẽ giữ nguyên hệ 0..100 như bản SVG gốc để
 * khớp chính xác bản đã duyệt qua mockup, chỉ đổi từ `<path>` tĩnh sang Canvas + animation
 * (bồng bềnh có sẵn qua [FloatingIcon] ở nơi gọi, thêm mạch đập/tia sét/xoay riêng ở đây). */
@Composable
fun BoostShopIcon(item: ShopItemDto, size: Dp) {
    val tier = boostTierFor(item)
    val infiniteTransition = rememberInfiniteTransition(label = "boostAura")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(850, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "boostPulse",
    )
    val flicker by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(650, easing = LinearEasing), RepeatMode.Reverse),
        label = "boostFlicker",
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(if (tier == BoostTier.LEGENDARY) 4000 else 7000, easing = LinearEasing)),
        label = "boostRotation",
    )
    val isHatch = item.boostType == "HATCH_MINUTES"
    val ringColor = when {
        tier == BoostTier.LEGENDARY -> BOOST_GOLD
        isHatch -> BOOST_DEW_BASE
        else -> BOOST_AMBER
    }

    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(size * 0.85f)
                .graphicsLayer { scaleX = pulse; scaleY = pulse }
                .background(ringColor.copy(alpha = 0.25f), CircleShape),
        )
        if (tier == BoostTier.GIANT || tier == BoostTier.LEGENDARY) {
            RadianceRays(sizeDp = size, rayCount = 16, color = ringColor, rotationDeg = rotation)
        }
        if (tier == BoostTier.LEGENDARY) {
            SparkleOrbit(sizeDp = size * 0.92f, color = BOOST_GOLD, rotationDeg = -rotation * 1.4f)
        }
        if (isHatch) {
            if (tier == BoostTier.LEGENDARY) MoonBody(size = size * 0.66f) else DewBody(size = size * 0.62f)
        } else {
            PouchBody(size = size * 0.66f, boltCount = BOOST_BOLT_COUNT.getValue(tier), flicker = flicker)
        }
    }
}

/** Túi rút dây phát sáng vàng hổ phách + tia sét bên trong, tia sét quanh túi tăng theo tier. */
@Composable
private fun PouchBody(size: Dp, boltCount: Int, flicker: Float) {
    Canvas(modifier = Modifier.size(size)) {
        val u = this.size.width / 100f
        val body = Path().apply {
            moveTo(28 * u, 50 * u)
            cubicTo(28 * u, 34 * u, 38 * u, 26 * u, 50 * u, 26 * u)
            cubicTo(62 * u, 26 * u, 72 * u, 34 * u, 72 * u, 50 * u)
            lineTo(70 * u, 82 * u)
            cubicTo(70 * u, 90 * u, 61 * u, 94 * u, 50 * u, 94 * u)
            cubicTo(39 * u, 94 * u, 30 * u, 90 * u, 30 * u, 82 * u)
            close()
        }
        drawPath(body, BOOST_AMBER)
        drawPath(
            Path().apply { moveTo(32 * u, 46 * u); quadraticTo(50 * u, 30 * u, 68 * u, 46 * u) },
            BOOST_AMBER_DARK,
            style = Stroke(4 * u, cap = StrokeCap.Round),
        )
        drawCircle(BOOST_AMBER_DARK, radius = 3 * u, center = Offset(40 * u, 42 * u))
        drawCircle(BOOST_AMBER_DARK, radius = 3 * u, center = Offset(60 * u, 42 * u))
        drawOval(BOOST_AMBER_LIGHT, topLeft = Offset(32 * u, 46 * u), size = Size(18 * u, 26 * u), alpha = 0.65f)
        drawPath(
            Path().apply {
                moveTo(50 * u, 48 * u); lineTo(44 * u, 62 * u); lineTo(49 * u, 62 * u)
                lineTo(45 * u, 76 * u); lineTo(58 * u, 58 * u); lineTo(51 * u, 58 * u); close()
            },
            BOOST_AMBER_DARK,
        )
        for (i in 0 until boltCount) {
            val angle = (360f / boltCount) * i + 20f
            val rad = Math.toRadians(angle.toDouble())
            val bx = 50 * u + 42 * u * cos(rad).toFloat()
            val by = 55 * u + 42 * u * sin(rad).toFloat()
            drawPath(
                Path().apply {
                    moveTo(bx, by - 6 * u); lineTo(bx - 3 * u, by + 1 * u); lineTo(bx, by + 1 * u)
                    lineTo(bx - 3 * u, by + 8 * u); lineTo(bx + 4 * u, by - 2 * u); lineTo(bx + 0.5f * u, by - 2 * u); close()
                },
                BOOST_AMBER_DARK,
                alpha = flicker,
            )
        }
    }
}

/** Giọt sương đơn giản, tông xanh ngọc dịu. */
@Composable
private fun DewBody(size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val u = this.size.width / 100f
        val path = Path().apply {
            moveTo(50 * u, 18 * u)
            cubicTo(64 * u, 40 * u, 74 * u, 56 * u, 74 * u, 68 * u)
            cubicTo(74 * u, 84 * u, 63 * u, 94 * u, 50 * u, 94 * u)
            cubicTo(37 * u, 94 * u, 26 * u, 84 * u, 26 * u, 68 * u)
            cubicTo(26 * u, 56 * u, 36 * u, 40 * u, 50 * u, 18 * u)
            close()
        }
        drawPath(path, BOOST_DEW_BASE)
        drawOval(BOOST_DEW_LIGHT, topLeft = Offset(33 * u, 50 * u), size = Size(16 * u, 24 * u), alpha = 0.7f)
    }
}

/** Ánh Trăng Ấp Trứng — trăng lưỡi liềm vàng kim + sao nhỏ, tông "huyền thoại" khác hẳn Giọt
 * Sương để phân biệt rõ ngay từ hình dạng, không chỉ qua hào quang. */
@Composable
private fun MoonBody(size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val u = this.size.width / 100f
        val crescent = Path().apply {
            moveTo(62 * u, 24 * u)
            cubicTo(46 * u, 24 * u, 33 * u, 38 * u, 33 * u, 55 * u)
            cubicTo(33 * u, 72 * u, 46 * u, 86 * u, 62 * u, 86 * u)
            cubicTo(68 * u, 86 * u, 74 * u, 84 * u, 78 * u, 81 * u)
            cubicTo(68 * u, 80 * u, 58 * u, 68 * u, 58 * u, 55 * u)
            cubicTo(58 * u, 42 * u, 68 * u, 30 * u, 78 * u, 29 * u)
            cubicTo(74 * u, 26 * u, 68 * u, 24 * u, 62 * u, 24 * u)
            close()
        }
        drawPath(crescent, BOOST_MOON_BASE)
        drawPath(crescent, BOOST_MOON_DARK, alpha = 0.28f)
        drawPath(sparkleMark(28 * u, 42 * u, 4.5f * u), BOOST_GOLD)
        drawPath(sparkleMark(74 * u, 65 * u, 3.5f * u), BOOST_GOLD)
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
                    // Giá không còn hiện ở đây — dời sang nút pill Xu Lá bên phải, đồng nhất với
                    // Trứng mới thay vì dòng giá chữ + nút "Mua ngay" to bản trước đây (T-126).
                    item.description?.let {
                        Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            when {
                !item.purchasable -> {} // để trống — không hiện nút, không mua được (không phải "Không bán")
                owned -> {
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(onClick = {}, enabled = false) { Text("Đã sở hữu") }
                }
                item.category == "EGG" -> {
                    Spacer(modifier = Modifier.width(8.dp))
                    EggCurrencyButtons(
                        item = item,
                        coinBalance = coinBalance,
                        focusMinutesBalance = focusMinutesBalance,
                        onBuyWith = onBuyEggWith,
                    )
                }
                else -> {
                    Spacer(modifier = Modifier.width(8.dp))
                    CoinOnlyButton(item = item, coinBalance = coinBalance, onClick = onBuy)
                }
            }
        }
    }
}

/** 2 nút mua trứng nhỏ gọn theo icon tiền tệ (Xu Lá/Giờ tích luỹ) — thay cho 1 nút "Mua ngay" +
 * dòng giá chữ vàng trước đây (T-125). Chạm nút nào mua ngay bằng loại tiền đó, không qua dialog.
 * Màu khớp bubble số dư dùng chung ([com.cozypomo.app.ui.common.BalancePill]): Xu Lá = `secondary`
 * (vàng), Giờ tích luỹ = `primary` (xanh nhạt) — trước đây cả 2 nút cùng màu `primary` nên khó
 * phân biệt (T-126, phản hồi người dùng). */
@Composable
private fun EggCurrencyButtons(item: ShopItemDto, coinBalance: Int?, focusMinutesBalance: Int?, onBuyWith: (String) -> Unit) {
    val priceHours = item.eggType?.priceHours ?: 0
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        MiniCurrencyButton(
            icon = Icons.Filled.Eco,
            label = "${item.priceCoin}",
            color = MaterialTheme.colorScheme.secondary,
            enabled = coinBalance == null || coinBalance >= item.priceCoin,
            onClick = { onBuyWith("COIN") },
        )
        MiniCurrencyButton(
            icon = Icons.Filled.HourglassBottom,
            label = "${priceHours}p",
            color = MaterialTheme.colorScheme.primary,
            enabled = focusMinutesBalance == null || focusMinutesBalance >= priceHours,
            onClick = { onBuyWith("FOCUS_MINUTE") },
        )
    }
}

/** Vật phẩm không phải EGG chỉ có 1 loại tiền (Xu Lá) — cùng kiểu pill nhỏ gọn để đồng nhất với
 * trứng thay vì dòng giá chữ + nút "Mua ngay" to bản trước đây (T-126). */
@Composable
private fun CoinOnlyButton(item: ShopItemDto, coinBalance: Int?, onClick: () -> Unit) {
    MiniCurrencyButton(
        icon = Icons.Filled.Eco,
        label = "${item.priceCoin}",
        color = MaterialTheme.colorScheme.secondary,
        enabled = coinBalance == null || coinBalance >= item.priceCoin,
        onClick = onClick,
    )
}

@Composable
private fun MiniCurrencyButton(icon: ImageVector, label: String, color: Color, enabled: Boolean, onClick: () -> Unit) {
    val bg = if (enabled) color.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainerHighest
    val fg = if (enabled) color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(50),
        color = bg,
        contentColor = fg,
    ) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

@Composable
private fun LargeCurrencyButton(modifier: Modifier = Modifier, icon: ImageVector, label: String, color: Color, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = color),
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
                                color = MaterialTheme.colorScheme.secondary,
                                enabled = coinBalance == null || coinBalance >= item.priceCoin,
                                onClick = { onBuyEggWith("COIN") },
                            )
                            LargeCurrencyButton(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Filled.HourglassBottom,
                                label = "$priceHours phút",
                                color = MaterialTheme.colorScheme.primary,
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
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        ) { Text("Mua ngay") }
                    }
                }
            }
        }
    }
}
