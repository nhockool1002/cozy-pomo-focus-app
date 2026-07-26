package com.cozypomo.app.data.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** Khớp `SyncEventDto` phía backend (`backend/src/sync/dto/sync-batch.dto.ts`) — `type` là 1
 * trong [com.cozypomo.app.data.local.sync.SyncEventType], `payload` tuỳ theo `type`. */
@Serializable
data class SyncEventRequest(
    val clientEventId: String,
    val type: String,
    val payload: JsonObject,
)

@Serializable
data class SyncBatchRequest(
    val events: List<SyncEventRequest>,
)

/** Khớp `SyncResult` phía backend — `data` tuỳ hình dạng theo `type` (VD `session_create` trả về
 * Session có field `id`), không có 1 kiểu chung an toàn nên giữ nguyên [JsonElement] thô. */
@Serializable
data class SyncResultDto(
    val clientEventId: String,
    val status: String,
    val data: JsonElement? = null,
    val error: String? = null,
)
