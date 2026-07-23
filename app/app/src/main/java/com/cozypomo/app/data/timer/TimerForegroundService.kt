package com.cozypomo.app.data.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.cozypomo.app.R
import com.cozypomo.app.data.local.session.SessionDao
import com.cozypomo.app.data.local.session.SessionStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Đếm ngược chính xác kể cả khi khoá màn hình/app ở nền — dùng SystemClock.elapsedRealtime()
 * (không Handler.postDelayed đơn thuần vì trôi giờ). Khi về 0, tự gọi
 * TimerRepository.completeSession dù UI không mở (đúng yêu cầu NFR "hoạt động ngầm").
 */
@AndroidEntryPoint
class TimerForegroundService : Service() {

    @Inject lateinit var sessionDao: SessionDao
    @Inject lateinit var timerRepository: TimerRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val sessionId = intent?.getStringExtra(EXTRA_SESSION_ID)
        if (sessionId == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification("Đang tập trung…"))

        tickJob?.cancel()
        tickJob = serviceScope.launch {
            while (isActive) {
                val entity = sessionDao.getById(sessionId)
                if (entity == null || entity.status != SessionStatus.RUNNING) {
                    stopSelf()
                    return@launch
                }

                val totalMs = entity.plannedMin * 60_000L
                val elapsed = SystemClock.elapsedRealtime() - entity.startElapsedRealtimeMs
                val remaining = totalMs - elapsed

                if (remaining <= 0) {
                    timerRepository.completeSession(sessionId)
                    stopSelf()
                    return@launch
                }

                updateNotification(formatRemaining(remaining))
                delay(1000)
            }
        }
        return START_STICKY
    }

    private fun formatRemaining(ms: Long): String {
        val totalSec = ms / 1000
        val minutes = totalSec / 60
        val seconds = totalSec % 60
        return "%02d:%02d còn lại".format(minutes, seconds)
    }

    private fun buildNotification(text: String): Notification {
        val openIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CozyPomo")
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Phiên tập trung",
                NotificationManager.IMPORTANCE_LOW,
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        tickJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_SESSION_ID = "session_id"
        private const val CHANNEL_ID = "timer_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
