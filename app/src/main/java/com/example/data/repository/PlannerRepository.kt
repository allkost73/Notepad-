package com.example.data.repository

import com.example.data.dao.PlannerDao
import com.example.data.model.DailyTask
import com.example.data.model.Note
import kotlinx.coroutines.flow.Flow

class PlannerRepository(private val dao: PlannerDao) {

    // --- Tasks ---
    fun getTasksForDate(epochDay: Long): Flow<List<DailyTask>> = dao.getTasksForDate(epochDay)

    fun getAllTasks(): Flow<List<DailyTask>> = dao.getAllTasks()

    fun getDatesWithActiveTasks(): Flow<List<Long>> = dao.getDatesWithActiveTasks()

    fun getTotalTasksCountForDate(epochDay: Long): Flow<Int> = dao.getTotalTasksCountForDate(epochDay)

    fun getCompletedTasksCountForDate(epochDay: Long): Flow<Int> = dao.getCompletedTasksCountForDate(epochDay)

    suspend fun insertTask(task: DailyTask): Long = dao.insertTask(task)

    suspend fun updateTask(task: DailyTask) = dao.updateTask(task)

    suspend fun deleteTask(task: DailyTask) = dao.deleteTask(task)

    suspend fun deleteTaskById(taskId: Long) = dao.deleteTaskById(taskId)

    suspend fun setTaskCompleted(taskId: Long, completed: Boolean) = dao.setTaskCompleted(taskId, completed)

    // --- Notes ---
    fun getAllNotes(): Flow<List<Note>> = dao.getAllNotes()

    fun searchNotes(query: String): Flow<List<Note>> = dao.searchNotes(query)

    suspend fun getNoteById(noteId: Long): Note? = dao.getNoteById(noteId)

    suspend fun insertNote(note: Note): Long = dao.insertNote(note)

    suspend fun updateNote(note: Note) = dao.updateNote(note)

    suspend fun deleteNote(note: Note) = dao.deleteNote(note)

    suspend fun deleteNoteById(noteId: Long) = dao.deleteNoteById(noteId)

    suspend fun setNotePinned(noteId: Long, isPinned: Boolean) = dao.setNotePinned(noteId, isPinned)
}
