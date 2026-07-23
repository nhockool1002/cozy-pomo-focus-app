package com.cozypomo.app.data.network

import kotlinx.serialization.Serializable

@Serializable
data class CreateSessionRequest(
    val eggTypeId: String,
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
    val eggTypeId: String,
    val plannedMin: Int,
    val strictMode: Boolean,
    val status: String,
    val startedAt: String,
    val endedAt: String? = null,
    val resultSpeciesId: String? = null,
    val coinsEarned: Int? = null,
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
    val resultSpecies: SpeciesDto? = null,
    val coinsEarned: Int,
)
