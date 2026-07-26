package com.cozypomo.app.data.network

import kotlinx.serialization.Serializable

@Serializable
data class BalanceResponse(val balance: Int, val focusMinutes: Int = 0)

@Serializable
data class GameSettingsDto(
    val id: Int,
    val coinsPerFocusMinute: Float = 1f,
    /** % phí giao dịch Chợ (T-106) — trừ vào phần người bán USER nhận được. */
    val marketFeePercent: Float = 10f,
    val maxActiveListingsPerUser: Int = 10,
)
