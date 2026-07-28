package com.cozypomo.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.cozypomo.app.data.notification.InboxNotifyScheduler
import com.cozypomo.app.data.sync.SyncOutboxScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CozyPomoApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var syncOutboxScheduler: SyncOutboxScheduler
    @Inject lateinit var inboxNotifyScheduler: InboxNotifyScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    /** T-043 — lưới an toàn định kỳ cho hàng đợi outbox, xem [SyncOutboxScheduler.schedulePeriodicFlush].
     * T-128 — cùng lúc kích hoạt kiểm tra Hộp thư nền, xem [InboxNotifyScheduler.schedulePeriodicCheck]. */
    override fun onCreate() {
        super.onCreate()
        syncOutboxScheduler.schedulePeriodicFlush()
        inboxNotifyScheduler.schedulePeriodicCheck()
    }
}
