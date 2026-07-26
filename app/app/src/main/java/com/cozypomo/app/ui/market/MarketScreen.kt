package com.cozypomo.app.ui.market

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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.cozypomo.app.data.network.MarketListingDto
import com.cozypomo.app.ui.common.CurrencyViewModel
import com.cozypomo.app.ui.common.EggIcon
import com.cozypomo.app.ui.common.MessageDialog
import com.cozypomo.app.ui.common.RarityBadge
import com.cozypomo.app.ui.common.SpeciesArtIcon
import com.cozypomo.app.ui.common.parseEggColor

/** T-109 — S-08 Chợ (màn độc lập, không sống trong Cửa hàng — vào qua FAB ở Trang chủ, T-111).
 * Danh sách trộn thú/thực vật/trứng, không phân "Chính thức" — chỉ khác dòng tên người bán. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(currencyViewModel: CurrencyViewModel, onBack: () -> Unit, viewModel: MarketViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val currencyState by currencyViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chợ") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
            )
        },
    ) { innerPadding ->
    Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text(
                "Trao đổi thú, thực vật và trứng với người chơi khác bằng Xu Lá",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MarketTab.entries.forEach { tab ->
                FilterChip(
                    selected = uiState.tab == tab,
                    onClick = { viewModel.selectTab(tab) },
                    label = { Text(tab.label) },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (uiState.listings.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (uiState.tab == MarketTab.MINE) "Bạn chưa đăng tin nào" else "Chợ đang trống — quay lại sau nhé",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 32.dp),
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(uiState.listings, key = { it.id }) { listing ->
                    MarketListingRow(
                        listing = listing,
                        mine = uiState.tab == MarketTab.MINE,
                        coinBalance = currencyState.coinBalance,
                        marketFeePercent = uiState.marketFeePercent,
                        onBuy = { viewModel.requestBuy(listing) },
                        onCancel = { viewModel.cancelListing(listing.id) },
                    )
                }
            }
        }
    }
    } // Scaffold content

    uiState.confirmBuyFor?.let { listing ->
        BuyConfirmDialog(
            listing = listing,
            coinBalance = currencyState.coinBalance,
            onConfirm = viewModel::confirmBuy,
            onDismiss = viewModel::dismissBuyConfirm,
        )
    }

    uiState.lastMessage?.let { message ->
        MessageDialog(
            message = message,
            onDismiss = {
                viewModel.dismissMessage()
                currencyViewModel.refresh()
            },
        )
    }
}

private fun listingName(listing: MarketListingDto): String =
    listing.species?.name ?: listing.ownedEgg?.eggType?.name ?: "Vật phẩm"

private fun listingSellerLabel(listing: MarketListingDto): String =
    if (listing.sellerType == "ADMIN") "CozyPomo" else "@${listing.seller?.displayName ?: listing.seller?.email?.substringBefore("@") ?: "ẩn danh"}"

@Composable
private fun MarketListingRow(
    listing: MarketListingDto,
    mine: Boolean,
    coinBalance: Int?,
    marketFeePercent: Float,
    onBuy: () -> Unit,
    onCancel: () -> Unit,
) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest, modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    val species = listing.species
                    val egg = listing.ownedEgg
                    when {
                        species != null -> SpeciesArtIcon(
                            category = species.category,
                            archetype = species.archetype,
                            paletteIdx = species.paletteIdx,
                            seed = species.name,
                            rarity = species.rarity,
                            size = 34.dp,
                        )
                        egg != null -> EggIcon(color = parseEggColor(egg.eggType.colorHex), size = 26.dp)
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(listingName(listing), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    listing.species?.let {
                        Spacer(modifier = Modifier.width(6.dp))
                        RarityBadge(it.rarity)
                    }
                }
                val statusLine = when {
                    mine && listing.status == "SOLD" -> {
                        // Người bán nhận priceCoin trừ phí marketFeePercent (Math.round, khớp
                        // đúng công thức backend đã dùng ở MarketService.buy) — không hiện
                        // priceCoin thô, tránh gây hiểu lầm "nhận đủ 100%" trong khi đã bị phí.
                        val received = Math.round(listing.priceCoin * (1 - marketFeePercent / 100f))
                        "Đã bán · +$received Xu (giá ${listing.priceCoin}, phí ${marketFeePercent.toInt()}%)"
                    }
                    mine && listing.status == "CANCELLED" -> "Đã huỷ"
                    mine && listing.status == "PENDING_APPROVAL" -> "Chờ Admin duyệt (loài hiếm)"
                    mine && listing.status == "REJECTED" -> "Đã bị từ chối"
                    mine -> "Đang bán · ${listing.priceCoin} Xu"
                    listing.ownedEgg != null -> "bởi ${listingSellerLabel(listing)} · đã ấp ${listing.ownedEgg.incubatedMin}/${listing.ownedEgg.eggType.hatchDurationMin}p"
                    else -> "bởi ${listingSellerLabel(listing)}"
                }
                Text(statusLine, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(modifier = Modifier.width(8.dp))
            when {
                mine && listing.status == "ACTIVE" -> OutlinedButton(onClick = onCancel) { Text("Huỷ") }
                mine -> {}
                coinBalance == null || coinBalance < listing.priceCoin ->
                    Button(onClick = {}, enabled = false) { Text("Cần thêm") }
                else -> Button(onClick = onBuy, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Text("${listing.priceCoin} Xu")
                }
            }
        }
    }
}

@Composable
private fun BuyConfirmDialog(
    listing: MarketListingDto,
    coinBalance: Int?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val balanceAfter = (coinBalance ?: 0) - listing.priceCoin
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Mua ${listingName(listing)}?", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Giá ${listing.priceCoin} Xu Lá · bởi ${listingSellerLabel(listing)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Số dư sau: $balanceAfter Xu Lá",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (balanceAfter < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Huỷ") }
                    Button(onClick = onConfirm, enabled = balanceAfter >= 0, modifier = Modifier.weight(1f)) { Text("Mua ngay") }
                }
            }
        }
    }
}
