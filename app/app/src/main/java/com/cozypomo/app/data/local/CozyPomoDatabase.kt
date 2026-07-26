package com.cozypomo.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cozypomo.app.data.local.session.SessionDao
import com.cozypomo.app.data.local.session.SessionEntity
import com.cozypomo.app.data.local.sync.SyncOutboxDao
import com.cozypomo.app.data.local.sync.SyncOutboxEntity

/**
 * DB local-first (T-029). Bảng `sessions` là nguồn sự thật cho Foreground Service/TimerRepository
 * (T-030). Bảng `sync_outbox` (T-043) là hàng đợi đồng bộ offline→online cho các thao tác phiên
 * gọi backend thất bại lúc mất mạng — xem [com.cozypomo.app.data.sync.SyncOutboxWorker].
 */
@Database(entities = [SessionEntity::class, SyncOutboxEntity::class], version = 4, exportSchema = false)
abstract class CozyPomoDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun syncOutboxDao(): SyncOutboxDao
}
