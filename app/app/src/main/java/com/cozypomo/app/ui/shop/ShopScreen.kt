package com.cozypomo.app.ui.shop

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.cozypomo.app.data.network.ShopItemDto
import com.cozypomo.app.ui.common.CurrencyViewModel
import com.cozypomo.app.ui.common.EggIcon
import com.cozypomo.app.ui.common.FloatingIcon
import com.cozypomo.app.ui.common.parseEggColor

/** T-037 — S-05 Cửa hàng: Trứng mới / Bình thuỷ tinh / Nhạc nền, item có hiệu ứng bồng bềnh nhẹ.
 * Số dư hiện qua bubble nổi dùng chung [CurrencyViewModel] (xem CozyPomoNavHost). */
@Composable
fun ShopScreen(currencyViewModel: CurrencyViewModel, viewModel: ShopViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val currencyState by currencyViewModel.uiState.collectAsState()

    // Số dư đổi sau khi mua — báo cho bubble dùng chung tải lại.
    LaunchedEffect(uiState.lastMessage) {
        if (uiState.lastMessage != null) currencyViewModel.refresh()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Tiệm Tạp Hóa Rừng Xanh",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }

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
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(uiState.items, key = { it.id }) { item ->
                        ShopItemRow(
                            item = item,
                            owned = uiState.ownedShopItemIds.contains(item.id),
                            coinBalance = currencyState.coinBalance,
                            onBuy = { viewModel.requestPurchase(item) },
                        )
                    }
                }
            }
        }

        uiState.lastMessage?.let { message ->
            Snackbar(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                action = { TextButton(onClick = viewModel::dismissMessage) { Text("Đóng") } },
            ) { Text(message) }
        }
    }

    uiState.payWithChoiceFor?.let { item ->
        PayWithDialog(
            item = item,
            coinBalance = currencyState.coinBalance,
            focusMinutesBalance = currencyState.focusMinutesBalance,
            onChoose = { payWith -> viewModel.confirmEggPurchase(item, payWith) },
            onDismiss = viewModel::dismissPayWithChoice,
        )
    }
}

@Composable
private fun ShopItemRow(item: ShopItemDto, owned: Boolean, coinBalance: Int?, onBuy: () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            FloatingIcon {
                Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest, modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        when (item.category) {
                            "EGG" -> EggIcon(color = parseEggColor(item.eggType?.colorHex ?: "#9CB380"), size = 28.dp)
                            "JAR_SKIN" -> Icon(Icons.Filled.LocalDrink, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            else -> Icon(Icons.Filled.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                item.description?.let {
                    Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                Text(
                    "${item.priceCoin} Xu" + if (item.category == "EGG") " · hoặc Giờ tích luỹ" else "",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            when {
                owned -> OutlinedButton(onClick = {}, enabled = false) { Text("Đã sở hữu") }
                item.category != "EGG" && (coinBalance == null || coinBalance < item.priceCoin) ->
                    Button(onClick = {}, enabled = false) { Text("Cần thêm Xu") }
                else -> Button(onClick = onBuy, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Text("Mua ngay")
                }
            }
        }
    }
}

@Composable
private fun PayWithDialog(
    item: ShopItemDto,
    coinBalance: Int?,
    focusMinutesBalance: Int?,
    onChoose: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val priceHours = item.eggType?.priceHours ?: 0
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Trả bằng gì cho ${item.name}?", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onChoose("COIN") },
                    enabled = coinBalance == null || coinBalance >= item.priceCoin,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("${item.priceCoin} Xu Lá") }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onChoose("FOCUS_MINUTE") },
                    enabled = focusMinutesBalance == null || focusMinutesBalance >= priceHours,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("$priceHours phút Giờ tích luỹ") }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDismiss) { Text("Huỷ") }
            }
        }
    }
}
