package com.cozypomo.app.data.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** T-123 — Hộp thư. `type`: EGG_RECEIVED / EGG_HATCHED / ADMIN_GIFT. */
@Serializable
data class InboxMessageDto(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val payload: JsonObject? = null,
    val isRead: Boolean,
    val createdAt: String,
)

@Serializable
data class UnreadCountResponse(val count: Int)
