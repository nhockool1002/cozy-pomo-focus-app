package com.cozypomo.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cozypomo.app.data.local.session.SessionDao
import com.cozypomo.app.data.local.session.SessionEntity

/**
 * DB local-first (T-029). Chỉ có bảng `sessions` ở phiên bản đầu — đủ cho TimerRepository
 * (T-030). Các bảng khác (inventory trứng, collection, settings) sẽ thêm dần khi xây các
 * Repository tương ứng (T-032/T-035/T-039) thay vì tạo trước schema chưa dùng tới.
 */
@Database(entities = [SessionEntity::class], version = 1, exportSchema = false)
abstract class CozyPomoDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
}
