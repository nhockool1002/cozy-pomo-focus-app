package com.cozypomo.app.data.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * T-128 — kích hoạt [InboxNotifyWorker], cùng khuôn mẫu [com.cozypomo.app.data.sync.SyncOutboxScheduler].
 * Chu kỳ 1 giờ — đủ nhanh cho quà Admin/bán đồ Chợ/nhắc streak (không cần tức thời), đủ thưa để
 * không tốn pin; WorkManager cũng không đảm bảo đúng giờ tuyệt đối khi máy vào Doze, chấp nhận trễ.
 */
@Singleton
class InboxNotifyScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Gọi 1 lần lúc app khởi động ([com.cozypomo.app.CozyPomoApp.onCreate]). */
    fun schedulePeriodicCheck() {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val request = PeriodicWorkRequestBuilder<InboxNotifyWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private companion object {
        const val PERIODIC_WORK_NAME = "inbox_notify_periodic"
    }
}
