package com.cozypomo.app.data.network

import kotlinx.serialization.Serializable

@Serializable
data class BalanceResponse(val balance: Int, val focusMinutes: Int = 0)

@Serializable
data class GameSettingsDto(val id: Int, val coinsPerFocusMinute: Float = 1f)
