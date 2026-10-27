package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class HabitLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val habitId: String = "",
    val date: String = "", // ISO Format: YYYY-MM-DD
    val value: Long = 1L,
    val completed: Boolean = true,
    val notes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>(
            "userId" to userId,
            "habitId" to habitId,
            "date" to date,
            "value" to value,
            "completed" to completed,
            "notes" to notes
        )
        return map.filterValues { it != null }
    }
}
