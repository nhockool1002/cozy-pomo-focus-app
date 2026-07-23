package com.cozypomo.app.ui.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cozypomo.app.data.timer.SessionUiState
import com.cozypomo.app.ui.common.CurrencyViewModel
import com.cozypomo.app.ui.common.JarMark
import com.cozypomo.app.ui.common.parseEggColor

/** S-01 — Trang chủ/Timer, lõi sản phẩm. Xem docs/technical-spec.md §2. Số dư Xu Lá/Giờ tích luỹ
 * hiện qua bubble nổi dùng chung [CurrencyViewModel] (xem CozyPomoNavHost) — không tự tải riêng ở đây nữa. */
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    currencyViewModel: CurrencyViewModel,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val sessionState by viewModel.sessionState.collectAsState()
    val isRunning = sessionState is SessionUiState.Running
    val selectedEgg = uiState.selectedOwnedEgg
    val isIncubating = isRunning && selectedEgg != null

    // Số dư đổi sau khi hoàn thành/bỏ cuộc phiên — báo cho bubble dùng chung tải lại.
    androidx.compose.runtime.LaunchedEffect(uiState.sessionResult) {
        if (uiState.sessionResult != null) currencyViewModel.refresh()
    }

    // Tiến trình ấp "sống" — cộng dồn ước tính từ số phút đã trôi qua trong phiên hiện tại
    // (theo incubationRatio) lên trên incubatedMin đã lưu, để hình trứng trong bình cập nhật
    // ngay khi đếm ngược chạy, không cần đợi hoàn thành phiên mới thấy thay đổi.
    val hatchDurationMin = (selectedEgg?.eggType?.hatchDurationMin ?: 1).coerceAtLeast(1)
    val liveIncubatedMin = if (selectedEgg != null && sessionState is SessionUiState.Running) {
        val running = sessionState as SessionUiState.Running
        val elapsedMin = (running.totalMs - running.remainingMs) / 60000f * uiState.incubationRatio
        selectedEgg.incubatedMin + elapsedMin
    } else {
        selectedEgg?.incubatedMin?.toFloat() ?: 0f
    }
    val jarProgress = (liveIncubatedMin / hatchDurationMin).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Cài đặt")
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IncubatingJar(size = 160.dp, isIncubating = isIncubating) {
                JarMark(
                    size = 140.dp,
                    eggColor = selectedEgg?.let { parseEggColor(it.eggType.colorHex) },
                    progress = jarProgress,
                    animate = isRunning,
                )
                Surface(
                    onClick = { if (!isRunning) viewModel.openEggPicker() },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Filled.Add, contentDescription = "Chọn trứng")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (selectedEgg != null) {
                    "${liveIncubatedMin.toInt()}/$hatchDurationMin phút ấp"
                } else {
                    "Chạm nút + để chọn trứng ấp"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )

            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = formatCountdown(sessionState, uiState.durationMin),
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 56.sp),
            )

            Spacer(modifier = Modifier.height(24.dp))
            Slider(
                value = uiState.durationMin.toFloat(),
                onValueChange = { viewModel.onDurationChange(it.toInt()) },
                valueRange = 10f..120f,
                enabled = !isRunning,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "${uiState.durationMin} phút",
                style = MaterialTheme.typography.bodyLarge,
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Nhận thưởng bằng:",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = uiState.rewardCurrency == "COIN",
                    onClick = { viewModel.onRewardCurrencyChange("COIN") },
                    enabled = !isRunning,
                    leadingIcon = { Icon(Icons.Filled.Eco, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("Xu Lá") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.secondary),
                )
                FilterChip(
                    selected = uiState.rewardCurrency == "FOCUS_MINUTE",
                    onClick = { viewModel.onRewardCurrencyChange("FOCUS_MINUTE") },
                    enabled = !isRunning,
                    leadingIcon = { Icon(Icons.Filled.HourglassBottom, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text("Giờ tích luỹ") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary),
                )
            }

            if (selectedEgg != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Dành cho ấp trứng: ${(uiState.incubationRatio * 100).toInt()}% · còn lại quy đổi theo lựa chọn ở trên",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = uiState.incubationRatio,
                    onValueChange = viewModel::onIncubationRatioChange,
                    valueRange = 0f..1f,
                    enabled = !isRunning,
                    colors = androidx.compose.material3.SliderDefaults.colors(
                        activeTrackColor = MaterialTheme.colorScheme.tertiary,
                        thumbColor = MaterialTheme.colorScheme.tertiary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (isRunning) {
            Button(
                onClick = viewModel::requestGiveUp,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text("BỎ CUỘC")
            }
        } else {
            Button(
                onClick = viewModel::startSession,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text("BẮT ĐẦU")
            }
        }
    }

    if (uiState.showGiveUpConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::dismissGiveUp,
            title = { Text("Bỏ cuộc?") },
            text = { Text("Bạn sẽ không nhận được Xu Lá hay Giờ tích luỹ cho phiên này. Trứng đang ấp vẫn an toàn, giữ nguyên tiến trình cũ. Bạn có chắc chắn muốn dừng lại không?") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmGiveUp) {
                    Text("Đồng ý", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissGiveUp) { Text("Huỷ") }
            },
        )
    }

    if (uiState.showEggPicker) {
        EggPickerDialog(
            ownedEggs = uiState.ownedEggs,
            selectedOwnedEgg = uiState.selectedOwnedEgg,
            onSelect = viewModel::selectOwnedEgg,
            onDismiss = viewModel::closeEggPicker,
        )
    }

    uiState.sessionResult?.let { result ->
        SessionResultDialog(result = result, onDismiss = viewModel::dismissSessionResult)
    }
}

/** Vầng hào quang mờ nhẹ nhấp nháy quanh bình khi đang ấp trứng — "animation đang ấp trứng". */
@Composable
private fun IncubatingJar(size: androidx.compose.ui.unit.Dp, isIncubating: Boolean, content: @Composable BoxScope.() -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "incubateGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(1100), repeatMode = RepeatMode.Reverse),
        label = "glowAlpha",
    )
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1100), repeatMode = RepeatMode.Reverse),
        label = "glowScale",
    )

    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        if (isIncubating) {
            Box(
                modifier = Modifier
                    .size(size * 0.9f)
                    .graphicsLayer { scaleX = glowScale; scaleY = glowScale }
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha), CircleShape),
            )
        }
        content()
    }
}

private fun formatCountdown(sessionState: SessionUiState, idleDurationMin: Int): String =
    when (sessionState) {
        is SessionUiState.Running -> {
            val totalSec = sessionState.remainingMs / 1000
            "%02d:%02d".format(totalSec / 60, totalSec % 60)
        }
        SessionUiState.Idle -> "%02d:00".format(idleDurationMin)
    }
