package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Habit
import com.example.data.model.HabitLog

@Entity(tableName = "local_habits")
data class LocalHabit(
    @PrimaryKey val id: String,
    val title: String,
    val description: String? = null,
    val category: String = "routine",
    val iconName: String = "check_circle",
    val colorHex: String = "#6366F1",
    val frequency: String = "daily",
    val targetDaysPerWeek: Long = 7L,
    val targetValue: Long = 1L,
    val unit: String = "times",
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toHabit(): Habit = Habit(
        id = id,
        userId = "local_guest",
        title = title,
        description = description,
        category = category,
        iconName = iconName,
        colorHex = colorHex,
        frequency = frequency,
        targetDaysPerWeek = targetDaysPerWeek,
        targetValue = targetValue,
        unit = unit,
        isArchived = isArchived
    )
}

@Entity(tableName = "local_habit_logs")
data class LocalHabitLog(
    @PrimaryKey val id: String,
    val habitId: String,
    val date: String,
    val value: Long = 1L,
    val completed: Boolean = true,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toHabitLog(): HabitLog = HabitLog(
        id = id,
        userId = "local_guest",
        habitId = habitId,
        date = date,
        value = value,
        completed = completed,
        notes = notes
    )
}
