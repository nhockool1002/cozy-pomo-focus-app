package com.cozypomo.app.data.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import com.cozypomo.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SessionNotifier (mục 3.9 docs/technical-spec.md, T-042) — lớp bảo hiểm độc lập báo hết giờ tập
 * trung ngay cả khi màn hình khoá VÀ [com.cozypomo.app.data.timer.TimerForegroundService] bị hệ
 * thống kill giữa chừng (hiếm nhưng có thể xảy ra), dùng AlarmManager (không phụ thuộc process
 * còn sống). Dùng [AlarmManager.setAndAllowWhileIdle] (không cần quyền `SCHEDULE_EXACT_ALARM` của
 * Android 12+) thay vì bản exact — chấp nhận trễ vài phút trong Doze vì đây chỉ là lớp dự phòng,
 * đường chính báo hết giờ vẫn là service đếm bằng `SystemClock.elapsedRealtime()`.
 *
 * [SessionEndReceiver] chỉ hiện thông báo, KHÔNG đụng tới Room/gọi API — tránh đua (race) với
 * service khi cả 2 cùng chạy đúng lúc; logic hoàn thành phiên thật vẫn chỉ có 1 nơi duy nhất
 * (TimerRepository.completeSession, tự phục hồi khi mở lại app qua ensureServiceRunningIfActive).
 */
@Singleton
class SessionNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    init {
        createNotificationChannel()
    }

    fun scheduleSessionEndNotification(sessionId: String, triggerAtEpochMs: Long) {
        alarmManager?.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtEpochMs, pendingIntentFor(sessionId))
    }

    /** Gọi khi phiên đã kết thúc bằng đường chính (hoàn thành/bỏ cuộc) — huỷ báo động dự phòng,
     * đồng thời dọn luôn thông báo nếu lỡ đã hiện trước đó (hiếm, race hẹp lúc alarm vừa bắn). */
    fun cancelSessionNotification(sessionId: String) {
        alarmManager?.cancel(pendingIntentFor(sessionId))
        context.getSystemService(NotificationManager::class.java)?.cancel(notificationIdFor(sessionId))
    }

    private fun pendingIntentFor(sessionId: String): PendingIntent {
        val intent = Intent(context, SessionEndReceiver::class.java).putExtra(EXTRA_SESSION_ID, sessionId)
        return PendingIntent.getBroadcast(
            context,
            sessionId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Hết giờ tập trung",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Báo khi phiên tập trung kết thúc, kể cả lúc khoá màn hình"
                setSound(
                    Uri.parse("android.resource://${context.packageName}/${R.raw.completion_chime}"),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
            }
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val EXTRA_SESSION_ID = "session_id"
        const val CHANNEL_ID = "session_end_channel"
        fun notificationIdFor(sessionId: String): Int = sessionId.hashCode()
    }
}
