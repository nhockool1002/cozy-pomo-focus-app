package com.cozypomo.app.data.network

import kotlinx.serialization.Serializable

/** 1 ngày trong `GET /stats/range` — backend chỉ trả ngày CÓ phiên nào đó (xem StatsService.getRange),
 * ngày không có phiên bị bỏ qua hoàn toàn khỏi mảng kết quả, phía app tự điền 0 cho ngày trống. */
@Serializable
data class DailyStatsDto(
    val date: String,
    val totalFocusMinutes: Int,
    val completedCount: Int,
    val givenUpCount: Int,
)

@Serializable
data class StatsSummaryDto(
    val streak: Int,
    val totalFocusMinutes: Int,
)

/** 1 dòng trong `GET /stats/by-label` — [label] null = phiên không gắn nhãn ("Chưa gắn nhãn"),
 * chỉ tính phiên COMPLETED (xem StatsService.getByLabel). */
@Serializable
data class LabelStatsDto(
    val label: String? = null,
    val totalFocusMinutes: Int,
)
