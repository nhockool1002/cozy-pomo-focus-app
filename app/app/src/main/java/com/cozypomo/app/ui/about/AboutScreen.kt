package com.cozypomo.app.ui.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cozypomo.app.BuildConfig
import com.cozypomo.app.ui.common.JarMark

private const val BUY_ME_A_COFFEE_URL = "https://buymeacoffee.com/nhutnm"

/** Giới thiệu ứng dụng — vào từ icon (i) ở TopAppBar của Cài đặt. Mô tả ngắn + tác giả + phiên
 * bản (khớp tag GitHub thật, xem T-101) + nút ủng hộ Buy Me a Coffee. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Giới thiệu") },
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
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            JarMark(size = 88.dp)
            Spacer(modifier = Modifier.height(12.dp))
            Text("CozyPomo", style = MaterialTheme.typography.headlineMedium)
            Text(
                BuildConfig.RELEASE_TAG,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                "Tác giả: Dev1002",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 16.dp),
            )

            Text(
                "CozyPomo là ứng dụng Pomodoro nhẹ nhàng — mỗi phiên tập trung là một bước ấp nở, " +
                    "giúp bạn dần mở khoá và nuôi lớn một khu rừng sinh vật của riêng mình. Tập trung " +
                    "càng đều, khu rừng càng thêm nhiều loài mới để khám phá.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )

            Button(
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BUY_ME_A_COFFEE_URL)))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                // padding TRƯỚC fillMaxWidth/height — nếu để sau .height(52.dp), padding ăn vào
                // chiều cao đã cố định thay vì đẩy cả nút xuống, ép nội dung vào còn ~28dp khiến
                // chữ bị cắt trên (bug thật đã gặp khi tự kiểm thử: đỉnh chữ B/M/C bị mất).
                modifier = Modifier.padding(top = 24.dp).fillMaxWidth().height(52.dp),
            ) {
                Icon(Icons.Filled.Coffee, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Buy Me a Coffee")
            }
        }
    }
}
