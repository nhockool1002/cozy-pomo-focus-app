package com.cozypomo.app.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate

private val ChartBarMaxHeight = 120.dp

/** S-06 — Thống kê (T-038): streak, tổng phút tập trung (mọi thời điểm), biểu đồ 7 ngày gần nhất. */
@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text("Thống kê", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Tiến độ tập trung của bạn",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (uiState.loading && uiState.days.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                StreakCard(streak = uiState.streak)

                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.CheckCircle,
                        tint = MaterialTheme.colorScheme.primary,
                        value = uiState.weekCompletedCount.toString(),
                        label = "Hoàn thành (7 ngày)",
                    )
                    StatTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Cancel,
                        tint = MaterialTheme.colorScheme.error,
                        value = uiState.weekGivenUpCount.toString(),
                        label = "Bỏ cuộc (7 ngày)",
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Phút tập trung theo ngày", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Tổng cộng ${uiState.totalFocusMinutesAllTime} phút từ trước tới nay",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        WeeklyBarChart(days = uiState.days)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun StreakCard(streak: Int) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = CircleShape, color = Color(0xFFFF8A3D).copy(alpha = 0.18f), modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = Color(0xFFFF8A3D))
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = if (streak > 0) "$streak ngày liên tiếp" else "Chưa có streak",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Hoàn thành ít nhất 1 phiên mỗi ngày để giữ streak",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatTile(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, value: String, label: String, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = tint)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WeeklyBarChart(days: List<DailyStatEntry>) {
    val maxMinutes = remember(days) { (days.maxOfOrNull { it.totalFocusMinutes } ?: 0).coerceAtLeast(1) }
    val today = remember { LocalDate.now() }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        days.forEach { entry ->
            val fraction = (entry.totalFocusMinutes / maxMinutes.toFloat()).coerceIn(0f, 1f)
            val barHeight: Dp = if (entry.totalFocusMinutes > 0) (ChartBarMaxHeight * fraction).coerceAtLeast(6.dp) else 2.dp

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (entry.totalFocusMinutes > 0) "${entry.totalFocusMinutes}" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier.height(ChartBarMaxHeight).width(24.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .height(barHeight)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(
                                if (entry.date == today) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            ),
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = weekdayAbbreviation(entry.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (entry.date == today) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

private fun weekdayAbbreviation(date: LocalDate): String = when (date.dayOfWeek) {
    DayOfWeek.MONDAY -> "T2"
    DayOfWeek.TUESDAY -> "T3"
    DayOfWeek.WEDNESDAY -> "T4"
    DayOfWeek.THURSDAY -> "T5"
    DayOfWeek.FRIDAY -> "T6"
    DayOfWeek.SATURDAY -> "T7"
    DayOfWeek.SUNDAY -> "CN"
}
