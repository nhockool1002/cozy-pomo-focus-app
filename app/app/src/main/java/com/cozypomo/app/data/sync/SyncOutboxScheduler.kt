package com.cozypomo.app.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * T-043 — kích hoạt [SyncOutboxWorker]. Cả 2 loại job đều ràng buộc `NetworkType.CONNECTED` —
 * WorkManager tự đợi có mạng mới chạy, không cần tự theo dõi kết nối thủ công (`ConnectivityManager`).
 */
@Singleton
class SyncOutboxScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    /** Gọi ngay sau khi ghi 1 dòng outbox mới (mất mạng lúc bắt đầu/hoàn thành/bỏ cuộc phiên) —
     * đẩy sớm nếu mạng đã có lại ngay lúc đó, không cần đợi tới lượt định kỳ tiếp theo. */
    fun enqueueFlush() {
        val request = OneTimeWorkRequestBuilder<SyncOutboxWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(IMMEDIATE_WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    /** Gọi 1 lần lúc app khởi động ([com.cozypomo.app.CozyPomoApp.onCreate]) — lưới an toàn định
     * kỳ, phòng trường hợp app bị đóng hẳn ngay sau khi ghi outbox, trước khi kịp gọi [enqueueFlush]. */
    fun schedulePeriodicFlush() {
        val request = PeriodicWorkRequestBuilder<SyncOutboxWorker>(30, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private companion object {
        const val IMMEDIATE_WORK_NAME = "sync_outbox_flush"
        const val PERIODIC_WORK_NAME = "sync_outbox_periodic"
    }
}
