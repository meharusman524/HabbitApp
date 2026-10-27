package com.example.ui.viewmodel

import com.example.data.model.Habit
import com.example.data.model.HabitLog
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

enum class AnalyticsPeriod(val label: String, val days: Long) {
    WEEK("7 Days", 7L),
    MONTH("30 Days", 30L),
    ALL_TIME("All Time", 90L)
}

data class DayTrend(
    val date: LocalDate,
    val dayLabel: String,
    val completedCount: Int,
    val totalCount: Int,
    val ratio: Float
)

data class HeatmapCell(
    val date: LocalDate,
    val dateString: String,
    val completedCount: Int,
    val intensity: Int // 0 (none) to 4 (maximum)
)

data class HabitPerformance(
    val habit: Habit,
    val currentStreak: Int,
    val bestStreak: Int,
    val totalCompletions: Int,
    val adherencePercent: Int
)

data class AnalyticsSummary(
    val overallAdherence: Int,
    val currentActiveStreak: Int,
    val bestOverallStreak: Int,
    val totalCompletedSessions: Int,
    val todayCompletedCount: Int,
    val todayTotalCount: Int,
    val todayAdherence: Int,
    val bestDayOfWeek: String,
    val topCategory: String,
    val dailyTrends: List<DayTrend>,
    val heatmapCells: List<HeatmapCell>,
    val habitPerformances: List<HabitPerformance>,
    val categoryShares: Map<String, Int>
)

