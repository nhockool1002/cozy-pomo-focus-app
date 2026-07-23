package com.cozypomo.app.data.network

import kotlinx.serialization.Serializable

@Serializable
data class CollectionEntryDto(
    val id: String,
    val userId: String,
    val speciesId: String,
    val hatchCount: Int,
    val isFavorite: Boolean = false,
    val firstHatchedAt: String,
    val lastHatchedAt: String,
    val species: SpeciesDto,
)

@Serializable
data class CollectionProgressDto(
    val unlocked: Int,
    val total: Int,
)

/**
 * `PATCH /collection/{speciesId}/favorite` trả về `collectionEntry.update(...)` KHÔNG kèm
 * `include: { species: true }` (khác `GET /collection` có include) — nên không có field `species`.
 * Dùng DTO nhỏ riêng khớp đúng response thật, thay vì tái dùng [CollectionEntryDto] (sẽ ném lỗi
 * thiếu field bắt buộc `species` nếu strict-decode).
 */
@Serializable
data class FavoriteToggleResponseDto(
    val id: String,
    val speciesId: String,
    val hatchCount: Int,
    val isFavorite: Boolean = false,
)
