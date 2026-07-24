package com.cozypomo.app.data.timer

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.cozypomo.app.data.events.CollectionEventBus
import com.cozypomo.app.data.local.session.SessionDao
import com.cozypomo.app.data.local.session.SessionEntity
import com.cozypomo.app.data.local.session.SessionStatus
import com.cozypomo.app.data.network.ApiService
import com.cozypomo.app.data.network.CompleteSessionRequest
import com.cozypomo.app.data.network.CreateSessionRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Kết quả 1 phiên hoàn thành — 3 trường hợp tuỳ có ấp trứng hay không, và có vừa nở hay không. */
sealed interface SessionCompletionResult {
    data class Hatched(
        val speciesName: String?,
        val speciesRarity: String?,
        val speciesCategory: String?,
        val speciesArchetype: String?,
        val speciesPaletteIdx: Int?,
        val coinsEarned: Int,
        val minutesAccumulated: Int,
    ) : SessionCompletionResult

    data class Incubating(
        val eggTypeName: String?,
        val incubatedMin: Int,
        val hatchDurationMin: Int,
        val coinsEarned: Int,
        val minutesAccumulated: Int,
    ) : SessionCompletionResult

    data class NoEgg(val coinsEarned: Int, val minutesAccumulated: Int) : SessionCompletionResult
}

sealed interface SessionUiState {
    data object Idle : SessionUiState
    data class Running(
        val sessionId: String,
        val ownedEggId: String?,
        val plannedMin: Int,
        val remainingMs: Long,
        val totalMs: Long,
    ) : SessionUiState
}

/**
 * TimerRepository (FR01/FR03, mục 3.1 docs/technical-spec.md) — lõi của app.
 * Room là nguồn sự thật cho phiên đang chạy (đọc bởi cả UI lẫn Foreground Service);
 * backend chỉ đồng bộ best-effort (không có outbox retry đầy đủ — đó là T-043).
 *
 * Mỗi phiên hoàn thành chia phút giữa Giờ tích luỹ và trứng đang ấp (nếu có) theo
 * [incubationRatio] — logic chia % + roll loài khi đủ ngưỡng nằm ở backend
 * (SessionsService.complete), app chỉ hiển thị kết quả trả về.
 */
