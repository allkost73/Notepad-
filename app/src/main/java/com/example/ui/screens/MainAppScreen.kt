package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.NoteEditDialog
import com.example.ui.components.TaskEditDialog
import com.example.ui.components.VoiceHelpDialog
import com.example.ui.components.VoiceRecognitionSheet
import com.example.ui.planner.MainTab
import com.example.ui.planner.PlannerViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: PlannerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tasks by viewModel.currentDayTasks.collectAsStateWithLifecycle()
    val datesWithTasks by viewModel.datesWithActiveTasks.collectAsStateWithLifecycle()
    val notes by viewModel.notesList.collectAsStateWithLifecycle()

    val voiceState by viewModel.voiceInputManager.voiceState.collectAsStateWithLifecycle()
    val liveRmsDb by viewModel.voiceInputManager.liveRmsDb.collectAsStateWithLifecycle()
    val partialText by viewModel.voiceInputManager.partialText.collectAsStateWithLifecycle()

    // Secondary inline voice callback handler for text fields inside dialogs
    var inlineVoiceCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }

    // Launcher for standard Google Voice Search system dialog (100% accuracy on Android)
    val systemSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                viewModel.voiceInputManager.setResultDirectly(spokenText)
                if (inlineVoiceCallback != null) {
                    inlineVoiceCallback?.invoke(spokenText)
                    inlineVoiceCallback = null
                    viewModel.closeVoiceSheet()
                } else {
                    viewModel.processRecognizedVoice(spokenText, isFromDirectSpeech = true)
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.openVoiceSheet()
        } else {
            viewModel.showSnackbar("Для голосового ввода необходимо разрешение на микрофон")
        }
    }

    fun launchDirectVoiceRecognition(customCallback: ((String) -> Unit)? = null) {
        inlineVoiceCallback = customCallback
        try {
            val speechIntent = viewModel.voiceInputManager.createSystemSpeechIntent()
            systemSpeechLauncher.launch(speechIntent)
        } catch (e: Exception) {
            viewModel.showSnackbar("Системный ввод речи недоступен: ${e.localizedMessage}")
        }
    }

    fun requestVoiceInput(customCallback: ((String) -> Unit)? = null) {
        inlineVoiceCallback = customCallback
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.openVoiceSheet()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Handle Snackbar messages
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val voiceBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Ежедневник и Заметки",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (uiState.currentTab == MainTab.PLANNER) "Планировщик задач" else "Личная записная книжка",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                actions = {
                    // Help for voice commands
                    IconButton(
                        onClick = { viewModel.openVoiceHelpDialog() },
                        modifier = Modifier.testTag("top_bar_voice_help_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Справка по голосовым командам",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Quick Voice input action on the top bar
                    IconButton(
                        onClick = { requestVoiceInput() },
                        modifier = Modifier.testTag("top_bar_mic_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Голосовой ввод",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("bottom_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = uiState.currentTab == MainTab.PLANNER,
                    onClick = { viewModel.setTab(MainTab.PLANNER) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == MainTab.PLANNER) Icons.Filled.EventNote else Icons.Outlined.EventNote,
                            contentDescription = null
                        )
                    },
                    label = { Text("Ежедневник", fontWeight = if (uiState.currentTab == MainTab.PLANNER) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("tab_planner_button")
                )

                NavigationBarItem(
                    selected = uiState.currentTab == MainTab.NOTEBOOK,
                    onClick = { viewModel.setTab(MainTab.NOTEBOOK) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == MainTab.NOTEBOOK) Icons.Filled.Description else Icons.Outlined.Description,
                            contentDescription = null
                        )
                    },
                    label = { Text("Записная книжка", fontWeight = if (uiState.currentTab == MainTab.NOTEBOOK) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("tab_notebook_button")
                )
            }
        },
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quick Voice Dictation FAB
                FloatingActionButton(
                    onClick = { requestVoiceInput() },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.testTag("quick_voice_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Голосовой ввод",
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Add Task or Add Note FAB
                ExtendedFloatingActionButton(
                    onClick = {
                        if (uiState.currentTab == MainTab.PLANNER) {
                            viewModel.openAddTaskDialog()
                        } else {
                            viewModel.openAddNoteDialog()
                        }
                    },
                    icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                    text = {
                        Text(
                            text = if (uiState.currentTab == MainTab.PLANNER) "Задача" else "Заметка",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("main_add_fab")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.currentTab) {
                MainTab.PLANNER -> {
                    PlannerScreen(
                        selectedEpochDay = uiState.selectedEpochDay,
                        datesWithTasks = datesWithTasks,
                        tasks = tasks,
                        currentFilter = uiState.taskFilter,
                        onDateSelected = { viewModel.selectDate(it) },
                        onFilterSelected = { viewModel.setTaskFilter(it) },
                        onToggleTask = { viewModel.toggleTask(it) },
                        onEditTask = { viewModel.openEditTaskDialog(it) },
                        onDeleteTask = { viewModel.deleteTask(it) },
                        onAddTask = { viewModel.openAddTaskDialog() },
                        onQuickAddTextTask = { text -> viewModel.quickAddTaskFromText(text) },
                        onOpenVoice = { requestVoiceInput() }
                    )
                }
                MainTab.NOTEBOOK -> {
                    NotebookScreen(
                        notes = notes,
                        searchQuery = uiState.notesSearchQuery,
                        selectedCategory = uiState.selectedNotesCategory,
                        onSearchQueryChange = { viewModel.setNotesSearchQuery(it) },
                        onCategorySelected = { viewModel.setNotesCategory(it) },
                        onNoteClick = { viewModel.openEditNoteDialog(it) },
                        onTogglePin = { viewModel.toggleNotePin(it) },
                        onEditNote = { viewModel.openEditNoteDialog(it) },
                        onDeleteNote = { viewModel.deleteNote(it) },
                        onAddNote = { viewModel.openAddNoteDialog() },
                        onQuickAddTextNote = { text -> viewModel.quickAddNoteFromText(text) },
                        onOpenVoice = { requestVoiceInput() }
                    )
                }
            }
        }
    }

    // --- Dialogs ---

    // 1. Task Add/Edit Dialog
    if (uiState.isTaskDialogVisible) {
        TaskEditDialog(
            initialTask = uiState.editingTask,
            defaultEpochDay = uiState.selectedEpochDay,
            onDismiss = { viewModel.closeTaskDialog() },
            onSave = { id, title, desc, epochDay, time, priority, cat ->
                viewModel.saveTask(
                    id = id,
                    title = title,
                    description = desc,
                    dateEpochDay = epochDay,
                    timeString = time,
                    priority = priority,
                    category = cat,
                    isCompleted = uiState.editingTask?.isCompleted ?: false
                )
            },
            onVoiceInputRequested = { callback ->
                requestVoiceInput(callback)
            }
        )
    }

    // 2. Note Add/Edit Dialog
    if (uiState.isNoteDialogVisible) {
        NoteEditDialog(
            initialNote = uiState.editingNote,
            onDismiss = { viewModel.closeNoteDialog() },
            onSave = { id, title, content, colorIdx, cat, isPinned ->
                viewModel.saveNote(
                    id = id,
                    title = title,
                    content = content,
                    colorIndex = colorIdx,
                    category = cat,
                    isPinned = isPinned
                )
            },
            onVoiceInputRequested = { callback ->
                requestVoiceInput(callback)
            }
        )
    }

    // 3. Voice Recognition & Smart Text Input BottomSheet
    if (uiState.isVoiceSheetVisible) {
        VoiceRecognitionSheet(
            sheetState = voiceBottomSheetState,
            voiceState = voiceState,
            partialText = partialText,
            rmsDb = liveRmsDb,
            parsedResult = uiState.voiceParsedResult,
            detectedCommand = uiState.voiceDetectedCommand,
            autoExecuteCommands = uiState.autoExecuteVoiceCommands,
            onStartListening = { viewModel.voiceInputManager.startListening() },
            onStopListening = { viewModel.voiceInputManager.stopListening() },
            onLaunchSystemSpeech = {
                try {
                    systemSpeechLauncher.launch(viewModel.voiceInputManager.createSystemSpeechIntent())
                } catch (e: Exception) {
                    viewModel.showSnackbar("Не удалось открыть системный ввод речи: ${e.localizedMessage}")
                }
            },
            onOpenHelp = { viewModel.openVoiceHelpDialog() },
            onToggleAutoExecute = { viewModel.toggleAutoExecuteVoiceCommands() },
            onExecuteCommand = { viewModel.executeDetectedCommand() },
            onTextChanged = { text -> viewModel.updateVoiceSheetText(text) },
            onApplyAsTask = { parsed ->
                if (inlineVoiceCallback != null) {
                    inlineVoiceCallback?.invoke(parsed.rawText)
                    viewModel.closeVoiceSheet()
                    inlineVoiceCallback = null
                } else {
                    viewModel.applyVoiceAsTask(parsed)
                }
            },
            onApplyAsNote = { parsed ->
                if (inlineVoiceCallback != null) {
                    inlineVoiceCallback?.invoke(parsed.rawText)
                    viewModel.closeVoiceSheet()
                    inlineVoiceCallback = null
                } else {
                    viewModel.applyVoiceAsNote(parsed)
                }
            },
            onDismiss = {
                viewModel.closeVoiceSheet()
                inlineVoiceCallback = null
            }
        )
    }

    // 4. Voice Commands Help Dialog
    if (uiState.isVoiceHelpDialogVisible) {
        VoiceHelpDialog(
            onDismiss = { viewModel.closeVoiceHelpDialog() }
        )
    }
}
