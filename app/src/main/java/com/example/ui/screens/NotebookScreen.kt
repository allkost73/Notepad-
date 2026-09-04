package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
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
import com.example.data.model.Note
import com.example.ui.components.EmptyStateView
import com.example.ui.components.NoteCard

@Composable
fun NotebookScreen(
    notes: List<Note>,
    searchQuery: String,
    selectedCategory: String,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onNoteClick: (Note) -> Unit,
    onTogglePin: (Note) -> Unit,
    onEditNote: (Note) -> Unit,
    onDeleteNote: (Note) -> Unit,
    onAddNote: () -> Unit,
    onQuickAddTextNote: (String) -> Unit,
    onOpenVoice: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var quickNoteText by remember { mutableStateOf("") }

    // Extract unique categories from notes
    val categories = remember(notes) {
        val unique = notes.map { it.category }.filter { it.isNotBlank() }.distinct()
        listOf("Все") + unique
    }

    val filteredByCategory = if (selectedCategory == "Все") {
        notes
    } else {
        notes.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    val pinnedNotes = filteredByCategory.filter { it.isPinned }
    val otherNotes = filteredByCategory.filter { !it.isPinned }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Quick Note Text Input Bar
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
                    value = quickNoteText,
                    onValueChange = { quickNoteText = it },
                    placeholder = { Text("Быстрая запись новой мысли или заметки...", fontSize = 13.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (quickNoteText.isNotBlank()) {
                                onQuickAddTextNote(quickNoteText)
                                quickNoteText = ""
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
                        .testTag("notebook_quick_input")
                )

                Spacer(modifier = Modifier.width(6.dp))

                if (quickNoteText.isNotBlank()) {
                    IconButton(
                        onClick = {
                            if (quickNoteText.isNotBlank()) {
                                onQuickAddTextNote(quickNoteText)
                                quickNoteText = ""
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .testTag("notebook_quick_submit_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Сохранить заметку",
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
                            .testTag("notebook_quick_mic_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Голосовой ввод заметки",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Поиск заметок по названию или тексту...", fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Очистить",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .testTag("notes_search_input")
        )

        // Categories Horizontal Row
        if (categories.size > 1) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { onCategorySelected(category) },
                        label = {
                            Text(
                                text = category,
                                fontSize = 12.sp,
                                fontWeight = if (selectedCategory == category) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("category_chip_$category")
                    )
                }
            }
        }

        // Notes List
        if (filteredByCategory.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Description,
                title = if (searchQuery.isBlank()) "Записная книжка пуста" else "Ничего не найдено",
                description = if (searchQuery.isBlank())
                    "Создайте первую заметку через поле ввода выше или продиктуйте мысль голосом!"
                else
                    "По запросу \"$searchQuery\" заметки не найдены.",
                actionText = if (searchQuery.isBlank()) "Создать заметку" else null,
                onActionClick = if (searchQuery.isBlank()) onAddNote else null
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("notes_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Pinned Section
                if (pinnedNotes.isNotEmpty()) {
                    item(key = "pinned_header") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Закрепленные (${pinnedNotes.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    items(pinnedNotes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onNoteClick = { onNoteClick(note) },
                            onTogglePin = { onTogglePin(note) },
                            onEdit = { onEditNote(note) },
                            onDelete = { onDeleteNote(note) }
                        )
                    }

                    if (otherNotes.isNotEmpty()) {
                        item(key = "other_header") {
                            Text(
                                text = "Другие заметки",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                            )
                        }
                    }
                }

                // Regular Notes
                items(otherNotes, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        onNoteClick = { onNoteClick(note) },
                        onTogglePin = { onTogglePin(note) },
                        onEdit = { onEditNote(note) },
                        onDelete = { onDeleteNote(note) }
                    )
                }
            }
        }
    }
}
