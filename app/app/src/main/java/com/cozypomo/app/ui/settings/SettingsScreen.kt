package com.cozypomo.app.ui.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cozypomo.app.data.network.InventoryItemDto
import com.cozypomo.app.ui.common.CurrencyViewModel
import com.cozypomo.app.ui.common.TesterCheatViewModel

/** S-07 — Cài đặt: gom mọi chức năng cấu hình + tài khoản (kể cả đăng xuất) vào 1 màn riêng.
 * Tester: chạm 5 lần vào "Phiên bản" bật/tắt bubble cheat nổi dùng chung toàn app (xem
 * [TesterCheatViewModel] + CozyPomoNavHost — bubble/menu không còn sống trong màn này nữa). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    currencyViewModel: CurrencyViewModel,
    cheatViewModel: TesterCheatViewModel,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencyState by currencyViewModel.uiState.collectAsState()
    val cheatState by cheatViewModel.uiState.collectAsState()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cài đặt") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            SettingsSection(title = "Tài khoản") {
                SettingsRow(icon = Icons.Filled.AccountCircle, label = uiState.email ?: "Đang tải…")
            }

            Spacer(modifier = Modifier.height(20.dp))

            SettingsSection(title = "Cấu hình phiên tập trung") {
                SettingsSliderRow(
                    label = "Thời gian tập trung mặc định",
                    value = uiState.focusMinutes,
                    valueRange = 10f..120f,
                    unit = "phút",
                    onDrag = viewModel::onFocusMinutesDrag,
                    onChangeFinished = viewModel::onFocusMinutesChangeFinished,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                SettingsSliderRow(
                    label = "Thời gian nghỉ mặc định",
                    value = uiState.breakMinutes,
                    valueRange = 1f..60f,
                    unit = "phút",
                    onDrag = viewModel::onBreakMinutesDrag,
                    onChangeFinished = viewModel::onBreakMinutesChangeFinished,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Strict Mode", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Không cho thoát app giữa phiên tập trung",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = uiState.strictModeEnabled, onCheckedChange = viewModel::onStrictModeToggle)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            SettingsSection(title = "Âm thanh") {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Chủ đề âm thanh",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SOUND_THEME_OPTIONS.forEach { (id, label) ->
                            FilterChip(
                                selected = uiState.soundTheme == id,
                                onClick = { viewModel.onSoundThemeSelected(id) },
                                label = { Text(label) },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            SettingsSection(title = "Kho đồ") {
                if (uiState.inventory.isEmpty()) {
                    SettingsRow(icon = Icons.Filled.LocalDrink, label = "Chưa sở hữu bình/nhạc nào — mua ở Cửa hàng")
                } else {
                    uiState.inventory.forEachIndexed { index, item ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        InventoryRow(item = item, onToggleEquip = { viewModel.toggleEquip(item.id) })
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            SettingsSection(title = "Số dư") {
                SettingsBalanceRow(
                    icon = Icons.Filled.Eco,
                    label = "Xu Lá",
                    value = currencyState.coinBalance?.toString() ?: "…",
                    tint = MaterialTheme.colorScheme.secondary,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                SettingsBalanceRow(
                    icon = Icons.Filled.HourglassBottom,
                    label = "Giờ tích luỹ",
                    value = currencyState.focusMinutesBalance?.let { "$it phút" } ?: "…",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            SettingsSection(title = "Thông tin ứng dụng") {
                SettingsRow(
                    icon = Icons.Filled.Info,
                    label = "Phiên bản ${uiState.versionName}",
                    onClick = if (cheatState.isTester) cheatViewModel::onVersionTapped else null,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { viewModel.logout(onLoggedOut) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(Icons.Filled.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Đăng xuất")
            }

            Spacer(modifier = Modifier.height(28.dp))

            uiState.userId?.let { userId ->
                Text(
                    text = userId,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            clipboard.setText(AnnotatedString(userId))
                            Toast.makeText(context, "Đã sao chép ID", Toast.LENGTH_SHORT).show()
                        }
                        .padding(vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp),
    )
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.fillMaxWidth()) { content() }
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, label: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Slider có nhãn + giá trị hiện tại — dùng cho thời gian tập trung/nghỉ mặc định. [onDrag] cập
 * nhật UI ngay khi kéo (mượt), [onChangeFinished] mới thật sự gọi PATCH /settings khi thả tay. */
@Composable
private fun SettingsSliderRow(
    label: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    unit: String,
    onDrag: (Int) -> Unit,
    onChangeFinished: (Int) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                "$value $unit",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onDrag(it.toInt()) },
            onValueChangeFinished = { onChangeFinished(value) },
            valueRange = valueRange,
        )
    }
}

/** 1 dòng vật phẩm trong Kho đồ (bình/nhạc đã mua ở Cửa hàng) + nút trang bị/bỏ trang bị. */
@Composable
private fun InventoryRow(item: InventoryItemDto, onToggleEquip: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val icon = if (item.shopItem.category == "MUSIC") Icons.Filled.MusicNote else Icons.Filled.LocalDrink
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(item.shopItem.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (item.equipped) {
            Text(
                "Đang dùng",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        } else {
            Button(
                onClick = onToggleEquip,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            ) { Text("Dùng") }
        }
    }
}

/** Dòng số dư "tinh tế" cho Cài đặt — ghi rõ tên loại tiền tệ + số lượng, khác kiểu pill gọn ở Trang chủ. */
@Composable
private fun SettingsBalanceRow(icon: ImageVector, label: String, value: String, tint: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = CircleShape, color = tint.copy(alpha = 0.15f), modifier = Modifier.size(36.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = tint)
    }
}