@Singleton
class TimerRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionDao: SessionDao,
    private val apiService: ApiService,
    private val collectionEventBus: CollectionEventBus,
) {
    private val _completionEvents = MutableSharedFlow<SessionCompletionResult>(extraBufferCapacity = 1)
    val completionEvents: SharedFlow<SessionCompletionResult> = _completionEvents.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeActiveSession(): Flow<SessionUiState> =
        sessionDao.observeActiveSession().flatMapLatest { entity ->
            if (entity == null) {
                flowOf(SessionUiState.Idle)
            } else {
                secondTicker().map { entity.toRunningState() }
            }
        }

    private fun secondTicker(): Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(1000)
        }
    }

    private fun SessionEntity.toRunningState(): SessionUiState {
        val totalMs = plannedMin * 60_000L
        val elapsed = SystemClock.elapsedRealtime() - startElapsedRealtimeMs
        val remaining = (totalMs - elapsed).coerceAtLeast(0)
        return SessionUiState.Running(id, ownedEggId, plannedMin, remaining, totalMs)
    }

    /**
     * Gọi khi Home mở lại — nếu Foreground Service đã bị hệ thống kill giữa chừng nhưng
     * Room vẫn còn phiên RUNNING, khởi động lại service để tiếp tục đếm chính xác từ
     * mốc thời gian đã lưu (không mất tiến trình).
     */
    suspend fun ensureServiceRunningIfActive() {
        val entity = sessionDao.getActiveOnce() ?: return
        startForegroundService(entity.id)
    }

    /** [Chỉ dùng cho bubble cheat tester] Kéo phiên đang chạy về gần cuối (còn ~5s) để test
     * nhận thưởng nhanh, không cần đợi hết thời gian thật. Không đổi gì phía backend. */
    suspend fun debugFastForwardActiveSession() {
        val entity = sessionDao.getActiveOnce() ?: return
        val totalMs = entity.plannedMin * 60_000L
        val remainingTargetMs = 5_000L
        val newStart = SystemClock.elapsedRealtime() - (totalMs - remainingTargetMs)
        sessionDao.upsert(entity.copy(startElapsedRealtimeMs = newStart))
    }

    suspend fun startSession(
        durationMin: Int,
        ownedEggId: String?,
        incubationRatio: Float?,
        rewardCurrency: String,
        strictMode: Boolean,
    ): String {
        val id = UUID.randomUUID().toString()
        val clientEventId = UUID.randomUUID().toString()
        val entity = SessionEntity(
            id = id,
            ownedEggId = ownedEggId,
            incubationRatio = ownedEggId?.let { incubationRatio ?: 1f },
            rewardCurrency = rewardCurrency,
            plannedMin = durationMin,
            strictMode = strictMode,
            status = SessionStatus.RUNNING,
            startedAtEpochMs = System.currentTimeMillis(),
            startElapsedRealtimeMs = SystemClock.elapsedRealtime(),
            clientEventId = clientEventId,
        )
        sessionDao.upsert(entity)
        startForegroundService(id)

        runCatching {
            apiService.createSession(
                CreateSessionRequest(ownedEggId, entity.incubationRatio, rewardCurrency, durationMin, strictMode, clientEventId),
            )
        }.onSuccess { remote ->
            sessionDao.upsert(entity.copy(remoteId = remote.id))
        }
        return id
    }

    suspend fun giveUpSession(sessionId: String) {
        val entity = sessionDao.getById(sessionId) ?: return
        sessionDao.upsert(entity.copy(status = SessionStatus.GIVEN_UP, endedAtEpochMs = System.currentTimeMillis()))
        stopForegroundService()
        entity.remoteId?.let { remoteId ->
            runCatching { apiService.giveUpSession(remoteId) }
        }
    }

    /** Gọi khi đếm về 0 (từ Foreground Service) — chấm dứt phiên, ấp/nở trứng, trả kết quả. */
    suspend fun completeSession(sessionId: String): SessionCompletionResult {
        var entity = sessionDao.getById(sessionId) ?: error("Không tìm thấy phiên $sessionId")

        if (entity.remoteId == null) {
            runCatching {
                apiService.createSession(
                    CreateSessionRequest(
                        entity.ownedEggId,
                        entity.incubationRatio,
                        entity.rewardCurrency,
                        entity.plannedMin,
                        entity.strictMode,
                        entity.clientEventId,
                    ),
                )
            }.onSuccess {
                entity = entity.copy(remoteId = it.id)
                sessionDao.upsert(entity)
            }
        }

        val response = entity.remoteId?.let { remoteId ->
            runCatching { apiService.completeSession(remoteId, CompleteSessionRequest(entity.clientEventId)) }.getOrNull()
        }

        // Offline/lỗi mạng: không biết chia % ra sao ở backend — coi như toàn bộ quy đổi thành
        // đúng 1 loại tiền theo lựa chọn rewardCurrency của người dùng, không ấp trứng, giữ trải
        // nghiệm không bị "treo" dù mất mạng đúng lúc hết giờ.
        val coinsEarned = response?.coinsEarned
            ?: if (entity.rewardCurrency == "FOCUS_MINUTE") 0 else entity.plannedMin * 10
        val minutesAccumulated = response?.minutesAccumulated
            ?: if (entity.rewardCurrency == "FOCUS_MINUTE") entity.plannedMin else 0

        val updated = entity.copy(
            status = SessionStatus.COMPLETED,
            endedAtEpochMs = System.currentTimeMillis(),
            resultSpeciesId = response?.resultSpecies?.id,
            resultSpeciesName = response?.resultSpecies?.name,
            resultSpeciesRarity = response?.resultSpecies?.rarity,
            coinsEarned = coinsEarned,
            minutesAccumulated = minutesAccumulated,
            hatched = response?.hatched ?: false,
        )
        sessionDao.upsert(updated)
        stopForegroundService()

        val result: SessionCompletionResult = when {
            response?.hatched == true -> SessionCompletionResult.Hatched(
                speciesName = response.resultSpecies?.name,
                speciesRarity = response.resultSpecies?.rarity,
                speciesCategory = response.resultSpecies?.category,
                speciesArchetype = response.resultSpecies?.archetype,
                speciesPaletteIdx = response.resultSpecies?.paletteIdx,
                coinsEarned = coinsEarned,
                minutesAccumulated = minutesAccumulated,
            )
            response?.ownedEgg != null -> SessionCompletionResult.Incubating(
                eggTypeName = response.ownedEgg.eggType.name,
                incubatedMin = response.ownedEgg.incubatedMin,
                hatchDurationMin = response.ownedEgg.eggType.hatchDurationMin,
                coinsEarned = coinsEarned,
                minutesAccumulated = minutesAccumulated,
            )
            else -> SessionCompletionResult.NoEgg(coinsEarned, minutesAccumulated)
        }
        _completionEvents.emit(result)
        // Báo cho Khu rừng/Kho Trứng (nếu đang mở ở tab khác) tự tải lại — nếu không, tiến
        // trình ấp/số loài mở khoá vẫn hiện giá trị CŨ cho tới khi người dùng rời rồi quay
        // lại tab đó, y hệt lỗi cheat-bubble đã sửa ở T-089 nhưng cho trường hợp hoàn thành
        // phiên thật.
        if (response?.ownedEgg != null || response?.hatched == true) {
            collectionEventBus.notifyChanged()
        }
        return result
    }

    private fun startForegroundService(sessionId: String) {
        val intent = Intent(context, TimerForegroundService::class.java)
            .putExtra(TimerForegroundService.EXTRA_SESSION_ID, sessionId)
        ContextCompat.startForegroundService(context, intent)
    }

    private fun stopForegroundService() {
        context.stopService(Intent(context, TimerForegroundService::class.java))
    }
}
