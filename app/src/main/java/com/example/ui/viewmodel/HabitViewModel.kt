package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Habit
import com.example.data.model.HabitLog
import com.example.data.repository.HabitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class ActiveSheet {
    NONE,
    ADD_HABIT,
    EDIT_HABIT,
    HABIT_NOTES,
    PROFILE
}

data class HabitUiState(
    val habits: List<Habit> = emptyList(),
    val logs: List<HabitLog> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedCategory: String = "All",
    val analyticsPeriod: AnalyticsPeriod = AnalyticsPeriod.WEEK,
    val activeSheet: ActiveSheet = ActiveSheet.NONE,
    val selectedHabitForAction: Habit? = null,
    val analyticsSummary: AnalyticsSummary? = null,
    val isLoading: Boolean = false,
    val userMessage: String? = null
)

class HabitViewModel(
    private val repository: HabitRepository
) : ViewModel() {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _selectedCategory = MutableStateFlow("All")
    private val _analyticsPeriod = MutableStateFlow(AnalyticsPeriod.WEEK)
    private val _activeSheet = MutableStateFlow(ActiveSheet.NONE)
    private val _selectedHabitForAction = MutableStateFlow<Habit?>(null)
    private val _userMessage = MutableStateFlow<String?>(null)
    private val _isActionLoading = MutableStateFlow(false)

    // Combine repository streams with UI state filters
    val uiState: StateFlow<HabitUiState> = combine(
        repository.observeHabits().catch { emit(emptyList()) },
        repository.observeAllLogs().catch { emit(emptyList()) },
        _selectedDate,
        _selectedCategory,
        _analyticsPeriod,
        _activeSheet,
        _selectedHabitForAction,
        _userMessage,
        _isActionLoading
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val habits = args[0] as List<Habit>
        @Suppress("UNCHECKED_CAST")
        val logs = args[1] as List<HabitLog>
        val selDate = args[2] as LocalDate
        val selCat = args[3] as String
        val period = args[4] as AnalyticsPeriod
        val sheet = args[5] as ActiveSheet
        val selHabit = args[6] as Habit?
        val msg = args[7] as String?
        val loading = args[8] as Boolean

        val summary = HabitAnalyticsCalculator.calculate(
            habits = habits,
            logs = logs,
            today = selDate,
            period = period
        )

        HabitUiState(
            habits = habits,
            logs = logs,
            selectedDate = selDate,
            selectedCategory = selCat,
            analyticsPeriod = period,
            activeSheet = sheet,
            selectedHabitForAction = selHabit,
            analyticsSummary = summary,
            isLoading = loading,
            userMessage = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = HabitUiState(isLoading = true)
    )

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setAnalyticsPeriod(period: AnalyticsPeriod) {
        _analyticsPeriod.value = period
    }

    fun showAddHabit() {
        _selectedHabitForAction.value = null
        _activeSheet.value = ActiveSheet.ADD_HABIT
    }

    fun showEditHabit(habit: Habit) {
        _selectedHabitForAction.value = habit
        _activeSheet.value = ActiveSheet.EDIT_HABIT
    }

    fun showHabitNotes(habit: Habit) {
        _selectedHabitForAction.value = habit
        _activeSheet.value = ActiveSheet.HABIT_NOTES
    }

    fun showProfile() {
        _activeSheet.value = ActiveSheet.PROFILE
    }

    fun dismissSheet() {
        _activeSheet.value = ActiveSheet.NONE
        _selectedHabitForAction.value = null
    }

    fun dismissMessage() {
        _userMessage.value = null
    }

    fun toggleHabitCompletion(habit: Habit) {
        val dateStr = _selectedDate.value.format(dateFormatter)
        val logs = uiState.value.logs
        val existingLog = logs.find { it.habitId == habit.id && it.date == dateStr }
        val isCurrentlyCompleted = existingLog?.completed == true
        val targetVal = habit.targetValue

        viewModelScope.launch {
            _isActionLoading.value = true
            val result = repository.logHabitProgress(
                habitId = habit.id,
                date = dateStr,
                value = if (!isCurrentlyCompleted) targetVal else 0L,
                completed = !isCurrentlyCompleted,
                notes = existingLog?.notes
            )
            _isActionLoading.value = false
            if (result.isFailure) {
                _userMessage.value = "Failed to update habit: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun updateProgressValue(habit: Habit, delta: Long) {
        val dateStr = _selectedDate.value.format(dateFormatter)
        val logs = uiState.value.logs
        val existingLog = logs.find { it.habitId == habit.id && it.date == dateStr }
        val currentVal = existingLog?.value ?: 0L
        val newVal = (currentVal + delta).coerceAtLeast(0L)
        val isComplete = newVal >= habit.targetValue

        viewModelScope.launch {
            _isActionLoading.value = true
            val result = repository.logHabitProgress(
                habitId = habit.id,
                date = dateStr,
                value = newVal,
                completed = isComplete,
                notes = existingLog?.notes
            )
            _isActionLoading.value = false
            if (result.isFailure) {
                _userMessage.value = "Failed to update progress: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun saveHabitNotes(habit: Habit, note: String) {
        val dateStr = _selectedDate.value.format(dateFormatter)
        val logs = uiState.value.logs
        val existingLog = logs.find { it.habitId == habit.id && it.date == dateStr }
        val currentVal = existingLog?.value ?: habit.targetValue
        val isCompleted = existingLog?.completed ?: true

        viewModelScope.launch {
            _isActionLoading.value = true
            val result = repository.logHabitProgress(
                habitId = habit.id,
                date = dateStr,
                value = currentVal,
                completed = isCompleted,
                notes = note
            )
            _isActionLoading.value = false
            dismissSheet()
            if (result.isFailure) {
                _userMessage.value = "Failed to save reflection note"
            }
        }
    }

    fun createHabit(
        title: String,
        description: String?,
        category: String,
        iconName: String,
        colorHex: String,
        frequency: String,
        targetDaysPerWeek: Long,
        targetValue: Long,
        unit: String
    ) {
        if (title.isBlank()) {
            _userMessage.value = "Please enter a habit title"
            return
        }

        viewModelScope.launch {
            _isActionLoading.value = true
            val habit = Habit(
                title = title.trim(),
                description = description?.trim(),
                category = category,
                iconName = iconName,
                colorHex = colorHex,
                frequency = frequency,
                targetDaysPerWeek = targetDaysPerWeek,
                targetValue = targetValue,
                unit = unit,
                isArchived = false
            )
            val result = repository.createHabit(habit)
            _isActionLoading.value = false
            if (result.isSuccess) {
                dismissSheet()
            } else {
                _userMessage.value = "Failed to create habit: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun updateHabit(
        id: String,
        title: String,
        description: String?,
        category: String,
        iconName: String,
        colorHex: String,
        frequency: String,
        targetDaysPerWeek: Long,
        targetValue: Long,
        unit: String
    ) {
        if (title.isBlank()) {
            _userMessage.value = "Please enter a habit title"
            return
        }

        viewModelScope.launch {
            _isActionLoading.value = true
            val habit = Habit(
                id = id,
                title = title.trim(),
                description = description?.trim(),
                category = category,
                iconName = iconName,
                colorHex = colorHex,
                frequency = frequency,
                targetDaysPerWeek = targetDaysPerWeek,
                targetValue = targetValue,
                unit = unit
            )
            val result = repository.updateHabit(habit)
            _isActionLoading.value = false
            if (result.isSuccess) {
                dismissSheet()
            } else {
                _userMessage.value = "Failed to update habit: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun deleteHabit(habitId: String) {
        viewModelScope.launch {
            _isActionLoading.value = true
            val result = repository.deleteHabit(habitId)
            _isActionLoading.value = false
            dismissSheet()
            if (result.isFailure) {
                _userMessage.value = "Failed to delete habit: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    private var hasCheckedAutoSeed = false

    init {
        viewModelScope.launch {
            repository.observeHabits().catch { emit(emptyList()) }.collect { list ->
                if (list.isEmpty() && !hasCheckedAutoSeed) {
                    hasCheckedAutoSeed = true
                    seedStarterHabits(silent = true)
                }
            }
        }
    }

    fun quickAddHabit(habit: Habit) {
        viewModelScope.launch {
            _isActionLoading.value = true
            val result = repository.createHabit(habit.copy(id = ""))
            _isActionLoading.value = false
            if (result.isSuccess) {
                _userMessage.value = "Added \"${habit.title}\" to your habits!"
            } else {
                _userMessage.value = "Failed to add habit"
            }
        }
    }

    fun seedStarterHabits(silent: Boolean = false) {
        val starters = listOf(
            Habit(
                title = "Drink 2.5L Water",
                description = "Maintain daily hydration with 250ml glasses",
                category = "health",
                iconName = "water",
                colorHex = "#3B82F6",
                frequency = "daily",
                targetDaysPerWeek = 7,
                targetValue = 2500,
                unit = "ml"
            ),
            Habit(
                title = "Morning Meditation",
                description = "10 minutes of calm mindful breathing",
                category = "mindfulness",
                iconName = "meditation",
                colorHex = "#8B5CF6",
                frequency = "daily",
                targetDaysPerWeek = 7,
                targetValue = 1,
                unit = "times"
            ),
            Habit(
                title = "30-Min Workout",
                description = "Strength training, cardio, or yoga",
                category = "fitness",
                iconName = "fitness",
                colorHex = "#F59E0B",
                frequency = "daily",
                targetDaysPerWeek = 5,
                targetValue = 1,
                unit = "times"
            ),
            Habit(
                title = "Read 20 Pages",
                description = "Continuous learning and mental growth",
                category = "focus",
                iconName = "book",
                colorHex = "#10B981",
                frequency = "daily",
                targetDaysPerWeek = 6,
                targetValue = 20,
                unit = "pages"
            ),
            Habit(
                title = "8,000 Daily Steps",
                description = "Consistent movement and active walking",
                category = "fitness",
                iconName = "walk",
                colorHex = "#06B6D4",
                frequency = "daily",
                targetDaysPerWeek = 7,
                targetValue = 8000,
                unit = "steps"
            ),
            Habit(
                title = "Morning Sunlight",
                description = "15 min natural light to set circadian rhythm",
                category = "health",
                iconName = "sun",
                colorHex = "#F97316",
                frequency = "daily",
                targetDaysPerWeek = 7,
                targetValue = 1,
                unit = "times"
            ),
            Habit(
                title = "Digital Detox Before Bed",
                description = "No screens 45 minutes before sleep",
                category = "routine",
                iconName = "sleep",
                colorHex = "#6366F1",
                frequency = "daily",
                targetDaysPerWeek = 7,
                targetValue = 1,
                unit = "times"
            ),
            Habit(
                title = "Daily Reflection Journal",
                description = "Write 3 wins or things you are grateful for",
                category = "mindfulness",
                iconName = "heart",
                colorHex = "#F43F5E",
                frequency = "daily",
                targetDaysPerWeek = 7,
                targetValue = 1,
                unit = "times"
            ),
            Habit(
                title = "Deep Work Session",
                description = "Undistracted focus sprint on top priority",
                category = "focus",
                iconName = "timer",
                colorHex = "#3B82F6",
                frequency = "daily",
                targetDaysPerWeek = 5,
                targetValue = 60,
                unit = "mins"
            ),
            Habit(
                title = "Sleep 8 Hours",
                description = "Rejuvenating, uninterrupted rest",
                category = "routine",
                iconName = "sleep",
                colorHex = "#8B5CF6",
                frequency = "daily",
                targetDaysPerWeek = 7,
                targetValue = 8,
                unit = "hours"
            )
        )

        viewModelScope.launch {
            _isActionLoading.value = true
            starters.forEach { habit ->
                repository.createHabit(habit)
            }
            _isActionLoading.value = false
            if (!silent) {
                _userMessage.value = "10 starter habits added to your board!"
            }
        }
    }
}

class HabitViewModelFactory(
    private val repository: HabitRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HabitViewModel::class.java)) {
            return HabitViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
