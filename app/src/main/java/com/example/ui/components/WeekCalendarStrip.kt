package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HabitLog
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@Composable
fun WeekCalendarStrip(
    selectedDate: LocalDate,
    logs: List<HabitLog>,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }
    val completedDates = remember(logs) {
        logs.filter { it.completed }.map { it.date }.toSet()
    }

    // Determine the Monday of the current selected date's week
    val startOfWeek = remember(selectedDate) {
        selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }

    val daysOfWeek = remember(startOfWeek) {
        (0 until 7).map { startOfWeek.plusDays(it.toLong()) }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                RoundedCornerShape(20.dp)
            )
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        daysOfWeek.forEach { date ->
            val isSelected = date == selectedDate
            val isToday = date == LocalDate.now()
            val hasCompletions = completedDates.contains(date.format(dateFormatter))

            val backgroundColor by animateColorAsState(
                targetValue = when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    else -> Color.Transparent
                },
                label = "day_bg"
            )

            val textColor by animateColorAsState(
                targetValue = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isToday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                },
                label = "day_text"
            )

            val labelColor by animateColorAsState(
                targetValue = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                label = "day_label"
            )

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(backgroundColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onDateSelected(date)
                    }
                    .padding(vertical = 8.dp, horizontal = 10.dp)
                    .testTag("calendar_day_${date.dayOfMonth}"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = date.dayOfWeek.name.take(3),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = labelColor
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${date.dayOfMonth}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.SemiBold
                    ),
                    color = textColor
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Completion dot
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                hasCompletions && isSelected -> MaterialTheme.colorScheme.onPrimary
                                hasCompletions -> MaterialTheme.colorScheme.primary
                                else -> Color.Transparent
                            }
                        )
                )
            }
        }
    }
}
