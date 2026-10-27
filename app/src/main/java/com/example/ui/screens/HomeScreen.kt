package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Habit
import com.example.data.model.HabitLog
import com.example.ui.components.HabitIconHelper
import com.example.ui.components.WeekCalendarStrip
import com.example.ui.viewmodel.HabitUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    uiState: HabitUiState,
    onSelectDate: (LocalDate) -> Unit,
    onSelectCategory: (String) -> Unit,
    onToggleHabit: (Habit) -> Unit,
    onUpdateProgress: (Habit, Long) -> Unit,
    onAddHabitClick: () -> Unit,
    onEditHabitClick: (Habit) -> Unit,
    onHabitNotesClick: (Habit) -> Unit,
    onProfileClick: () -> Unit,
    onSeedStarterHabits: () -> Unit,
    onDismissMessage: () -> Unit,
    onQuickAddHabit: (Habit) -> Unit = {},
    isGuestMode: Boolean = false,
    onSignInClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }
    val displayDateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, MMM d") }
    val selectedDateStr = uiState.selectedDate.format(dateFormatter)

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            onDismissMessage()
        }
    }

    val filteredHabits = remember(uiState.habits, uiState.selectedCategory) {
        if (uiState.selectedCategory == "All") {
            uiState.habits
        } else {
            uiState.habits.filter { it.category.equals(uiState.selectedCategory, ignoreCase = true) }
        }
    }

    val logsMap = remember(uiState.logs, selectedDateStr) {
        uiState.logs.filter { it.date == selectedDateStr }.associateBy { it.habitId }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddHabitClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .testTag("add_habit_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add New Habit",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Header
            item {
                HomeHeader(
                    selectedDate = uiState.selectedDate,
                    displayDateFormatter = displayDateFormatter,
                    onProfileClick = onProfileClick
                )
            }

            // Guest Mode Banner
            if (isGuestMode) {
                item {
                    GuestModeBanner(
                        onSignInClick = onSignInClick,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }
            }

            // Week Calendar Strip
            item {
                WeekCalendarStrip(
                    selectedDate = uiState.selectedDate,
                    logs = uiState.logs,
                    onDateSelected = onSelectDate,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }

            // Daily Progress Overview Card
            item {
                val summary = uiState.analyticsSummary
                val todayCompleted = summary?.todayCompletedCount ?: 0
                val todayTotal = summary?.todayTotalCount ?: 0
                val progressRatio = if (todayTotal > 0) todayCompleted.toFloat() / todayTotal else 0f
                val streak = summary?.currentActiveStreak ?: 0

                DailyProgressCard(
                    completedCount = todayCompleted,
                    totalCount = todayTotal,
                    ratio = progressRatio,
                    streak = streak,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }

            // Popular Habit Templates Quick-Add Row
            item {
                val existingTitles = remember(uiState.habits) {
                    uiState.habits.map { it.title.lowercase() }.toSet()
                }
                QuickTemplatesSection(
                    onQuickAdd = onQuickAddHabit,
                    existingHabitTitles = existingTitles,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Category Filter Row
            item {
                CategoryFilterRow(
                    selectedCategory = uiState.selectedCategory,
                    onSelectCategory = onSelectCategory,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Habits Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (uiState.selectedCategory == "All") "Today's Habits" else "${uiState.selectedCategory.replaceFirstChar { it.uppercase() }} Habits",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${filteredHabits.count { logsMap[it.id]?.completed == true }} / ${filteredHabits.size} done",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Empty State
            if (filteredHabits.isEmpty()) {
                item {
                    EmptyHabitsState(
                        onAddHabit = onAddHabitClick,
                        onSeedStarters = onSeedStarterHabits,
                        isFiltered = uiState.selectedCategory != "All"
                    )
                }
            } else {
                items(filteredHabits, key = { it.id }) { habit ->
                    val log = logsMap[habit.id]
                    HabitCard(
                        habit = habit,
                        log = log,
                        onToggle = { onToggleHabit(habit) },
                        onUpdateProgress = { delta -> onUpdateProgress(habit, delta) },
                        onEdit = { onEditHabitClick(habit) },
                        onNotes = { onHabitNotesClick(habit) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    selectedDate: LocalDate,
    displayDateFormatter: DateTimeFormatter,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = "HabitPulse",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "HABITPULSE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.4.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = selectedDate.format(displayDateFormatter),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape)
                .clickable { onProfileClick() }
                .testTag("profile_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Profile",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun DailyProgressCard(
    completedCount: Int,
    totalCount: Int,
    ratio: Float,
    streak: Int,
    modifier: Modifier = Modifier
) {
    val animatedRatio by animateFloatAsState(
        targetValue = ratio,
        animationSpec = spring(),
        label = "progress"
    )
    val percentage = (ratio * 100).toInt()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Daily Rhythm",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (streak > 0) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = "Streak",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "$streak d streak",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = when {
                            totalCount == 0 -> "Tap any popular template below to start!"
                            percentage == 100 -> "All habits completed! Perfect streak day!"
                            percentage >= 50 -> "Great momentum! $completedCount of $totalCount completed."
                            completedCount > 0 -> "$completedCount of $totalCount done · Keep it up!"
                            else -> "Start today by checking off your first habit!"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Circular Progress Indicator badge with percentage inside
                Box(
                    modifier = Modifier.size(54.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        strokeWidth = 6.dp,
                        strokeCap = StrokeCap.Round
                    )
                    CircularProgressIndicator(
                        progress = { animatedRatio },
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 6.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        text = "$percentage%",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { animatedRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun QuickTemplatesSection(
    onQuickAdd: (Habit) -> Unit,
    existingHabitTitles: Set<String>,
    modifier: Modifier = Modifier
) {
    val templates = remember {
        listOf(
            Habit(title = "Drink 2.5L Water", description = "Track hydration with 250ml glasses", category = "health", iconName = "water", colorHex = "#3B82F6", targetValue = 2500, unit = "ml"),
            Habit(title = "Morning Meditation", description = "10 mins calm mindful breathing", category = "mindfulness", iconName = "meditation", colorHex = "#8B5CF6", targetValue = 1, unit = "times"),
            Habit(title = "30-Min Workout", description = "Cardio, weights or stretching", category = "fitness", iconName = "fitness", colorHex = "#F59E0B", targetValue = 1, unit = "times"),
            Habit(title = "Read 20 Pages", description = "Expand mind with books", category = "focus", iconName = "book", colorHex = "#10B981", targetValue = 20, unit = "pages"),
            Habit(title = "8,000 Steps", description = "Daily walking & active movement", category = "fitness", iconName = "walk", colorHex = "#06B6D4", targetValue = 8000, unit = "steps"),
            Habit(title = "Morning Sunlight", description = "15 min natural light", category = "health", iconName = "sun", colorHex = "#F97316", targetValue = 1, unit = "times"),
            Habit(title = "Daily Reflection", description = "Write 3 wins or gratitude notes", category = "mindfulness", iconName = "heart", colorHex = "#F43F5E", targetValue = 1, unit = "times"),
            Habit(title = "Deep Work Session", description = "Undistracted focus block", category = "focus", iconName = "timer", colorHex = "#3B82F6", targetValue = 60, unit = "mins"),
            Habit(title = "Sleep 8 Hours", description = "Restorative nighttime rest", category = "routine", iconName = "sleep", colorHex = "#8B5CF6", targetValue = 8, unit = "hours"),
            Habit(title = "Digital Detox", description = "No screens 45 min before sleep", category = "routine", iconName = "sleep", colorHex = "#6366F1", targetValue = 1, unit = "times")
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Popular Habit Templates",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "1-Tap Add",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(templates) { template ->
                val isAlreadyAdded = existingHabitTitles.contains(template.title.lowercase())
                val templateColor = HabitIconHelper.parseColor(template.colorHex)

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (isAlreadyAdded) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                        else templateColor.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .clickable(enabled = !isAlreadyAdded) {
                            onQuickAdd(template)
                        }
                        .testTag("quick_add_${template.title.replace(" ", "_")}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(templateColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = HabitIconHelper.getIcon(template.iconName),
                                contentDescription = null,
                                tint = templateColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = template.title,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isAlreadyAdded) "✓ Added" else "+ Add to routine",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isAlreadyAdded) MaterialTheme.colorScheme.onSurfaceVariant else templateColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterRow(
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = remember {
        listOf("All" to "All") + HabitIconHelper.availableCategories
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { (key, label) ->
            val isSelected = selectedCategory.equals(key, ignoreCase = true)
            FilterChip(
                selected = isSelected,
                onClick = { onSelectCategory(key) },
                label = { Text(label) },
                shape = RoundedCornerShape(14.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    selectedBorderColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
private fun HabitCard(
    habit: Habit,
    log: HabitLog?,
    onToggle: () -> Unit,
    onUpdateProgress: (Long) -> Unit,
    onEdit: () -> Unit,
    onNotes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompleted = log?.completed == true
    val currentVal = log?.value ?: 0L
    val targetVal = habit.targetValue
    val isNumeric = targetVal > 1L
    val habitColor = HabitIconHelper.parseColor(habit.colorHex)
    var menuExpanded by remember { mutableStateOf(false) }

    val cardBg by animateColorAsState(
        targetValue = if (isCompleted) {
            habitColor.copy(alpha = 0.08f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "card_bg"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("habit_card_${habit.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isCompleted) habitColor.copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Colored Icon Badge
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(habitColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = HabitIconHelper.getIcon(habit.iconName),
                        contentDescription = habit.title,
                        tint = habitColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Title and description
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = if (isCompleted) TextDecoration.None else TextDecoration.None
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = habit.category.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            color = habitColor
                        )
                        if (isNumeric) {
                            Text(
                                text = " · $currentVal / $targetVal ${habit.unit}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isNumeric) {
                        Spacer(modifier = Modifier.height(6.dp))
                        val numRatio = if (targetVal > 0) (currentVal.toFloat() / targetVal).coerceIn(0f, 1f) else 0f
                        LinearProgressIndicator(
                            progress = { numRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = habitColor,
                            trackColor = habitColor.copy(alpha = 0.15f),
                            strokeCap = StrokeCap.Round
                        )
                    }
                }

                // Check Action / Stepper
                if (!isNumeric) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCompleted) habitColor
                                else habitColor.copy(alpha = 0.1f)
                            )
                            .border(
                                width = 1.5.dp,
                                color = habitColor,
                                shape = CircleShape
                            )
                            .clickable { onToggle() }
                            .testTag("toggle_habit_${habit.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onUpdateProgress(-1L) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Decrease",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isCompleted) habitColor
                                    else habitColor.copy(alpha = 0.12f)
                                )
                                .border(1.5.dp, habitColor, CircleShape)
                                .clickable { onToggle() }
                                .testTag("toggle_habit_${habit.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Completed",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Increase",
                                    tint = habitColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // More Menu
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Habit") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (log?.notes.isNullOrBlank()) "Add Daily Reflection" else "Edit Reflection") },
                            leadingIcon = { Icon(Icons.Default.NoteAlt, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onNotes()
                            }
                        )
                    }
                }
            }

            // Note Banner if present
            if (!log?.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable { onNotes() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.NoteAlt,
                        contentDescription = null,
                        tint = habitColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = log!!.notes!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyHabitsState(
    onAddHabit: () -> Unit,
    onSeedStarters: () -> Unit,
    isFiltered: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SelfImprovement,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isFiltered) "No habits in this category" else "No habits yet",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (isFiltered) "Switch categories or create a new habit" else "Start small. Daily tiny gains compound into greatness.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (!isFiltered) {
            Button(
                onClick = onSeedStarters,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("seed_habits_button")
            ) {
                Text("Add Recommended Starter Habits")
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = onAddHabit,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text("Create Custom Habit")
        }
    }
}

@Composable
private fun GuestModeBanner(
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSignInClick() }
            .testTag("guest_mode_banner"),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "Offline Storage Active",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Guest Mode (Offline)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tracking habits locally · Tap to sign in & backup",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "Sign in ›",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
