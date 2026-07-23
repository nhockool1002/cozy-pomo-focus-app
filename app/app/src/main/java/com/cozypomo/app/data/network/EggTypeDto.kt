package com.cozypomo.app.data.network

import kotlinx.serialization.Serializable

@Serializable
data class EggTypeDto(
    val id: String,
    val name: String,
    val colorHex: String,
    val priceCoin: Int,
)
