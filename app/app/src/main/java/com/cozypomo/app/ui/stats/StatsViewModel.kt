package com.cozypomo.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cozypomo.app.data.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** 1 ngày trong biểu đồ tuần — luôn đủ 7 phần tử (kể cả ngày không có phiên nào, điền 0) vì
 * backend chỉ trả về ngày có dữ liệu (xem StatsService.getRange), khác với nhu cầu vẽ biểu đồ
 * cần đủ trục ngày liên tục. */
data class DailyStatEntry(
    val date: LocalDate,
    val totalFocusMinutes: Int,
    val completedCount: Int,
    val givenUpCount: Int,
)

data class StatsUiState(
    val loading: Boolean = true,
    val streak: Int = 0,
    val totalFocusMinutesAllTime: Int = 0,
    val days: List<DailyStatEntry> = emptyList(),
) {
    val weekCompletedCount: Int get() = days.sumOf { it.completedCount }
    val weekGivenUpCount: Int get() = days.sumOf { it.givenUpCount }
}

/** S-06 — Thống kê (T-038). Tải `/stats/range` (7 ngày gần nhất, theo ngày local của máy — không
 * cần khớp tuyệt đối UTC với backend, chấp nhận sai lệch nhỏ gần nửa đêm) + `/stats/summary`
 * (streak + tổng phút tập trung mọi thời điểm). */
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val apiService: ApiService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            val today = LocalDate.now()
            val start = today.minusDays(6)
            val rangeResult = runCatching { apiService.getStatsRange(start.toString(), today.toString()) }
            val summaryResult = runCatching { apiService.getStatsSummary() }
            val byDate = rangeResult.getOrNull()?.associateBy { it.date } ?: emptyMap()

            val days = (0..6).map { offset ->
                val date = start.plusDays(offset.toLong())
                val entry = byDate[date.toString()]
                DailyStatEntry(
                    date = date,
                    totalFocusMinutes = entry?.totalFocusMinutes ?: 0,
                    completedCount = entry?.completedCount ?: 0,
                    givenUpCount = entry?.givenUpCount ?: 0,
                )
            }

            _uiState.update { state ->
                state.copy(
                    loading = false,
                    days = days,
                    streak = summaryResult.getOrNull()?.streak ?: state.streak,
                    totalFocusMinutesAllTime = summaryResult.getOrNull()?.totalFocusMinutes ?: state.totalFocusMinutesAllTime,
                )
            }
        }
    }
}
