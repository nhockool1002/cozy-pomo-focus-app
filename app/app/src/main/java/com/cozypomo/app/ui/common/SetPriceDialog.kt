package com.cozypomo.app.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/** T-110 — dialog đặt giá dùng chung cho Đăng bán ở Chợ (loài từ Khu rừng, trứng từ Kho đồ) —
 * không ép giá sàn/trần (theo yêu cầu Dev1002), chỉ cần số nguyên dương. */
@Composable
fun SetPriceDialog(
    title: String,
    subtitle: String? = null,
    onConfirm: (priceCoin: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var priceText by remember { mutableStateOf("") }
    val price = priceText.toIntOrNull()

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                subtitle?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { new -> if (new.all(Char::isDigit)) priceText = new },
                    label = { Text("Giá (Xu Lá)") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Huỷ") }
                    Button(
                        onClick = { price?.let(onConfirm) },
                        enabled = price != null && price > 0,
                        modifier = Modifier.weight(1f),
                    ) { Text("Đăng bán") }
                }
            }
        }
    }
}