object HabitAnalyticsCalculator {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun calculate(
        habits: List<Habit>,
        logs: List<HabitLog>,
        today: LocalDate = LocalDate.now(),
        period: AnalyticsPeriod = AnalyticsPeriod.WEEK
    ): AnalyticsSummary {
        val activeHabits = habits.filter { !it.isArchived }
        val logsByDate = logs.filter { it.completed }.groupBy { it.date }
        val logsByHabit = logs.filter { it.completed }.groupBy { it.habitId }

        // 1. Today's stats
        val todayStr = today.format(dateFormatter)
        val todayLogs = logsByDate[todayStr] ?: emptyList()
        val todayCompletedIds = todayLogs.map { it.habitId }.toSet()
        val todayCompletedCount = activeHabits.count { todayCompletedIds.contains(it.id) }
        val todayTotalCount = activeHabits.size
        val todayAdherence = if (todayTotalCount > 0) (todayCompletedCount * 100) / todayTotalCount else 0

        // 2. Global Streaks
        var currentStreak = 0
        var checkDate = today
        val todayHasAny = logsByDate.containsKey(todayStr)
        if (!todayHasAny) {
            checkDate = today.minusDays(1)
        }
        while (logsByDate.containsKey(checkDate.format(dateFormatter))) {
            currentStreak++
            checkDate = checkDate.minusDays(1)
        }

        // Longest streak across all recorded days
        val allDatesWithCompletions = logsByDate.keys.mapNotNull {
            try { LocalDate.parse(it, dateFormatter) } catch (e: Exception) { null }
        }.sorted()

        var maxStreak = 0
        var tempStreak = 0
        var prevDate: LocalDate? = null
        for (d in allDatesWithCompletions) {
            if (prevDate == null || ChronoUnit.DAYS.between(prevDate, d) == 1L) {
                tempStreak++
            } else if (ChronoUnit.DAYS.between(prevDate, d) > 1L) {
                tempStreak = 1
            }
            if (tempStreak > maxStreak) {
                maxStreak = tempStreak
            }
            prevDate = d
        }
        val bestOverallStreak = maxOf(maxStreak, currentStreak)

        // 3. Daily trends for the chosen period
        val periodDays = period.days.toInt()
        val dailyTrends = mutableListOf<DayTrend>()
        val startDate = today.minusDays((periodDays - 1).toLong())

        for (i in 0 until periodDays) {
            val d = startDate.plusDays(i.toLong())
            val dStr = d.format(dateFormatter)
            val dLogs = logsByDate[dStr] ?: emptyList()
            val completedCount = dLogs.size
            val total = maxOf(activeHabits.size, 1)
            val ratio = (completedCount.toFloat() / total).coerceIn(0f, 1f)
            dailyTrends.add(
                DayTrend(
                    date = d,
                    dayLabel = d.dayOfWeek.name.take(3),
                    completedCount = completedCount,
                    totalCount = total,
                    ratio = ratio
                )
            )
        }

        // 4. Heatmap Cells (Last 35 days for a clean 7x5 or 5-week grid)
        val heatmapDays = 35
        val heatmapStart = today.minusDays((heatmapDays - 1).toLong())
        val heatmapCells = mutableListOf<HeatmapCell>()
        for (i in 0 until heatmapDays) {
            val d = heatmapStart.plusDays(i.toLong())
            val dStr = d.format(dateFormatter)
            val count = logsByDate[dStr]?.size ?: 0
            val intensity = when {
                count == 0 -> 0
                count == 1 -> 1
                count <= 2 -> 2
                count <= 4 -> 3
                else -> 4
            }
            heatmapCells.add(HeatmapCell(date = d, dateString = dStr, completedCount = count, intensity = intensity))
        }

        // 5. Habit-by-habit performance
        val habitPerformances = activeHabits.map { habit ->
            val hLogs = logsByHabit[habit.id] ?: emptyList()
            val habitDates = hLogs.mapNotNull {
                try { LocalDate.parse(it.date, dateFormatter) } catch (e: Exception) { null }
            }.toSet()

            var hStreak = 0
            var cDate = if (habitDates.contains(today)) today else today.minusDays(1)
            while (habitDates.contains(cDate)) {
                hStreak++
                cDate = cDate.minusDays(1)
            }

            val thirtyDaysAgo = today.minusDays(30)
            val recentLogsCount = habitDates.count { !it.isBefore(thirtyDaysAgo) }
            val adherence = (recentLogsCount * 100 / 30).coerceAtMost(100)

            HabitPerformance(
                habit = habit,
                currentStreak = hStreak,
                bestStreak = maxOf(hStreak, recentLogsCount),
                totalCompletions = hLogs.size,
                adherencePercent = adherence
            )
        }.sortedByDescending { it.adherencePercent }

        // 6. Category breakdown
        val categoryShares = mutableMapOf<String, Int>()
        for (log in logs.filter { it.completed }) {
            val h = activeHabits.find { it.id == log.habitId }
            val cat = h?.category ?: "routine"
            categoryShares[cat] = (categoryShares[cat] ?: 0) + 1
        }
        val topCategory = categoryShares.maxByOrNull { it.value }?.key ?: "Mindfulness"

        // 7. Best Day of Week
        val dayCounts = mutableMapOf<DayOfWeek, Int>()
        for (log in logs.filter { it.completed }) {
            try {
                val d = LocalDate.parse(log.date, dateFormatter)
                dayCounts[d.dayOfWeek] = (dayCounts[d.dayOfWeek] ?: 0) + 1
            } catch (_: Exception) {}
        }
        val bestDay = dayCounts.maxByOrNull { it.value }?.key?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Today"

        val overallAdherence = if (activeHabits.isNotEmpty()) {
            (dailyTrends.map { it.ratio }.average() * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }

        return AnalyticsSummary(
            overallAdherence = overallAdherence,
            currentActiveStreak = currentStreak,
            bestOverallStreak = bestOverallStreak,
            totalCompletedSessions = logs.count { it.completed },
            todayCompletedCount = todayCompletedCount,
            todayTotalCount = todayTotalCount,
            todayAdherence = todayAdherence,
            bestDayOfWeek = bestDay,
            topCategory = topCategory.replaceFirstChar { it.uppercase() },
            dailyTrends = dailyTrends,
            heatmapCells = heatmapCells,
            habitPerformances = habitPerformances,
            categoryShares = categoryShares
        )
    }
}
