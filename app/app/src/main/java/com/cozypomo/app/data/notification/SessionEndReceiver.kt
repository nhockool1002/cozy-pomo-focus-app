package com.cozypomo.app.data.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.cozypomo.app.R

/**
 * Bắn thông báo dự phòng khi báo động của [SessionNotifier] tới đích — chỉ xảy ra thật sự nếu
 * [com.cozypomo.app.data.timer.TimerForegroundService] không kịp gọi
 * [SessionNotifier.cancelSessionNotification] trước đó (bị hệ thống kill). Cố tình KHÔNG đụng
 * tới Room/gọi API ở đây — chỉ hiện thông báo nhắc người dùng tự mở app, tránh đua với service.
 */
class SessionEndReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val openIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, SessionNotifier.CHANNEL_ID)
            .setContentTitle("Đã hết giờ tập trung!")
            .setContentText("Mở CozyPomo để xem kết quả phiên vừa rồi.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        val sessionId = intent.getStringExtra(SessionNotifier.EXTRA_SESSION_ID)
        val notificationId = sessionId?.let(SessionNotifier::notificationIdFor) ?: SessionNotifier.CHANNEL_ID.hashCode()
        context.getSystemService(NotificationManager::class.java)?.notify(notificationId, notification)
    }
}
