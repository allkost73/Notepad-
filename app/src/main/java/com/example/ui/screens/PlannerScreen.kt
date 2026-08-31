package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyTask
import com.example.ui.components.CalendarStrip
import com.example.ui.components.DailyProgressCard
import com.example.ui.components.EmptyStateView
import com.example.ui.components.TaskCard
import com.example.ui.planner.TaskFilter

@Composable
fun PlannerScreen(
    selectedEpochDay: Long,
    datesWithTasks: List<Long>,
    tasks: List<DailyTask>,
    currentFilter: TaskFilter,
    onDateSelected: (Long) -> Unit,
    onFilterSelected: (TaskFilter) -> Unit,
    onToggleTask: (DailyTask) -> Unit,
    onEditTask: (DailyTask) -> Unit,
    onDeleteTask: (DailyTask) -> Unit,
    onAddTask: () -> Unit,
    onQuickAddTextTask: (String) -> Unit,
    onOpenVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalTasks = tasks.size
    val completedTasks = tasks.count { it.isCompleted }

    var quickTaskText by remember { mutableStateOf("") }

    val filteredTasks = when (currentFilter) {
        TaskFilter.ALL -> tasks
        TaskFilter.ACTIVE -> tasks.filter { !it.isCompleted }
        TaskFilter.COMPLETED -> tasks.filter { it.isCompleted }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Horizontal Week Calendar Strip
        CalendarStrip(
            selectedEpochDay = selectedEpochDay,
            datesWithTasks = datesWithTasks,
            onDateSelected = onDateSelected
        )

        // Quick Text Input Bar
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = quickTaskText,
                    onValueChange = { quickTaskText = it },
                    placeholder = { Text("Быстрый ввод: «В 18:00 тренировка»", fontSize = 13.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (quickTaskText.isNotBlank()) {
                                onQuickAddTextTask(quickTaskText)
                                quickTaskText = ""
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("planner_quick_task_input")
                )

                Spacer(modifier = Modifier.width(6.dp))

                if (quickTaskText.isNotBlank()) {
                    IconButton(
                        onClick = {
                            if (quickTaskText.isNotBlank()) {
                                onQuickAddTextTask(quickTaskText)
                                quickTaskText = ""
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .testTag("planner_quick_add_submit_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Добавить задачу",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = onOpenVoice,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .testTag("planner_quick_mic_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Голосовой ввод",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("planner_tasks_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Daily Progress Summary Card
            item(key = "progress_card") {
                DailyProgressCard(
                    selectedEpochDay = selectedEpochDay,
                    totalTasks = totalTasks,
                    completedTasks = completedTasks
                )
            }

            // Filter Tabs Row
            item(key = "filter_row") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskFilter.values().forEach { filter ->
                        val count = when (filter) {
                            TaskFilter.ALL -> totalTasks
                            TaskFilter.ACTIVE -> totalTasks - completedTasks
                            TaskFilter.COMPLETED -> completedTasks
                        }

                        FilterChip(
                            selected = currentFilter == filter,
                            onClick = { onFilterSelected(filter) },
                            label = {
                                Text(
                                    text = "${filter.titleRu} ($count)",
                                    fontSize = 12.sp,
                                    fontWeight = if (currentFilter == filter) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.testTag("filter_chip_${filter.name}")
                        )
                    }
                }
            }

            // Section Header
            item(key = "section_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Задачи на день",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Empty State or List of Tasks
            if (filteredTasks.isEmpty()) {
                item(key = "empty_state") {
                    val msg = when (currentFilter) {
                        TaskFilter.ALL -> "На этот день еще нет задач. Добавьте задачу через поле ввода выше или продиктуйте голосом!"
                        TaskFilter.ACTIVE -> "Все текущие задачи на этот день выполнены!"
                        TaskFilter.COMPLETED -> "Пока нет выполненных задач в этот день."
                    }
                    EmptyStateView(
                        icon = Icons.Default.EventNote,
                        title = if (currentFilter == TaskFilter.ALL) "Список задач пуст" else "Задачи не найдены",
                        description = msg,
                        actionText = if (currentFilter == TaskFilter.ALL) "Добавить задачу" else null,
                        onActionClick = if (currentFilter == TaskFilter.ALL) onAddTask else null
                    )
                }
            } else {
                items(filteredTasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onToggleCompleted = { onToggleTask(task) },
                        onEdit = { onEditTask(task) },
                        onDelete = { onDeleteTask(task) }
                    )
                }
            }
        }
    }
}
