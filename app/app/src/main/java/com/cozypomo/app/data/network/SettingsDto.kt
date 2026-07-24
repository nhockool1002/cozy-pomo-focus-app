package com.cozypomo.app.data.network

import kotlinx.serialization.Serializable

@Serializable
data class SettingsDto(
    val userId: String,
    val focusMinutes: Int,
    val breakMinutes: Int,
    val strictModeEnabled: Boolean,
    val soundTheme: String,
)

/** Mọi field optional — PATCH /settings chỉ ghi đè field nào được gửi lên (xem UpdateSettingsDto ở backend). */
@Serializable
data class UpdateSettingsRequest(
    val focusMinutes: Int? = null,
    val breakMinutes: Int? = null,
    val strictModeEnabled: Boolean? = null,
    val soundTheme: String? = null,
)
