package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Habit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createHabit_authenticatedUser_success() = runBlocking {
        val uid = signInTestUser("alice_rule_test@example.com")
        val repository = HabitRepository(firestore)

        val habit = Habit(
            userId = uid,
            title = "Morning Run",
            description = "5km run",
            category = "fitness",
            iconName = "fitness",
            colorHex = "#81C784",
            frequency = "daily",
            targetDaysPerWeek = 5,
            targetValue = 1,
            unit = "times"
        )

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createHabit(habit)
        }
        assertTrue(result.isSuccess)
        val habitId = result.getOrThrow()
        assertTrue(habitId.isNotBlank())

        val habits = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.observeHabits().first()
        }
        val myHabit = habits.find { it.id == habitId }
        assertNotNull(myHabit)
        assertEquals("Morning Run", myHabit!!.title)
    }

    @Test
    fun logHabitProgress_authenticatedUser_success() = runBlocking {
        val uid = signInTestUser("bob_rule_test@example.com")
        val repository = HabitRepository(firestore)

        val habit = Habit(
            userId = uid,
            title = "Drink Water",
            category = "health",
            iconName = "water",
            colorHex = "#64B5F6",
            frequency = "daily",
            targetDaysPerWeek = 7,
            targetValue = 2000,
            unit = "ml"
        )

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createHabit(habit)
        }
        assertTrue(createResult.isSuccess)
        val habitId = createResult.getOrThrow()

        val logResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.logHabitProgress(habitId, "2026-10-27", 2000L, true, "Hydrated!")
        }
        assertTrue(logResult.isSuccess)

        val logs = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.observeLogsForDate("2026-10-27").first()
        }
        val myLog = logs.find { it.habitId == habitId }
        assertNotNull(myLog)
        assertEquals(2000L, myLog!!.value)
        assertTrue(myLog.completed)
    }

    @Test
    fun unauthenticatedUser_fails() = runBlocking {
        // Not signed in
        auth.signOut()
        val repository = HabitRepository(firestore)

        var threwException = false
        try {
            repository.createHabit(
                Habit(
                    title = "Secret Habit",
                    category = "general"
                )
            )
        } catch (e: IllegalStateException) {
            threwException = true
        }
        assertTrue("Expected IllegalStateException for unauthenticated user", threwException)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 10000L
    }
}
