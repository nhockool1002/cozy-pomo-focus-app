package com.cozypomo.app.data.local.session

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Trạng thái đúng theo enum `SessionStatus` phía backend (Prisma): RUNNING/COMPLETED/GIVEN_UP. */
object SessionStatus {
    const val RUNNING = "RUNNING"
    const val COMPLETED = "COMPLETED"
    const val GIVEN_UP = "GIVEN_UP"
}

/**
 * Cache offline cho 1 phiên Pomodoro — nguồn sự thật cho Foreground Service đếm giờ
 * (đọc [startElapsedRealtimeMs] để tính thời gian còn lại, không lệch khi khoá màn hình).
 * [remoteId] null cho tới khi đồng bộ được `POST /sessions` lên backend (best-effort, không
 * chặn timer — outbox retry đầy đủ khi mất mạng là T-043, chưa làm ở đây).
 *
 * [ownedEggId] trứng ĐANG SỞ HỮU được chọn ấp (null = tập trung không ấp trứng nào).
 * [incubationRatio] % thời gian dành cho ấp trứng, phần còn lại vào Giờ tích luỹ — chỉ có ý
 * nghĩa khi có [ownedEggId].
 */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val remoteId: String? = null,
    val ownedEggId: String? = null,
    val incubationRatio: Float? = null,
    /** "COIN" hoặc "FOCUS_MINUTE" — phần thời gian không dành cho ấp trứng quy đổi thành CHỈ 1 loại tiền này. */
    val rewardCurrency: String = "COIN",
    val plannedMin: Int,
    val strictMode: Boolean,
    val status: String,
    val startedAtEpochMs: Long,
    val startElapsedRealtimeMs: Long,
    val endedAtEpochMs: Long? = null,
    val resultSpeciesId: String? = null,
    val resultSpeciesName: String? = null,
    val resultSpeciesRarity: String? = null,
    val coinsEarned: Int? = null,
    val minutesAccumulated: Int? = null,
    val hatched: Boolean = false,
    val clientEventId: String,
)
