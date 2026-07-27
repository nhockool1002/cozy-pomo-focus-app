package com.cozypomo.app.ui.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cozypomo.app.data.network.InboxMessageDto
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZoneId.systemDefault())

/** S-08 — Hộp thư (T-124): danh sách thư (nhận trứng/trứng nở/admin tặng quà), chạm để đánh dấu đã đọc. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(onBack: () -> Unit, viewModel: InboxViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadMessages() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hộp thư") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    if (state.unreadCount > 0) {
                        TextButton(onClick = viewModel::markAllRead) { Text("Đánh dấu tất cả đã đọc") }
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                state.loading && state.messages.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.messages.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            Icons.Filled.MailOutline,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.padding(6.dp))
                        Text("Chưa có thư nào", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.messages, key = { it.id }) { message ->
                            InboxMessageRow(message = message, onClick = { viewModel.markRead(message.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InboxMessageRow(message: InboxMessageDto, onClick: () -> Unit) {
    val background = if (message.isRead) Color.Transparent else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(background)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = inboxTypeIcon(message.type),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = message.title,
                fontWeight = if (message.isRead) FontWeight.Normal else FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(text = message.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = runCatching { DATE_FORMATTER.format(Instant.parse(message.createdAt)) }.getOrDefault(""),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
        if (!message.isRead) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.error, CircleShape),
            )
        }
    }
}

private fun inboxTypeIcon(type: String) = when (type) {
    "EGG_RECEIVED" -> Icons.Filled.Egg
    "EGG_HATCHED" -> Icons.Filled.AutoAwesome
    "ADMIN_GIFT" -> Icons.Filled.CardGiftcard
    else -> Icons.Filled.MarkEmailRead
}
