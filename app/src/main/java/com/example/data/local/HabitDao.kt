package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: LocalHabit)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: LocalHabitLog)

    @Query("SELECT * FROM local_habits WHERE isArchived = 0 ORDER BY createdAt ASC")
    fun getActiveHabits(): Flow<List<LocalHabit>>

    @Query("SELECT * FROM local_habits WHERE isArchived = 0 ORDER BY createdAt ASC")
    suspend fun getActiveHabitsList(): List<LocalHabit>

    @Query("SELECT * FROM local_habit_logs ORDER BY date DESC")
    fun getAllLogs(): Flow<List<LocalHabitLog>>

    @Query("SELECT * FROM local_habit_logs ORDER BY date DESC")
    suspend fun getAllLogsList(): List<LocalHabitLog>

    @Query("SELECT * FROM local_habit_logs WHERE date = :date")
    fun getLogsForDate(date: String): Flow<List<LocalHabitLog>>

    @Query("SELECT * FROM local_habit_logs WHERE id = :id LIMIT 1")
    suspend fun getLogById(id: String): LocalHabitLog?

    @Query("DELETE FROM local_habits WHERE id = :id")
    suspend fun deleteHabit(id: String)

    @Query("DELETE FROM local_habit_logs WHERE habitId = :habitId")
    suspend fun deleteLogsForHabit(habitId: String)
}
