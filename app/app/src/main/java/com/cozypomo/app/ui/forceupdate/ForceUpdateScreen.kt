package com.cozypomo.app.ui.forceupdate

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cozypomo.app.ui.common.JarMark

/**
 * T-122 — màn chặn toàn màn hình khi `versionCode` hiện tại thấp hơn `minSupportedVersionCode`.
 * Không có nút quay lại/bỏ qua trong UI (chỉ 1 lựa chọn: cập nhật) — `RootNavHost` điều hướng
 * tới đây bằng `popUpTo(Splash){inclusive=true}` nên back stack rỗng, bấm nút Back hệ thống sẽ
 * thoát app (chấp nhận được, không cần chặn — user mở lại app sẽ quay lại đúng màn này).
 */
@Composable
fun ForceUpdateScreen(viewModel: ForceUpdateViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val config = viewModel.config

    Box(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            JarMark(size = 96.dp)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = config.updateTitle,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = config.updateMessage,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
            )
            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = { openPlayStore(context, config.updateUrl) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Cập nhật ngay")
            }
        }
    }
}

private fun openPlayStore(context: Context, updateUrl: String) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}")).apply {
                setPackage("com.android.vending")
            },
        )
    } catch (e: ActivityNotFoundException) {
        // Máy không cài Play Store (hiếm, emulator không kèm Play Services) — mở thẳng URL https.
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl)))
    }
}
