package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.HabitAmber
import com.example.ui.theme.HabitBlue
import com.example.ui.theme.HabitCyan
import com.example.ui.theme.HabitEmerald
import com.example.ui.theme.HabitIndigo
import com.example.ui.theme.HabitOrange
import com.example.ui.theme.HabitPurple
import com.example.ui.theme.HabitRose

object HabitIconHelper {

    val availableIcons = listOf(
        "meditation" to Icons.Default.SelfImprovement,
        "water" to Icons.Default.LocalDrink,
        "book" to Icons.Default.Book,
        "fitness" to Icons.Default.FitnessCenter,
        "run" to Icons.Default.DirectionsRun,
        "walk" to Icons.Default.DirectionsWalk,
        "heart" to Icons.Default.Favorite,
        "timer" to Icons.Default.Timer,
        "sleep" to Icons.Default.NightsStay,
        "sun" to Icons.Default.WbSunny,
        "code" to Icons.Default.Code,
        "music" to Icons.Default.Headphones
    )

    val availableColors = listOf(
        "#10B981" to HabitEmerald,
        "#3B82F6" to HabitBlue,
        "#8B5CF6" to HabitPurple,
        "#F59E0B" to HabitAmber,
        "#F43F5E" to HabitRose,
        "#06B6D4" to HabitCyan,
        "#6366F1" to HabitIndigo,
        "#F97316" to HabitOrange
    )

    val availableCategories = listOf(
        "routine" to "Routine",
        "health" to "Health",
        "fitness" to "Fitness",
        "mindfulness" to "Mindfulness",
        "focus" to "Focus",
        "learning" to "Learning"
    )

    fun getIcon(name: String): ImageVector {
        return availableIcons.find { it.first == name.lowercase() }?.second ?: Icons.Default.CheckCircle
    }

    fun parseColor(hex: String, fallback: Color = HabitEmerald): Color {
        return try {
            val clean = hex.removePrefix("#")
            val colorInt = when (clean.length) {
                6 -> ("FF$clean").toLong(16).toInt()
                8 -> clean.toLong(16).toInt()
                else -> return fallback
            }
            Color(colorInt)
        } catch (_: Exception) {
            fallback
        }
    }
}
