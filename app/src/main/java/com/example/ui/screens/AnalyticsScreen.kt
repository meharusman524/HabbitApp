package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.HabitIconHelper
import com.example.ui.viewmodel.AnalyticsPeriod
import com.example.ui.viewmodel.AnalyticsSummary
import com.example.ui.viewmodel.DayTrend
import com.example.ui.viewmodel.HabitPerformance
import com.example.ui.viewmodel.HeatmapCell
import java.time.format.DateTimeFormatter

@Composable
fun AnalyticsScreen(
    summary: AnalyticsSummary?,
    selectedPeriod: AnalyticsPeriod,
    onSelectPeriod: (AnalyticsPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (summary == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Loading analytics...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Surface
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Screen Header
            item {
                Column {
                    Text(
                        text = "Progress Analytics",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Consistency patterns & habit momentum over time",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Period Selector Tabs
            item {
                PeriodSelectorTabs(
                    selectedPeriod = selectedPeriod,
                    onSelectPeriod = onSelectPeriod
                )
            }

            // Key Metric Cards (2x2 Grid)
            item {
                MetricCardsGrid(summary = summary)
            }

            // Weekly Completion Trend Chart
            item {
                WeeklyTrendCard(dailyTrends = summary.dailyTrends)
            }

            // Consistency Heatmap (GitHub Style)
            item {
                HeatmapCard(heatmapCells = summary.heatmapCells)
            }

            // Habit-by-Habit Deep Dive Breakdown
            item {
                Text(
                    text = "Habit Breakdown",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (summary.habitPerformances.isEmpty()) {
                item {
                    Text(
                        text = "No active habits to analyze yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(summary.habitPerformances, key = { it.habit.id }) { performance ->
                    HabitPerformanceCard(performance = performance)
                }
            }
        }
    }
}

@Composable
private fun PeriodSelectorTabs(
    selectedPeriod: AnalyticsPeriod,
    onSelectPeriod: (AnalyticsPeriod) -> Unit
) {
    val periods = AnalyticsPeriod.entries
    val selectedIndex = periods.indexOf(selectedPeriod)

    TabRow(
        selectedTabIndex = selectedIndex,
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        contentColor = MaterialTheme.colorScheme.primary,
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                color = MaterialTheme.colorScheme.primary,
                height = 3.dp
            )
        },
        divider = {},
        modifier = Modifier.clip(RoundedCornerShape(14.dp))
    ) {
        periods.forEach { period ->
            val isSelected = period == selectedPeriod
            Tab(
                selected = isSelected,
                onClick = { onSelectPeriod(period) },
                text = {
                    Text(
                        text = period.label,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.testTag("period_tab_${period.name.lowercase()}")
            )
        }
    }
}

@Composable
private fun MetricCardsGrid(summary: AnalyticsSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricStatCard(
                title = "Consistency",
                value = "${summary.overallAdherence}%",
                subtitle = "Overall adherence",
                icon = Icons.Default.AutoGraph,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            MetricStatCard(
                title = "Active Streak",
                value = "${summary.currentActiveStreak} days",
                subtitle = "Best: ${summary.bestOverallStreak} days",
                icon = Icons.Default.LocalFireDepartment,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricStatCard(
                title = "Total Sessions",
                value = "${summary.totalCompletedSessions}",
                subtitle = "Completions logged",
                icon = Icons.Default.CheckCircle,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
            MetricStatCard(
                title = "Top Focus",
                value = summary.topCategory,
                subtitle = "Best day: ${summary.bestDayOfWeek}",
                icon = Icons.Default.Stars,
                tint = Color(0xFF0EA5E9),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetricStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(tint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun WeeklyTrendCard(dailyTrends: List<DayTrend>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Completion Trend",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Ratio of habits done",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Custom Canvas Chart with Bars
            val primaryColor = MaterialTheme.colorScheme.primary
            val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val count = dailyTrends.size
                    if (count == 0) return@Canvas
                    val spacing = size.width / count
                    val barWidth = (spacing * 0.45f).coerceAtMost(28.dp.toPx())
                    val maxHeight = size.height - 24.dp.toPx()

                    dailyTrends.forEachIndexed { index, trend ->
                        val centerX = (index * spacing) + (spacing / 2f)
                        val left = centerX - (barWidth / 2f)

                        // Draw background track
                        drawRoundRect(
                            color = trackColor,
                            topLeft = Offset(left, 0f),
                            size = Size(barWidth, maxHeight),
                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                        )

                        // Draw active bar
                        val fillHeight = (maxHeight * trend.ratio).coerceAtLeast(barWidth)
                        val top = maxHeight - fillHeight

                        if (trend.ratio > 0f) {
                            drawRoundRect(
                                color = primaryColor,
                                topLeft = Offset(left, top),
                                size = Size(barWidth, fillHeight),
                                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                            )
                        }
                    }
                }
            }

            // Labels below bars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                dailyTrends.forEach { trend ->
                    Text(
                        text = trend.dayLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HeatmapCard(heatmapCells: List<HeatmapCell>) {
    var selectedCell by remember { mutableStateOf<HeatmapCell?>(null) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Consistency Heatmap",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "Last 5 weeks",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 7 Columns Grid for Heatmap
            val primary = MaterialTheme.colorScheme.primary
            val emptyColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)

            // Render 5 rows of 7 days
            val rows = heatmapCells.chunked(7)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                rows.forEach { weekRow ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        weekRow.forEach { cell ->
                            val cellColor = when (cell.intensity) {
                                0 -> emptyColor
                                1 -> primary.copy(alpha = 0.3f)
                                2 -> primary.copy(alpha = 0.55f)
                                3 -> primary.copy(alpha = 0.8f)
                                else -> primary
                            }

                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(cellColor)
                                    .clickable { selectedCell = cell }
                                    .then(
                                        if (selectedCell == cell) {
                                            Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(8.dp))
                                        } else {
                                            Modifier
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (cell.completedCount > 0) {
                                    Text(
                                        text = "${cell.completedCount}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = if (cell.intensity >= 2) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tooltip or legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedCell != null) {
                    Text(
                        text = "${selectedCell!!.date.format(dateFormatter)}: ${selectedCell!!.completedCount} completions",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "Tap any cell for daily stats",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Legend
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Less",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    listOf(
                        emptyColor,
                        primary.copy(alpha = 0.3f),
                        primary.copy(alpha = 0.55f),
                        primary.copy(alpha = 0.8f),
                        primary
                    ).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(color)
                        )
                    }
                    Text(
                        text = "More",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HabitPerformanceCard(performance: HabitPerformance) {
    val habit = performance.habit
    val habitColor = HabitIconHelper.parseColor(habit.colorHex)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(habitColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = HabitIconHelper.getIcon(habit.iconName),
                        contentDescription = null,
                        tint = habitColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${habit.frequency.replaceFirstChar { it.uppercase() }} · ${performance.totalCompletions} total logged",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${performance.currentStreak}d",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Adherence Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "30-Day Consistency",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${performance.adherencePercent}%",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = habitColor
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { performance.adherencePercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = habitColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
        }
    }
}
