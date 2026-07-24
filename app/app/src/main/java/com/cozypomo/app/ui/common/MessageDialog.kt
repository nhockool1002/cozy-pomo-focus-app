package com.cozypomo.app.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * Modal thông báo ngắn dùng chung (thay Snackbar cũ) — Snackbar neo đáy màn hình từng bị thanh
 * điều hướng dưới (bottom nav + thanh điều hướng hệ thống) che khuất vì render như 1 Box nổi tự
 * do, không tính padding/inset. Dialog render trong 1 window riêng nên luôn hiện giữa màn hình,
 * không bao giờ bị che, và "tinh tế" hơn kiểu banner đáy màn hình.
 */
@Composable
fun MessageDialog(message: String, onDismiss: () -> Unit, confirmLabel: String = "Đóng") {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp).widthIn(min = 220.dp, max = 320.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(confirmLabel)
                }
            }
        }
    }
}
