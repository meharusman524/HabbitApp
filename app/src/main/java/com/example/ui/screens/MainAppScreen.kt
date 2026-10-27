package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.AddEditHabitDialog
import com.example.ui.components.HabitNotesDialog
import com.example.ui.components.ProfileSheet
import com.example.ui.viewmodel.ActiveSheet
import com.example.ui.viewmodel.HabitViewModel

@Composable
fun MainAppScreen(
    viewModel: HabitViewModel,
    onSignOut: () -> Unit,
    isGuestMode: Boolean = false,
    onSignInRequested: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Today, 1: Analytics

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                            contentDescription = "Today",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Today",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_tab_today")
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 1) Icons.Filled.AutoGraph else Icons.Outlined.AutoGraph,
                            contentDescription = "Analytics",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Analytics",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_tab_analytics")
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> {
                    HomeScreen(
                        uiState = uiState,
                        onSelectDate = { viewModel.selectDate(it) },
                        onSelectCategory = { viewModel.selectCategory(it) },
                        onToggleHabit = { viewModel.toggleHabitCompletion(it) },
                        onUpdateProgress = { habit, delta -> viewModel.updateProgressValue(habit, delta) },
                        onAddHabitClick = { viewModel.showAddHabit() },
                        onEditHabitClick = { viewModel.showEditHabit(it) },
                        onHabitNotesClick = { viewModel.showHabitNotes(it) },
                        onProfileClick = { viewModel.showProfile() },
                        onSeedStarterHabits = { viewModel.seedStarterHabits() },
                        onDismissMessage = { viewModel.dismissMessage() },
                        isGuestMode = isGuestMode,
                        onSignInClick = onSignInRequested
                    )
                }
                1 -> {
                    AnalyticsScreen(
                        summary = uiState.analyticsSummary,
                        selectedPeriod = uiState.analyticsPeriod,
                        onSelectPeriod = { viewModel.setAnalyticsPeriod(it) }
                    )
                }
            }
        }
    }

    // Modal Sheets
    when (uiState.activeSheet) {
        ActiveSheet.ADD_HABIT -> {
            AddEditHabitDialog(
                habitToEdit = null,
                onDismiss = { viewModel.dismissSheet() },
                onSave = { title, desc, cat, icon, color, freq, days, targetVal, unit ->
                    viewModel.createHabit(title, desc, cat, icon, color, freq, days, targetVal, unit)
                }
            )
        }
        ActiveSheet.EDIT_HABIT -> {
            AddEditHabitDialog(
                habitToEdit = uiState.selectedHabitForAction,
                onDismiss = { viewModel.dismissSheet() },
                onSave = { title, desc, cat, icon, color, freq, days, targetVal, unit ->
                    val habit = uiState.selectedHabitForAction
                    if (habit != null) {
                        viewModel.updateHabit(habit.id, title, desc, cat, icon, color, freq, days, targetVal, unit)
                    }
                },
                onDelete = { habitId ->
                    viewModel.deleteHabit(habitId)
                }
            )
        }
        ActiveSheet.HABIT_NOTES -> {
            val habit = uiState.selectedHabitForAction
            if (habit != null) {
                val currentLog = uiState.logs.find {
                    it.habitId == habit.id && it.date == uiState.selectedDate.toString()
                }
                HabitNotesDialog(
                    habit = habit,
                    currentLog = currentLog,
                    onDismiss = { viewModel.dismissSheet() },
                    onSaveNotes = { note ->
                        viewModel.saveHabitNotes(habit, note)
                    }
                )
            }
        }
        ActiveSheet.PROFILE -> {
            ProfileSheet(
                onDismiss = { viewModel.dismissSheet() },
                onSignOut = onSignOut,
                onSignIn = onSignInRequested
            )
        }
        ActiveSheet.NONE -> {}
    }
}
