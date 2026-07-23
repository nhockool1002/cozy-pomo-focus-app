package com.cozypomo.app.data.timer

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.content.ContextCompat
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

data class HatchResult(
    val speciesName: String?,
    val speciesRarity: String?,
    val coinsEarned: Int,
)

sealed interface SessionUiState {
    data object Idle : SessionUiState
    data class Running(
        val sessionId: String,
        val eggTypeId: String,
        val plannedMin: Int,
        val remainingMs: Long,
        val totalMs: Long,
    ) : SessionUiState
}

/**
 * TimerRepository (FR01/FR03, mục 3.1 docs/technical-spec.md) — lõi của app.
 * Room là nguồn sự thật cho phiên đang chạy (đọc bởi cả UI lẫn Foreground Service);
 * backend chỉ đồng bộ best-effort (không có outbox retry đầy đủ — đó là T-043).
 */
@Singleton
class TimerRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionDao: SessionDao,
    private val apiService: ApiService,
) {
    private val _hatchEvents = MutableSharedFlow<HatchResult>(extraBufferCapacity = 1)
    val hatchEvents: SharedFlow<HatchResult> = _hatchEvents.asSharedFlow()

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
        return SessionUiState.Running(id, eggTypeId, plannedMin, remaining, totalMs)
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

    suspend fun startSession(durationMin: Int, eggTypeId: String, strictMode: Boolean): String {
        val id = UUID.randomUUID().toString()
        val clientEventId = UUID.randomUUID().toString()
        val entity = SessionEntity(
            id = id,
            eggTypeId = eggTypeId,
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
            apiService.createSession(CreateSessionRequest(eggTypeId, durationMin, strictMode, clientEventId))
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

    /** Gọi khi đếm về 0 (từ Foreground Service) — chấm dứt phiên, roll trứng, trả kết quả. */
    suspend fun completeSession(sessionId: String): HatchResult {
        var entity = sessionDao.getById(sessionId) ?: error("Không tìm thấy phiên $sessionId")

        if (entity.remoteId == null) {
            runCatching {
                apiService.createSession(
                    CreateSessionRequest(entity.eggTypeId, entity.plannedMin, entity.strictMode, entity.clientEventId),
                )
            }.onSuccess {
                entity = entity.copy(remoteId = it.id)
                sessionDao.upsert(entity)
            }
        }

        val response = entity.remoteId?.let { remoteId ->
            runCatching { apiService.completeSession(remoteId, CompleteSessionRequest(entity.clientEventId)) }.getOrNull()
        }

        val coinsEarned = response?.coinsEarned ?: entity.plannedMin
        val updated = entity.copy(
            status = SessionStatus.COMPLETED,
            endedAtEpochMs = System.currentTimeMillis(),
            resultSpeciesId = response?.resultSpecies?.id,
            resultSpeciesName = response?.resultSpecies?.name,
            resultSpeciesRarity = response?.resultSpecies?.rarity,
            coinsEarned = coinsEarned,
        )
        sessionDao.upsert(updated)
        stopForegroundService()

        val result = HatchResult(updated.resultSpeciesName, updated.resultSpeciesRarity, coinsEarned)
        _hatchEvents.emit(result)
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
