package com.cozypomo.app.data.network

import kotlinx.serialization.Serializable

@Serializable
data class CreateSessionRequest(
    val ownedEggId: String? = null,
    val incubationRatio: Float? = null,
    val rewardCurrency: String? = null,
    val plannedMin: Int,
    val strictMode: Boolean,
    val clientEventId: String? = null,
)

@Serializable
data class CompleteSessionRequest(
    val clientEventId: String? = null,
)

@Serializable
data class SessionDto(
    val id: String,
    val userId: String,
    val ownedEggId: String? = null,
    val incubationRatio: Float? = null,
    val rewardCurrency: String? = null,
    val plannedMin: Int,
    val strictMode: Boolean,
    val status: String,
    val startedAt: String,
    val endedAt: String? = null,
    val coinsEarned: Int? = null,
    val minutesAccumulated: Int? = null,
    val minutesIncubated: Int? = null,
    val clientEventId: String? = null,
)

@Serializable
data class SpeciesDto(
    val id: String,
    val name: String,
    val category: String,
    val archetype: String,
    val paletteIdx: Int,
    val rarity: String,
    val lore: String? = null,
    val isActive: Boolean = true,
)

@Serializable
data class CompleteSessionResponse(
    val session: SessionDto,
    val coinsEarned: Int,
    val minutesAccumulated: Int,
    val ownedEgg: OwnedEggDto? = null,
    val resultSpecies: SpeciesDto? = null,
    val hatched: Boolean = false,
)
