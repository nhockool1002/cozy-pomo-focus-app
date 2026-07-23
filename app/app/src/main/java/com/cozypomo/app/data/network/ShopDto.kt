package com.cozypomo.app.data.network

import kotlinx.serialization.Serializable

@Serializable
data class ShopItemDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val category: String, // EGG | JAR_SKIN | MUSIC
    val priceCoin: Int,
    val isActive: Boolean = true,
    val eggTypeId: String? = null,
    val eggType: EggTypeDto? = null,
)

@Serializable
data class InventoryItemDto(
    val id: String,
    val userId: String,
    val shopItemId: String,
    val quantity: Int,
    val acquiredAt: String,
    val equipped: Boolean = false,
    val shopItem: ShopItemDto,
)

@Serializable
data class PurchaseRequest(
    val clientEventId: String? = null,
    /** Chỉ áp dụng cho vật phẩm EGG — COIN hoặc FOCUS_MINUTE. Vật phẩm khác luôn trả bằng COIN. */
    val payWith: String? = null,
)
