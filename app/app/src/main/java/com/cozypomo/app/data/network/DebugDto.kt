package com.cozypomo.app.data.network

import kotlinx.serialization.Serializable

/** Chỉ dùng cho bubble cheat của tài khoản tester (xem DebugController phía backend). */
@Serializable
data class GrantCurrencyRequest(val currency: String, val amount: Int)

@Serializable
data class GrantEggRequest(val eggTypeName: String? = null)

@Serializable
data class GrantSpeciesRequest(val rarity: String)
