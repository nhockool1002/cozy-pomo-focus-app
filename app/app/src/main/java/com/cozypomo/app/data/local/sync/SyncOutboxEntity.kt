package com.cozypomo.app.data.local.sync

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Loại event outbox — khớp `SYNC_EVENT_TYPES` phía backend (`backend/src/sync/dto/sync-batch.dto.ts`). */
object SyncEventType {
    const val SESSION_CREATE = "session_create"
    const val SESSION_COMPLETE = "session_complete"
    const val SESSION_GIVE_UP = "session_give_up"
}

/**
 * Hàng đợi outbox (T-043) — 1 dòng cho mỗi thao tác phiên gọi `ApiService` thất bại lúc mất mạng
 * (xem các nhánh `runCatching{}.onFailure{}` ở [com.cozypomo.app.data.timer.TimerRepository]).
 * Chỉ lưu [localSessionId] + [type] — KHÔNG đóng băng sẵn payload gửi lên backend, vì
 * `SyncOutboxWorker` đọc lại state mới nhất từ bảng `sessions` (Room) ngay lúc flush (VD
 * `remoteId` có thể vừa được đồng bộ ở lần chạy trước đó).
 */
@Entity(tableName = "sync_outbox")
data class SyncOutboxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val localSessionId: String,
    val type: String,
    val createdAtEpochMs: Long,
)
