package com.cozypomo.app.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cozypomo.app.data.network.EggTypeDto
import com.cozypomo.app.data.timer.SessionUiState
import com.cozypomo.app.ui.common.JarMark
import kotlinx.coroutines.launch

/** S-01 — Trang chủ/Timer, lõi sản phẩm. Xem docs/technical-spec.md §2. */
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val sessionState by viewModel.sessionState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val isRunning = sessionState is SessionUiState.Running

    LaunchedEffect(uiState.lastHatch) {
        uiState.lastHatch?.let { hatch ->
            val name = hatch.speciesName ?: "một loài mới"
            scope.launch {
                snackbarHostState.showSnackbar("Trứng đã nở: $name (+${hatch.coinsEarned} Xu Lá)")
            }
            viewModel.consumeHatchResult()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { /* Cài đặt — S-07, chưa xây (T-039) */ }) {
                    Icon(Icons.Filled.Settings, contentDescription = "Cài đặt")
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = uiState.coinBalance?.toString() ?: "…",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                    JarMark(size = 140.dp)
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
                    text = uiState.selectedEggType?.name ?: "Chưa chọn trứng",
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
                    enabled = uiState.selectedEggType != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    Text("BẮT ĐẦU")
                }
            }
        }
    }

    if (uiState.showGiveUpConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::dismissGiveUp,
            title = { Text("Bỏ cuộc?") },
            text = { Text("Trứng của bạn sẽ vỡ và tiến trình phiên này sẽ mất. Bạn có chắc chắn muốn dừng lại không?") },
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
        AlertDialog(
            onDismissRequest = viewModel::closeEggPicker,
            title = { Text("Chọn trứng") },
            text = {
                Column {
                    if (uiState.eggTypes.isEmpty()) {
                        Text("Đang tải…")
                    }
                    uiState.eggTypes.forEach { egg ->
                        EggPickerRow(egg = egg, onClick = { viewModel.selectEggType(egg) })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::closeEggPicker) { Text("Đóng") }
            },
        )
    }
}

@Composable
private fun EggPickerRow(egg: EggTypeDto, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(parseEggColor(egg.colorHex)),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(egg.name, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun parseEggColor(hex: String): Color = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrDefault(Color.Gray)

private fun formatCountdown(sessionState: SessionUiState, idleDurationMin: Int): String =
    when (sessionState) {
        is SessionUiState.Running -> {
            val totalSec = sessionState.remainingMs / 1000
            "%02d:%02d".format(totalSec / 60, totalSec % 60)
        }
        SessionUiState.Idle -> "%02d:00".format(idleDurationMin)
    }
