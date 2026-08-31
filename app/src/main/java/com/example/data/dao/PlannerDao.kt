package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DailyTask
import com.example.data.model.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannerDao {

    // --- Tasks Queries ---
    @Query("SELECT * FROM daily_tasks WHERE dateEpochDay = :epochDay ORDER BY isCompleted ASC, priority DESC, id DESC")
    fun getTasksForDate(epochDay: Long): Flow<List<DailyTask>>

    @Query("SELECT * FROM daily_tasks ORDER BY dateEpochDay ASC, isCompleted ASC, priority DESC")
    fun getAllTasks(): Flow<List<DailyTask>>

    @Query("SELECT DISTINCT dateEpochDay FROM daily_tasks WHERE isCompleted = 0")
    fun getDatesWithActiveTasks(): Flow<List<Long>>

    @Query("SELECT COUNT(*) FROM daily_tasks WHERE dateEpochDay = :epochDay")
    fun getTotalTasksCountForDate(epochDay: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM daily_tasks WHERE dateEpochDay = :epochDay AND isCompleted = 1")
    fun getCompletedTasksCountForDate(epochDay: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: DailyTask): Long

    @Update
    suspend fun updateTask(task: DailyTask)

    @Delete
    suspend fun deleteTask(task: DailyTask)

    @Query("DELETE FROM daily_tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Long)

    @Query("UPDATE daily_tasks SET isCompleted = :completed WHERE id = :taskId")
    suspend fun setTaskCompleted(taskId: Long, completed: Boolean)

    // --- Notes Queries ---
    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY isPinned DESC, updatedAt DESC")
    fun searchNotes(query: String): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: Long): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: Long)

    @Query("UPDATE notes SET isPinned = :isPinned WHERE id = :noteId")
    suspend fun setNotePinned(noteId: Long, isPinned: Boolean)
}
