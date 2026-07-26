package com.cozypomo.app.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cozypomo.app.data.local.session.SessionDao
import com.cozypomo.app.data.local.sync.SyncEventType
import com.cozypomo.app.data.local.sync.SyncOutboxDao
import com.cozypomo.app.data.local.sync.SyncOutboxEntity
import com.cozypomo.app.data.network.ApiService
import com.cozypomo.app.data.network.SyncBatchRequest
import com.cozypomo.app.data.network.SyncEventRequest
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * T-043 — xả hàng đợi outbox (`sync_outbox`, xem [com.cozypomo.app.data.local.sync.SyncOutboxEntity])
 * lên `POST /sync/batch` khi có mạng (ràng buộc `NetworkType.CONNECTED` ở [SyncOutboxScheduler]).
 *
 * Đọc lại state MỚI NHẤT từ bảng `sessions` (Room) ngay lúc chạy thay vì đóng băng payload tại thời
 * điểm enqueue — quan trọng cho trường hợp `session_complete`/`session_give_up` cần
 * [remoteId][com.cozypomo.app.data.local.session.SessionEntity.remoteId] thật của phiên (backend
 * không hỗ trợ tra theo `clientEventId` cho 2 thao tác này, chỉ `session_create` mới idempotent
 * theo `clientEventId`) — nếu dòng `session_create` cùng phiên vẫn chưa xử lý xong (cũng đang mất
 * mạng), tạm bỏ qua dòng phụ thuộc này ở lượt chạy này, đợi lượt sau khi `remoteId` đã có.
 */
@HiltWorker
class SyncOutboxWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncOutboxDao: SyncOutboxDao,
    private val sessionDao: SessionDao,
    private val apiService: ApiService,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val rows = syncOutboxDao.getAll()
        if (rows.isEmpty()) return@withContext Result.success()

        val resolved = mutableListOf<Pair<SyncOutboxEntity, SyncEventRequest>>()
        for (row in rows) {
            val entity = sessionDao.getById(row.localSessionId)
            if (entity == null) {
                syncOutboxDao.delete(row) // không còn phiên gốc (không nên xảy ra) — dọn dòng rác
                continue
            }
            when (row.type) {
                SyncEventType.SESSION_CREATE -> {
                    if (entity.remoteId != null) {
                        // Đã đồng bộ bằng đường khác (VD TimerRepository tự retry ngay trong
                        // completeSession) — dọn dòng outbox thừa, không gửi lại.
                        syncOutboxDao.delete(row)
                        continue
                    }
                    resolved += row to SyncEventRequest(
                        clientEventId = entity.clientEventId,
                        type = row.type,
                        payload = buildJsonObject {
                            entity.ownedEggId?.let { put("ownedEggId", it) }
                            entity.incubationRatio?.let { put("incubationRatio", it) }
                            put("rewardCurrency", entity.rewardCurrency)
                            put("plannedMin", entity.plannedMin)
                            put("strictMode", entity.strictMode)
                        },
                    )
                }
                SyncEventType.SESSION_COMPLETE, SyncEventType.SESSION_GIVE_UP -> {
                    val remoteId = entity.remoteId ?: continue // đợi session_create xong đã
                    resolved += row to SyncEventRequest(
                        clientEventId = entity.clientEventId,
                        type = row.type,
                        payload = buildJsonObject { put("sessionId", remoteId) },
                    )
                }
                else -> syncOutboxDao.delete(row) // loại lạ (không nên xảy ra) — dọn luôn
            }
        }

        if (resolved.isEmpty()) {
            // Còn dòng trong outbox nhưng chưa dòng nào gửi được lượt này (VD toàn bộ đang chờ
            // session_create) — thử lại sau, không coi là lỗi.
            return@withContext if (syncOutboxDao.getAll().isEmpty()) Result.success() else Result.retry()
        }

        val results = runCatching { apiService.syncBatch(SyncBatchRequest(resolved.map { it.second })) }
            .getOrElse { return@withContext Result.retry() }
        val resultByClientEventId = results.associateBy { it.clientEventId }

        for ((row, request) in resolved) {
            val result = resultByClientEventId[request.clientEventId] ?: continue
            if (result.status == "ok" && row.type == SyncEventType.SESSION_CREATE) {
                val remoteId = runCatching { result.data?.jsonObject?.get("id")?.jsonPrimitive?.content }.getOrNull()
                if (remoteId != null) {
                    sessionDao.getById(row.localSessionId)?.let { entity ->
                        sessionDao.upsert(entity.copy(remoteId = remoteId))
                    }
                }
            }
            // "ok" hoặc "error" đều coi là xử lý xong — lỗi phía backend (VD phiên không hợp lệ)
            // gửi lại mãi cũng không tự hết, xoá khỏi outbox để không kẹt hàng đợi vĩnh viễn.
            syncOutboxDao.delete(row)
        }

        if (syncOutboxDao.getAll().isEmpty()) Result.success() else Result.retry()
    }
}
