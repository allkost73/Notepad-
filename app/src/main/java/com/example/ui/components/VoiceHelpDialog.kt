package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun VoiceHelpDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = modifier
                .fillMaxWidth()
                .testTag("voice_help_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BoxIcon(icon = Icons.Default.Mic)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Голосовое управление",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Команды и примеры фраз",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Закрыть")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 1: Navigation
                HelpSection(
                    icon = Icons.Default.Navigation,
                    title = "Переключение разделов",
                    examples = listOf(
                        "«Открой заметки» или «Записная книжка»",
                        "«Открой ежедневник» или «Покажи задачи»"
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Section 2: Date Navigation
                HelpSection(
                    icon = Icons.Default.DateRange,
                    title = "Навигация по календарю",
                    examples = listOf(
                        "«Задачи на завтра» или «Покажи сегодня»",
                        "«Задачи на пятницу», «Задачи на понедельник»",
                        "«Следующий день» или «Предыдущий день»"
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Section 3: Task Filters
                HelpSection(
                    icon = Icons.Default.FilterList,
                    title = "Фильтрация списка задач",
                    examples = listOf(
                        "«Покажи активные» или «В работе»",
                        "«Покажи выполненные» или «Завершенные»",
                        "«Покажи все задачи» (сброс фильтра)"
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Section 4: Search Notes
                HelpSection(
                    icon = Icons.Default.Search,
                    title = "Поиск по заметкам",
                    examples = listOf(
                        "«Найди заметку проект»",
                        "«Поиск пароль»",
                        "«Очистить поиск»"
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Section 5: Direct Actions & Creation
                HelpSection(
                    icon = Icons.Default.EditNote,
                    title = "Быстрое создание голосом",
                    examples = listOf(
                        "«Создай задачу в пятницу в 18:00 тренировка»",
                        "«Напомни завтра в 10 утра позвонить врачу срочно»",
                        "«Создай заметку список покупок сыр и хлеб»"
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Section 6: Task Completion
                HelpSection(
                    icon = Icons.Default.CheckCircle,
                    title = "Отметка и удаление задач",
                    examples = listOf(
                        "«Выполни задачу Купить молоко»",
                        "«Сделано Тренировка»",
                        "«Удали задачу Отчет»"
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Tips Card
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Совет: если на телефоне фоновое распознавание работает неустойчиво, нажмите кнопку «Системный микрофон Google» в окне ввода для максимальной точности.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Понятно", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun BoxIcon(icon: ImageVector) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(40.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun HelpSection(
    icon: ImageVector,
    title: String,
    examples: List<String>
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            examples.forEach { example ->
                Text(
                    text = "• $example",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
    }
}
