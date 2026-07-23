package com.cozypomo.app.ui.splash

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import android.media.MediaPlayer
import com.cozypomo.app.BuildConfig
import com.cozypomo.app.R
import com.cozypomo.app.ui.common.JarMark
import com.cozypomo.app.ui.common.SpeciesArtIcon
import kotlinx.coroutines.delay

private data class SplashCreature(val category: String, val archetype: String, val paletteIdx: Int, val seed: String)

private val SPLASH_CREATURES = listOf(
    SplashCreature("FOREST", "fox", 0, "Cáo Pomodoro"),
    SplashCreature("PLANT", "flowerRound", 4, "Hoa Tulip"),
    SplashCreature("SEA", "turtle", 9, "Rùa May Mắn"),
)

/** S-00 — làm nóng phiên đăng nhập rồi điều hướng tiếp; hiện đủ lâu để thấy hoạt ảnh chào mừng. */
@Composable
fun SplashScreen(
    onNavigate: (SplashDestination) -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val destination by viewModel.destination.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(destination) {
        destination?.let(onNavigate)
    }

    // Âm thanh chào mừng vui nhộn — phát đúng 1 lần khi Splash xuất hiện.
    LaunchedEffect(Unit) {
        runCatching {
            val player = MediaPlayer.create(context, R.raw.welcome_chime)
            player.setOnCompletionListener { it.release() }
            player.start()
        }
    }

    Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            JarMark(size = 120.dp)
            Spacer(modifier = Modifier.height(20.dp))
            Text(text = "CozyPomo", style = MaterialTheme.typography.headlineMedium)

            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                SPLASH_CREATURES.forEachIndexed { index, creature ->
                    EntranceReveal(delayMillis = 200 + index * 180) {
                        SpeciesArtIcon(
                            category = creature.category,
                            archetype = creature.archetype,
                            paletteIdx = creature.paletteIdx,
                            seed = creature.seed,
                            size = 44.dp,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
            )
        }

        Text(
            text = "v${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
        )
    }
}

@Composable
private fun EntranceReveal(delayMillis: Int, content: @Composable () -> Unit) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        appeared = true
    }
    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.4f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "splashCreatureScale",
    )
    val alpha by animateFloatAsState(targetValue = if (appeared) 1f else 0f, label = "splashCreatureAlpha")
    Box(modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }) {
        content()
    }
}
