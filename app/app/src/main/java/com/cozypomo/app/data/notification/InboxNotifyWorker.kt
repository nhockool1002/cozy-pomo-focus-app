package com.cozypomo.app.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cozypomo.app.R
import com.cozypomo.app.data.network.ApiService
import com.cozypomo.app.data.network.InboxMessageDto
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * T-128 — không có push (FCM) ở v1 (xem [com.cozypomo.app.ui.inbox.InboxViewModel]), nên thông
 * báo hệ thống thật (hiện cả khi app đóng/nền) cho thư Hộp thư mới (nhận quà Admin, bán được đồ ở
 * Chợ, nhắc streak sắp mất — xem `InboxMessageType` phía backend) đi qua job nền định kỳ này thay
 * vì push tức thời. Chấp nhận độ trễ tới 1 giờ (chu kỳ [InboxNotifyScheduler]) — đánh đổi hợp lý
 * để không phải dựng hạ tầng FCM (SDK, `google-services.json`, gửi từ backend, lưu device token)
 * cho b1 tính năng thông báo nền, đúng tinh thần "v1 tối giản" đã ghi ở InboxViewModel.
 *
 * Đánh dấu thư "mới" bằng cách so `createdAt` với mốc đã thấy lần trước (lưu trong
 * `SharedPreferences`, không phải Room — chỉ 1 giá trị đơn, không cần DAO/migration riêng). Lần
 * chạy ĐẦU TIÊN (chưa có mốc lưu) chỉ ghi nhận mốc hiện tại, KHÔNG bắn thông báo cho lịch sử đã có
 * từ trước khi cài tính năng này.
 */
@HiltWorker
class InboxNotifyWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val apiService: ApiService,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastSeen = prefs.getString(KEY_LAST_SEEN, null)

        val messages = runCatching { apiService.getInbox() }.getOrElse { return@withContext Result.retry() }
        if (messages.isEmpty()) return@withContext Result.success()

        // list() trả về sắp xếp createdAt giảm dần (mới nhất trước) — xem InboxService.list().
        val newest = messages.first().createdAt
        if (lastSeen == null) {
            prefs.edit().putString(KEY_LAST_SEEN, newest).apply()
            return@withContext Result.success()
        }

        val freshMessages = messages.filter { it.createdAt > lastSeen }
        if (freshMessages.isNotEmpty()) {
            notify(freshMessages)
            prefs.edit().putString(KEY_LAST_SEEN, newest).apply()
        }
        Result.success()
    }

    private fun notify(freshMessages: List<InboxMessageDto>) {
        ensureChannel()
        val openIntent = applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)
        val contentIntent = PendingIntent.getActivity(applicationContext, 0, openIntent, PendingIntent.FLAG_IMMUTABLE)
        val manager = applicationContext.getSystemService(NotificationManager::class.java) ?: return

        if (freshMessages.size == 1) {
            val m = freshMessages.first()
            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setContentTitle(m.title)
                .setContentText(m.body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(m.body))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            manager.notify(m.id.hashCode(), notification)
        } else {
            val inboxStyle = NotificationCompat.InboxStyle()
            freshMessages.forEach { inboxStyle.addLine(it.title) }
            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setContentTitle("Bạn có ${freshMessages.size} thông báo mới")
                .setContentText(freshMessages.joinToString(", ") { it.title })
                .setStyle(inboxStyle)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            manager.notify(GROUP_NOTIFICATION_ID, notification)
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Hộp thư", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Quà từ Admin, bán đồ ở Chợ, nhắc giữ streak"
            }
            applicationContext.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "inbox_channel"
        private const val PREFS_NAME = "inbox_notify_prefs"
        private const val KEY_LAST_SEEN = "last_seen_created_at"
        private const val GROUP_NOTIFICATION_ID = 900001
    }
}
