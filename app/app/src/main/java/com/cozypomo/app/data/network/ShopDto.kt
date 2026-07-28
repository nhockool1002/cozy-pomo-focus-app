package com.cozypomo.app.data.network

import kotlinx.serialization.Serializable

@Serializable
data class ShopItemDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val category: String, // EGG | JAR_SKIN | MUSIC | BOOST
    val priceCoin: Int,
    val isActive: Boolean = true,
    /** T-116 — false = trứng Truyền thuyết: hiện trong Cửa hàng nhưng không bán, chỉ Admin phát. */
    val purchasable: Boolean = true,
    val eggTypeId: String? = null,
    val eggType: EggTypeDto? = null,
    /** Chỉ có ý nghĩa khi category = BOOST — "FOCUS_MINUTES" hoặc "HATCH_MINUTES". */
    val boostType: String? = null,
    /** Số phút cộng thêm khi dùng (Giờ tích luỹ nếu FOCUS_MINUTES, phút ấp nếu HATCH_MINUTES). */
    val boostAmount: Int? = null,
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

@Serializable
data class UseItemRequest(
    /** Bắt buộc khi dùng vật phẩm bổ trợ HATCH_MINUTES — id trứng đang ấp để cộng thêm phút. */
    val ownedEggId: String? = null,
)

@Serializable
data class UseItemResponse(
    val kind: String, // FOCUS_MINUTES | HATCH_MINUTES
    val amount: Int,
    val ownedEgg: OwnedEggDto? = null,
    val resultSpecies: SpeciesDto? = null,
    val hatched: Boolean = false,
)
