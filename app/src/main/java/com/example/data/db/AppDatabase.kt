package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.PlannerDao
import com.example.data.model.DailyTask
import com.example.data.model.Note
import com.example.data.model.Priority
import com.example.data.model.TaskCategory
import com.example.util.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [DailyTask::class, Note::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun plannerDao(): PlannerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "daily_planner_database"
                )
                    .addCallback(PlannerDatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class PlannerDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database.plannerDao())
                }
            }
        }

        private suspend fun populateInitialData(dao: PlannerDao) {
            val today = DateUtils.getTodayEpochDay()
            val tomorrow = today + 1

            // Initial Starter Tasks in Russian
            dao.insertTask(
                DailyTask(
                    title = "Утреннее планирование дня",
                    description = "Распределить приоритеты, проверить почту и расписание встреч",
                    dateEpochDay = today,
                    timeString = "09:00",
                    priority = Priority.HIGH,
                    category = TaskCategory.WORK,
                    isCompleted = true
                )
            )

            dao.insertTask(
                DailyTask(
                    title = "Попробовать голосовой ввод 🎙️",
                    description = "Нажмите на кнопку микрофона и продиктуйте новую задачу или заметку",
                    dateEpochDay = today,
                    timeString = "12:00",
                    priority = Priority.HIGH,
                    category = TaskCategory.IDEAS,
                    isCompleted = false
                )
            )

            dao.insertTask(
                DailyTask(
                    title = "Купить свежие фрукты и чай",
                    description = "Яблоки, апельсины, зеленый чай с жасмином",
                    dateEpochDay = today,
                    timeString = "18:30",
                    priority = Priority.MEDIUM,
                    category = TaskCategory.SHOPPING,
                    isCompleted = false
                )
            )

            dao.insertTask(
                DailyTask(
                    title = "Вечерняя пробежка / тренировка",
                    description = "30 минут кардио или легкая растяжка",
                    dateEpochDay = today,
                    timeString = "20:00",
                    priority = Priority.LOW,
                    category = TaskCategory.HEALTH,
                    isCompleted = false
                )
            )

            dao.insertTask(
                DailyTask(
                    title = "Встреча по новому проекту",
                    description = "Обсуждение макетов и архитектуры",
                    dateEpochDay = tomorrow,
                    timeString = "14:00",
                    priority = Priority.HIGH,
                    category = TaskCategory.WORK,
                    isCompleted = false
                )
            )

            // Initial Starter Notes in Russian
            dao.insertNote(
                Note(
                    title = "Добро пожаловать в Ежедневник! 📝",
                    content = "Ваш персональный органайзер объединяет:\n" +
                            "• Ежедневник с календарём и расписанием задач по часам\n" +
                            "• Записную книжку для идей, списков и заметок с цветными карточками\n" +
                            "• Голосовой ввод — просто нажимайте на микрофон и говорите!",
                    colorIndex = 1,
                    category = "Инструкция",
                    isPinned = true
                )
            )

            dao.insertNote(
                Note(
                    title = "Как работает голосовой ввод 🎙️",
                    content = "Вы можете:\n" +
                            "1. Нажать микрофон внизу экрана для быстрой голосовой записи\n" +
                            "2. Сказать например: 'Завтра в 15:00 созвон с коллегами'\n" +
                            "3. Приложение автоматически распознает текст и предложит сохранить в Задачи или Заметки!",
                    colorIndex = 2,
                    category = "Подсказка",
                    isPinned = true
                )
            )

            dao.insertNote(
                Note(
                    title = "Список полезных привычек",
                    content = "1. Выпивать стакан воды утром\n2. Читать 20 страниц книги\n3. Планировать задачи с вечера\n4. Прогулка на свежем воздухе",
                    colorIndex = 3,
                    category = "Саморазвитие",
                    isPinned = false
                )
            )
        }
    }
}
