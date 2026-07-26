package com.cozypomo.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.cozypomo.app.data.sync.SyncOutboxScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CozyPomoApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var syncOutboxScheduler: SyncOutboxScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    /** T-043 — lưới an toàn định kỳ cho hàng đợi outbox, xem [SyncOutboxScheduler.schedulePeriodicFlush]. */
    override fun onCreate() {
        super.onCreate()
        syncOutboxScheduler.schedulePeriodicFlush()
    }
}
