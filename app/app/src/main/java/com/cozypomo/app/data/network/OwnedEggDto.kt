package com.cozypomo.app.data.network

import kotlinx.serialization.Serializable

@Serializable
data class OwnedEggDto(
    val id: String,
    val userId: String,
    val eggTypeId: String,
    val status: String,
    val incubatedMin: Int,
    val acquiredAt: String,
    val hatchedAt: String? = null,
    val resultSpeciesId: String? = null,
    val eggType: EggTypeDto,
    val resultSpecies: SpeciesDto? = null,
)
