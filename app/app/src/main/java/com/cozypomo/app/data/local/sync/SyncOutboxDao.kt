package com.cozypomo.app.data.local.sync

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SyncOutboxDao {
    @Insert
    suspend fun insert(entry: SyncOutboxEntity)

    /** Thứ tự tạo — xử lý `session_create` trước `session_complete`/`session_give_up` cùng phiên. */
    @Query("SELECT * FROM sync_outbox ORDER BY createdAtEpochMs ASC")
    suspend fun getAll(): List<SyncOutboxEntity>

    @Delete
    suspend fun delete(entry: SyncOutboxEntity)
}
