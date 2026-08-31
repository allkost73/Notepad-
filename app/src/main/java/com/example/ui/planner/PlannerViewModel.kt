package com.example.ui.planner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.DailyTask
import com.example.data.model.Note
import com.example.data.model.Priority
import com.example.data.model.TaskCategory
import com.example.data.repository.PlannerRepository
import com.example.util.DateUtils
import com.example.voice.ParsedVoiceResult
import com.example.voice.SmartVoiceParser
import com.example.voice.VoiceInputManager
import com.example.voice.VoiceState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MainTab(val titleRu: String) {
    PLANNER("Ежедневник"),
    NOTEBOOK("Записная книжка")
}

enum class TaskFilter(val titleRu: String) {
    ALL("Все"),
    ACTIVE("В работе"),
    COMPLETED("Выполненные")
}

data class PlannerUiState(
    val currentTab: MainTab = MainTab.PLANNER,
    val selectedEpochDay: Long = DateUtils.getTodayEpochDay(),
    val taskFilter: TaskFilter = TaskFilter.ALL,
    val notesSearchQuery: String = "",
    val selectedNotesCategory: String = "Все",
    val isVoiceSheetVisible: Boolean = false,
    val voiceParsedResult: ParsedVoiceResult? = null,
    val isTaskDialogVisible: Boolean = false,
    val editingTask: DailyTask? = null,
    val isNoteDialogVisible: Boolean = false,
    val editingNote: Note? = null,
    val snackbarMessage: String? = null
)

class PlannerViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = PlannerRepository(database.plannerDao())
    val voiceInputManager = VoiceInputManager(application)

    private val _uiState = MutableStateFlow(PlannerUiState())
    val uiState: StateFlow<PlannerUiState> = _uiState.asStateFlow()

    private val _selectedEpochDay = MutableStateFlow(DateUtils.getTodayEpochDay())

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentDayTasks: StateFlow<List<DailyTask>> = _selectedEpochDay
        .flatMapLatest { epochDay ->
            repository.getTasksForDate(epochDay)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val datesWithActiveTasks: StateFlow<List<Long>> = repository.getDatesWithActiveTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _notesSearchQuery = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val notesList: StateFlow<List<Note>> = _notesSearchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.getAllNotes()
            } else {
                repository.searchNotes(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Collect voice recognition results and auto-parse
        viewModelScope.launch {
            voiceInputManager.voiceState.collect { state ->
                if (state is VoiceState.Success) {
                    val parsed = SmartVoiceParser.parse(state.recognizedText)
                    _uiState.value = _uiState.value.copy(
                        voiceParsedResult = parsed
                    )
                }
            }
        }
    }

    fun setTab(tab: MainTab) {
        _uiState.value = _uiState.value.copy(currentTab = tab)
    }

    fun selectDate(epochDay: Long) {
        _selectedEpochDay.value = epochDay
        _uiState.value = _uiState.value.copy(selectedEpochDay = epochDay)
    }

    fun setTaskFilter(filter: TaskFilter) {
        _uiState.value = _uiState.value.copy(taskFilter = filter)
    }

    fun setNotesSearchQuery(query: String) {
        _notesSearchQuery.value = query
        _uiState.value = _uiState.value.copy(notesSearchQuery = query)
    }

    fun setNotesCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedNotesCategory = category)
    }

    // --- Task Actions ---
    fun toggleTask(task: DailyTask) {
        viewModelScope.launch {
            repository.setTaskCompleted(task.id, !task.isCompleted)
        }
    }

    fun saveTask(
        id: Long = 0,
        title: String,
        description: String,
        dateEpochDay: Long,
        timeString: String,
        priority: Priority,
        category: TaskCategory,
        isCompleted: Boolean = false
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val task = DailyTask(
                id = id,
                title = title.trim(),
                description = description.trim(),
                dateEpochDay = dateEpochDay,
                timeString = timeString.trim(),
                priority = priority,
                category = category,
                isCompleted = isCompleted
            )
            if (id == 0L) {
                repository.insertTask(task)
                showSnackbar("Задача успешно добавлена")
            } else {
                repository.updateTask(task)
                showSnackbar("Задача обновлена")
            }
            closeTaskDialog()
        }
    }

    fun deleteTask(task: DailyTask) {
        viewModelScope.launch {
            repository.deleteTask(task)
            showSnackbar("Задача удалена")
        }
    }

    // --- Note Actions ---
    fun saveNote(
        id: Long = 0,
        title: String,
        content: String,
        colorIndex: Int,
        category: String,
        isPinned: Boolean
    ) {
        if (title.isBlank() && content.isBlank()) return
        viewModelScope.launch {
            val note = Note(
                id = id,
                title = if (title.isBlank()) "Без названия" else title.trim(),
                content = content.trim(),
                colorIndex = colorIndex,
                category = if (category.isBlank()) "Общее" else category.trim(),
                isPinned = isPinned,
                updatedAt = System.currentTimeMillis()
            )
            if (id == 0L) {
                repository.insertNote(note)
                showSnackbar("Заметка сохранена")
            } else {
                repository.updateNote(note)
                showSnackbar("Заметка обновлена")
            }
            closeNoteDialog()
        }
    }

    fun toggleNotePin(note: Note) {
        viewModelScope.launch {
            repository.setNotePinned(note.id, !note.isPinned)
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
            showSnackbar("Заметка удалена")
        }
    }

    // --- Voice UI Actions ---
    fun openVoiceSheet() {
        voiceInputManager.reset()
        _uiState.value = _uiState.value.copy(
            isVoiceSheetVisible = true,
            voiceParsedResult = null
        )
        voiceInputManager.startListening()
    }

    fun closeVoiceSheet() {
        voiceInputManager.reset()
        _uiState.value = _uiState.value.copy(
            isVoiceSheetVisible = false,
            voiceParsedResult = null
        )
    }

    fun updateVoiceSheetText(text: String) {
        if (text.isBlank()) {
            _uiState.value = _uiState.value.copy(voiceParsedResult = null)
        } else {
            val parsed = SmartVoiceParser.parse(text)
            _uiState.value = _uiState.value.copy(voiceParsedResult = parsed)
        }
    }

    fun quickAddTaskFromText(text: String, fallbackEpochDay: Long = _selectedEpochDay.value) {
        if (text.isBlank()) return
        val parsed = SmartVoiceParser.parse(text)
        // If user didn't mention specific relative date (like 'завтра' / 'сегодня'), use currently selected date
        val finalEpochDay = if (text.contains("завтра", ignoreCase = true) ||
            text.contains("послезавтра", ignoreCase = true) ||
            text.contains("сегодня", ignoreCase = true) ||
            text.contains("понедельник", ignoreCase = true) ||
            text.contains("вторник", ignoreCase = true) ||
            text.contains("сред", ignoreCase = true) ||
            text.contains("четверг", ignoreCase = true) ||
            text.contains("пятниц", ignoreCase = true) ||
            text.contains("суббот", ignoreCase = true) ||
            text.contains("воскресень", ignoreCase = true)
        ) {
            parsed.suggestedEpochDay
        } else {
            fallbackEpochDay
        }

        saveTask(
            title = parsed.cleanedTitle.ifBlank { text.trim() },
            description = parsed.description,
            dateEpochDay = finalEpochDay,
            timeString = parsed.suggestedTimeString,
            priority = parsed.suggestedPriority,
            category = parsed.suggestedCategory
        )
    }

    fun quickAddNoteFromText(text: String) {
        if (text.isBlank()) return
        val lines = text.trim().lines()
        val title = lines.firstOrNull() ?: text.trim()
        val content = if (lines.size > 1) lines.drop(1).joinToString("\n") else text.trim()

        saveNote(
            title = if (title.length > 40) title.take(40) + "..." else title,
            content = content,
            colorIndex = 0,
            category = "Заметки",
            isPinned = false
        )
    }

    fun applyVoiceAsTask(parsed: ParsedVoiceResult) {
        closeVoiceSheet()
        saveTask(
            title = parsed.cleanedTitle,
            description = parsed.description,
            dateEpochDay = parsed.suggestedEpochDay,
            timeString = parsed.suggestedTimeString,
            priority = parsed.suggestedPriority,
            category = parsed.suggestedCategory
        )
        selectDate(parsed.suggestedEpochDay)
        setTab(MainTab.PLANNER)
    }

    fun applyVoiceAsNote(parsed: ParsedVoiceResult) {
        closeVoiceSheet()
        saveNote(
            title = parsed.cleanedTitle,
            content = parsed.rawText,
            colorIndex = 1,
            category = "Голосовая",
            isPinned = false
        )
        setTab(MainTab.NOTEBOOK)
    }

    // --- Dialogs ---
    fun openAddTaskDialog(dateEpochDay: Long = _selectedEpochDay.value) {
        _uiState.value = _uiState.value.copy(
            isTaskDialogVisible = true,
            editingTask = null,
            selectedEpochDay = dateEpochDay
        )
    }

    fun openEditTaskDialog(task: DailyTask) {
        _uiState.value = _uiState.value.copy(
            isTaskDialogVisible = true,
            editingTask = task
        )
    }

    fun closeTaskDialog() {
        _uiState.value = _uiState.value.copy(
            isTaskDialogVisible = false,
            editingTask = null
        )
    }

    fun openAddNoteDialog() {
        _uiState.value = _uiState.value.copy(
            isNoteDialogVisible = true,
            editingNote = null
        )
    }

    fun openEditNoteDialog(note: Note) {
        _uiState.value = _uiState.value.copy(
            isNoteDialogVisible = true,
            editingNote = note
        )
    }

    fun closeNoteDialog() {
        _uiState.value = _uiState.value.copy(
            isNoteDialogVisible = false,
            editingNote = null
        )
    }

    fun showSnackbar(message: String) {
        _uiState.value = _uiState.value.copy(snackbarMessage = message)
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}
