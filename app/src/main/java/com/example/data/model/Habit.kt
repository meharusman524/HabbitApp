package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Habit(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String? = null,
    val category: String = "routine",
    val iconName: String = "check_circle",
    val colorHex: String = "#64B5F6",
    val frequency: String = "daily",
    val targetDaysPerWeek: Long = 7L,
    val targetValue: Long = 1L,
    val unit: String = "times",
    val isArchived: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>(
            "userId" to userId,
            "title" to title,
            "description" to description,
            "category" to category,
            "iconName" to iconName,
            "colorHex" to colorHex,
            "frequency" to frequency,
            "targetDaysPerWeek" to targetDaysPerWeek,
            "targetValue" to targetValue,
            "unit" to unit,
            "isArchived" to isArchived
        )
        return map.filterValues { it != null }
    }
}
