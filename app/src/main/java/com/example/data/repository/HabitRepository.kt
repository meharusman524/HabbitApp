package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.HabitDao
import com.example.data.local.LocalHabit
import com.example.data.local.LocalHabitLog
import com.example.data.model.Habit
import com.example.data.model.HabitLog
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

class HabitRepository(
    private val db: FirebaseFirestore?,
    private val localDao: HabitDao? = null
) {

    constructor(context: Context) : this(
        db = try {
            FirebaseFirestore.getInstance(
                context.applicationContext.getString(R.string.firestore_database_id)
            )
        } catch (_: Exception) {
            null
        },
        localDao = AppDatabase.getInstance(context).habitDao()
    )

    // Secondary constructor for rule tests with direct Firestore instance
    constructor(firestore: FirebaseFirestore) : this(
        db = firestore,
        localDao = null
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
    }

    val isCloudMode: Boolean
        get() = auth.currentUser != null && db != null

    /**
     * Observe all active habits (from Firestore if signed in, or from local Room if guest).
     */
    fun observeHabits(): Flow<List<Habit>> = flow {
        val uid = auth.currentUser?.uid
        if (uid != null && db != null) {
            val path = "users/$uid/habits"
            val habitsRef = db.collection("users").document(uid).collection("habits")

            habitsRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Habit::class.java).filter { !it.isArchived }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
                .collect { emit(it) }
        } else if (localDao != null) {
            localDao.getActiveHabits()
                .map { list -> list.map { it.toHabit() } }
                .collect { emit(it) }
        } else {
            // Unauthenticated in test environment without localDao
            throw IllegalStateException("User must be signed in before accessing Firestore.")
        }
    }

    /**
     * Observe all logs (from Firestore if signed in, or from local Room if guest).
     */
    fun observeAllLogs(): Flow<List<HabitLog>> = flow {
        val uid = auth.currentUser?.uid
        if (uid != null && db != null) {
            val path = "users/$uid/logs"
            val logsRef = db.collection("users").document(uid).collection("logs")

            logsRef.snapshots()
                .map { snapshot -> snapshot.toObjects(HabitLog::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
                .collect { emit(it) }
        } else if (localDao != null) {
            localDao.getAllLogs()
                .map { list -> list.map { it.toHabitLog() } }
                .collect { emit(it) }
        } else {
            throw IllegalStateException("User must be signed in before accessing Firestore.")
        }
    }

    /**
     * Observe logs for a specific date.
     */
    fun observeLogsForDate(date: String): Flow<List<HabitLog>> = flow {
        val uid = auth.currentUser?.uid
        if (uid != null && db != null) {
            val path = "users/$uid/logs"
            val logsRef = db.collection("users").document(uid).collection("logs")

            logsRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(HabitLog::class.java).filter { it.date == date }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
                .collect { emit(it) }
        } else if (localDao != null) {
            localDao.getLogsForDate(date)
                .map { list -> list.map { it.toHabitLog() } }
                .collect { emit(it) }
        } else {
            throw IllegalStateException("User must be signed in before accessing Firestore.")
        }
    }

    /**
     * Create a new habit.
     */
    suspend fun createHabit(habit: Habit): Result<String> {
        val uid = auth.currentUser?.uid
        val docId = if (habit.id.isNotBlank()) habit.id else UUID.randomUUID().toString()

        if (uid != null && db != null) {
            val habitsRef = db.collection("users").document(uid).collection("habits")
            val docRef = habitsRef.document(docId)

            val payload = habit.toMap().toMutableMap().apply {
                put("userId", uid)
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }

            return try {
                docRef.set(payload).await()
                Result.success(docId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, docRef.path)
                Result.failure(e)
            }
        } else if (localDao != null) {
            val local = LocalHabit(
                id = docId,
                title = habit.title,
                description = habit.description,
                category = habit.category,
                iconName = habit.iconName,
                colorHex = habit.colorHex,
                frequency = habit.frequency,
                targetDaysPerWeek = habit.targetDaysPerWeek,
                targetValue = habit.targetValue,
                unit = habit.unit,
                isArchived = habit.isArchived
            )
            localDao.insertHabit(local)
            return Result.success(docId)
        } else {
            throw IllegalStateException("User must be signed in before accessing Firestore.")
        }
    }

    /**
     * Update an existing habit.
     */
    suspend fun updateHabit(habit: Habit): Result<Unit> {
        val uid = auth.currentUser?.uid
        require(habit.id.isNotBlank()) { "Habit ID must not be blank for update" }

        if (uid != null && db != null) {
            val docRef = db.collection("users").document(uid).collection("habits").document(habit.id)
            val updates = mutableMapOf<String, Any?>(
                "title" to habit.title,
                "description" to habit.description,
                "category" to habit.category,
                "iconName" to habit.iconName,
                "colorHex" to habit.colorHex,
                "frequency" to habit.frequency,
                "targetDaysPerWeek" to habit.targetDaysPerWeek,
                "targetValue" to habit.targetValue,
                "unit" to habit.unit,
                "isArchived" to habit.isArchived,
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            return try {
                docRef.update(updates).await()
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, docRef.path)
                Result.failure(e)
            }
        } else if (localDao != null) {
            val local = LocalHabit(
                id = habit.id,
                title = habit.title,
                description = habit.description,
                category = habit.category,
                iconName = habit.iconName,
                colorHex = habit.colorHex,
                frequency = habit.frequency,
                targetDaysPerWeek = habit.targetDaysPerWeek,
                targetValue = habit.targetValue,
                unit = habit.unit,
                isArchived = habit.isArchived
            )
            localDao.insertHabit(local)
            return Result.success(Unit)
        } else {
            throw IllegalStateException("User must be signed in before accessing Firestore.")
        }
    }

    /**
     * Delete a habit.
     */
    suspend fun deleteHabit(habitId: String): Result<Unit> {
        val uid = auth.currentUser?.uid
        require(habitId.isNotBlank()) { "Habit ID must not be blank" }

        if (uid != null && db != null) {
            val docRef = db.collection("users").document(uid).collection("habits").document(habitId)
            return try {
                docRef.delete().await()
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.DELETE, docRef.path)
                Result.failure(e)
            }
        } else if (localDao != null) {
            localDao.deleteHabit(habitId)
            localDao.deleteLogsForHabit(habitId)
            return Result.success(Unit)
        } else {
            throw IllegalStateException("User must be signed in before accessing Firestore.")
        }
    }

    /**
     * Toggle completion or log progress for a habit on a specific date.
     */
    suspend fun logHabitProgress(
        habitId: String,
        date: String,
        value: Long,
        completed: Boolean,
        notes: String? = null
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
        require(habitId.isNotBlank()) { "Habit ID must not be blank" }
        require(date.isNotBlank()) { "Date must not be blank" }

        val logDocId = "${habitId}_${date.replace("-", "_")}"

        if (uid != null && db != null) {
            val docRef = db.collection("users").document(uid).collection("logs").document(logDocId)
            return try {
                val snapshot = docRef.get().await()
                if (snapshot.exists()) {
                    val updates = mutableMapOf<String, Any?>(
                        "value" to value,
                        "completed" to completed,
                        "notes" to notes,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ).filterValues { it != null }
                    docRef.update(updates).await()
                } else {
                    val newLog = mutableMapOf<String, Any?>(
                        "userId" to uid,
                        "habitId" to habitId,
                        "date" to date,
                        "value" to value,
                        "completed" to completed,
                        "notes" to notes,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ).filterValues { it != null }
                    docRef.set(newLog).await()
                }
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, docRef.path)
                Result.failure(e)
            }
        } else if (localDao != null) {
            val localLog = LocalHabitLog(
                id = logDocId,
                habitId = habitId,
                date = date,
                value = value,
                completed = completed,
                notes = notes
            )
            localDao.insertLog(localLog)
            return Result.success(Unit)
        } else {
            throw IllegalStateException("User must be signed in before accessing Firestore.")
        }
    }

    /**
     * Synchronize local guest habits and logs into Firestore upon Google sign in.
     */
    suspend fun syncLocalHabitsToCloud() {
        val uid = auth.currentUser?.uid ?: return
        val firestoreDb = db ?: return
        val habits = localDao?.getActiveHabitsList() ?: emptyList()
        val logs = localDao?.getAllLogsList() ?: emptyList()

        for (h in habits) {
            val habit = h.toHabit()
            val docRef = firestoreDb.collection("users").document(uid).collection("habits").document(habit.id)
            val payload = habit.toMap().toMutableMap().apply {
                put("userId", uid)
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            try { docRef.set(payload).await() } catch (_: Exception) {}
        }

        for (l in logs) {
            val log = l.toHabitLog()
            val logDocId = "${log.habitId}_${log.date.replace("-", "_")}"
            val docRef = firestoreDb.collection("users").document(uid).collection("logs").document(logDocId)
            val payload = log.toMap().toMutableMap().apply {
                put("userId", uid)
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            try { docRef.set(payload).await() } catch (_: Exception) {}
        }
    }
}
