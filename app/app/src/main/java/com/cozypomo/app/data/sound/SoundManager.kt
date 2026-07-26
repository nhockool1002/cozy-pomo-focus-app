package com.cozypomo.app.data.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.cozypomo.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SoundManager (mục 3.9 docs/technical-spec.md, T-042) — nhạc nền theo `soundTheme` (Cài đặt,
 * T-095: trước đây chỉ lưu lựa chọn qua `PATCH /settings`, chưa phát gì thật) qua Media3
 * ExoPlayer (vòng lặp liên tục), tiếng "ting" hoàn thành qua SoundPool (độ trễ thấp, hợp âm
 * ngắn phát đúng lúc phiên vừa kết thúc). App-scoped singleton — sống cùng vòng đời process,
 * process luôn còn sống trong lúc có phiên chạy nhờ [com.cozypomo.app.data.timer.TimerForegroundService].
 */
@Singleton
class SoundManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var ambientPlayer: ExoPlayer? = null
    private var currentTrackId: String? = null

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val completionChimeSoundId = soundPool.load(context, R.raw.completion_chime, 1)

    /**
     * Bắt đầu (hoặc đổi) nhạc nền theo [trackId] ("default"/"rain"/"forest"/"lofi" — đúng giá trị
     * `soundTheme` ở Cài đặt). "default" hoặc giá trị lạ = không phát nhạc nền (yên tĩnh). Gọi lại
     * với cùng trackId đang phát thì bỏ qua, tránh giật nhạc nếu bị gọi lặp (VD service restart).
     */
    fun playAmbientTrack(trackId: String) {
        if (trackId == currentTrackId) return
        stopAmbientTrack()
        val resId = trackIdToRawRes(trackId) ?: return
        currentTrackId = trackId
        ambientPlayer = ExoPlayer.Builder(context)
            .setAudioAttributes(
                androidx.media3.common.AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_ONE
                volume = 0.35f
                setMediaItem(MediaItem.fromUri("android.resource://${context.packageName}/$resId"))
                prepare()
                playWhenReady = true
            }
    }

    /** Dừng + giải phóng nhạc nền — gọi khi phiên kết thúc (hoàn thành/bỏ cuộc). */
    fun stopAmbientTrack() {
        ambientPlayer?.release()
        ambientPlayer = null
        currentTrackId = null
    }

    /** Tiếng "ting" khi hết giờ — phát ngay, không chờ/chặn ambient dừng hẳn. */
    fun playCompletionChime() {
        soundPool.play(completionChimeSoundId, 0.8f, 0.8f, 1, 0, 1f)
    }

    private fun trackIdToRawRes(trackId: String): Int? = when (trackId) {
        "rain" -> R.raw.ambient_rain
        "forest" -> R.raw.ambient_forest
        "lofi" -> R.raw.ambient_lofi
        else -> null
    }
}
