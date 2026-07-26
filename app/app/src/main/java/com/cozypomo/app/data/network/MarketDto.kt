package com.cozypomo.app.data.network

import kotlinx.serialization.Serializable

/** Khớp `MarketListing` phía backend (T-106) — `itemType` quyết định `species` hay `ownedEgg`
 * có giá trị, `status` gồm `PENDING_APPROVAL`/`ACTIVE`/`SOLD`/`CANCELLED`/`REJECTED`. */
@Serializable
data class MarketListingDto(
    val id: String,
    val itemType: String,
    val sellerType: String,
    val sellerId: String? = null,
    val speciesId: String? = null,
    val species: SpeciesDto? = null,
    val ownedEggId: String? = null,
    val ownedEgg: OwnedEggDto? = null,
    val priceCoin: Int,
    val status: String,
    val buyerId: String? = null,
    val createdAt: String,
    val soldAt: String? = null,
    val cancelledAt: String? = null,
    val seller: MarketSellerDto? = null,
)

@Serializable
data class MarketSellerDto(
    val id: String,
    val email: String,
    val displayName: String? = null,
)

@Serializable
data class CreateSpeciesListingRequest(
    val speciesId: String,
    val priceCoin: Int,
    val clientEventId: String? = null,
)

@Serializable
data class CreateEggListingRequest(
    val ownedEggId: String,
    val priceCoin: Int,
    val clientEventId: String? = null,
)

@Serializable
data class BuyListingRequest(
    val clientEventId: String? = null,
)
