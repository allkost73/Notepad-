package com.example.voice

import com.example.ui.planner.MainTab
import com.example.ui.planner.TaskFilter
import com.example.util.DateUtils
import java.util.Calendar

sealed class VoiceCommand {
    data class SwitchTab(val tab: MainTab) : VoiceCommand()
    data class SelectDate(val epochDay: Long, val label: String) : VoiceCommand()
    object NextDay : VoiceCommand()
    object PreviousDay : VoiceCommand()
    data class SetFilter(val filter: TaskFilter) : VoiceCommand()
    data class SearchNotes(val query: String) : VoiceCommand()
    object ClearNotesSearch : VoiceCommand()
    data class CompleteTask(val query: String) : VoiceCommand()
    data class DeleteTask(val query: String) : VoiceCommand()
    data class CreateTaskDirectly(val parsed: ParsedVoiceResult) : VoiceCommand()
    data class CreateNoteDirectly(val title: String, val content: String) : VoiceCommand()
    object ShowHelp : VoiceCommand()
}

data class VoiceCommandDetection(
    val command: VoiceCommand,
    val title: String,
    val description: String
)

object VoiceCommandParser {

    fun detectCommand(rawInput: String): VoiceCommandDetection? {
        val text = rawInput.trim()
        if (text.isEmpty()) return null
        val lower = text.lowercase()

        // 1. Help commands
        if (lower in listOf("помощь", "справка", "команды", "что ты умеешь", "голосовые команды", "список команд", "help")) {
            return VoiceCommandDetection(
                command = VoiceCommand.ShowHelp,
                title = "Справка по командам",
                description = "Открыть список голосовых команд управления"
            )
        }

        // 2. Navigation between Tabs (Ежедневник vs Заметки)
        if (matchesAny(lower, listOf(
                "открой заметки", "открыть заметки", "перейди в заметки", "перейти в заметки",
                "покажи заметки", "показать заметки", "записная книжка", "открой записную книжку",
                "перейди в записную книжку", "блокнот", "открой блокнот", "книжка", "раздел заметок"
            ))) {
            return VoiceCommandDetection(
                command = VoiceCommand.SwitchTab(MainTab.NOTEBOOK),
                title = "Перейти в Заметки",
                description = "Переключение на вкладку записной книжки"
            )
        }

        if (matchesAny(lower, listOf(
                "открой ежедневник", "открыть ежедневник", "перейди в ежедневник", "перейти в ежедневник",
                "покажи ежедневник", "показать ежедневник", "планировщик", "открой планировщик",
                "покажи задачи", "открой задачи", "план на день", "раздел задач", "календарь", "открой календарь"
            ))) {
            return VoiceCommandDetection(
                command = VoiceCommand.SwitchTab(MainTab.PLANNER),
                title = "Перейти в Ежедневник",
                description = "Переключение на вкладку расписания и задач"
            )
        }

        // 3. Navigation between Dates
        if (matchesAny(lower, listOf("покажи сегодня", "задачи на сегодня", "перейди на сегодня", "сегодняшний день", "план на сегодня", "сегодня"))) {
            return VoiceCommandDetection(
                command = VoiceCommand.SelectDate(DateUtils.getTodayEpochDay(), "Сегодня"),
                title = "Перейти на Сегодня",
                description = "Показать расписание на сегодняшний день"
            )
        }

        if (matchesAny(lower, listOf("покажи завтра", "задачи на завтра", "перейди на завтра", "план на завтра", "завтра"))) {
            return VoiceCommandDetection(
                command = VoiceCommand.SelectDate(DateUtils.getTodayEpochDay() + 1, "Завтра"),
                title = "Перейти на Завтра",
                description = "Показать задачи на завтрашний день"
            )
        }

        if (matchesAny(lower, listOf("покажи послезавтра", "задачи на послезавтра", "перейди на послезавтра", "послезавтра"))) {
            return VoiceCommandDetection(
                command = VoiceCommand.SelectDate(DateUtils.getTodayEpochDay() + 2, "Послезавтра"),
                title = "Перейти на Послезавтра",
                description = "Показать задачи на послезавтра"
            )
        }

        if (matchesAny(lower, listOf("покажи вчера", "задачи на вчера", "вчера"))) {
            return VoiceCommandDetection(
                command = VoiceCommand.SelectDate(DateUtils.getTodayEpochDay() - 1, "Вчера"),
                title = "Перейти на Вчера",
                description = "Показать задачи за вчерашний день"
            )
        }

        if (matchesAny(lower, listOf("следующий день", "день вперед", "вперед", "следующая дата"))) {
            return VoiceCommandDetection(
                command = VoiceCommand.NextDay,
                title = "Следующий день",
                description = "Перейти на один день вперед"
            )
        }

        if (matchesAny(lower, listOf("предыдущий день", "день назад", "назад", "прошлый день"))) {
            return VoiceCommandDetection(
                command = VoiceCommand.PreviousDay,
                title = "Предыдущий день",
                description = "Перейти на один день назад"
            )
        }

        // Days of week navigation (e.g. "задачи на пятницу", "покажи вторник")
        val weekdays = listOf(
            listOf("понедельник", "понедельника", "в понедельник") to Calendar.MONDAY,
            listOf("вторник", "вторника", "во вторник") to Calendar.TUESDAY,
            listOf("среду", "среда", "в среду") to Calendar.WEDNESDAY,
            listOf("четверг", "четверга", "в четверг") to Calendar.THURSDAY,
            listOf("пятницу", "пятница", "в пятницу") to Calendar.FRIDAY,
            listOf("субботу", "суббота", "в субботу") to Calendar.SATURDAY,
            listOf("воскресенье", "воскресенья", "в воскресенье") to Calendar.SUNDAY
        )

        for ((synonyms, calDay) in weekdays) {
            for (syn in synonyms) {
                if (lower == syn || lower == "задачи на $syn" || lower == "покажи $syn" || lower == "план на $syn") {
                    val epoch = getNextWeekdayEpochDay(calDay)
                    val label = DateUtils.formatFullDateWithWeekday(epoch)
                    return VoiceCommandDetection(
                        command = VoiceCommand.SelectDate(epoch, label),
                        title = "Перейти на $syn",
                        description = "Показать задачи на $label"
                    )
                }
            }
        }

        // 4. Task Filters (Все, В работе, Выполненные)
        if (matchesAny(lower, listOf("покажи все задачи", "все задачи", "сбрось фильтр", "сбросить фильтр", "покажи все"))) {
            return VoiceCommandDetection(
                command = VoiceCommand.SetFilter(TaskFilter.ALL),
                title = "Фильтр: Все задачи",
                description = "Показать все запланированные задачи"
            )
        }

        if (matchesAny(lower, listOf(
                "покажи активные", "активные задачи", "только активные", "в работе",
                "покажи задачи в работе", "невыполненные задачи", "что осталось сделать"
            ))) {
            return VoiceCommandDetection(
                command = VoiceCommand.SetFilter(TaskFilter.ACTIVE),
                title = "Фильтр: В работе",
                description = "Показать только невыполненные задачи"
            )
        }

        if (matchesAny(lower, listOf(
                "покажи выполненные", "выполненные задачи", "только выполненные", "сделанные задачи",
                "завершенные задачи", "что сделано", "покажи завершенные"
            ))) {
            return VoiceCommandDetection(
                command = VoiceCommand.SetFilter(TaskFilter.COMPLETED),
                title = "Фильтр: Выполненные",
                description = "Показать только завершенные задачи"
            )
        }

        // 5. Notes Search & Filter
        val searchPrefixes = listOf("найди заметку ", "найти заметку ", "поиск заметки ", "поиск ", "найди ", "ищи ")
        for (prefix in searchPrefixes) {
            if (lower.startsWith(prefix)) {
                val query = text.substring(prefix.length).trim().removePrefix(":").trim()
                if (query.isNotEmpty()) {
                    return VoiceCommandDetection(
                        command = VoiceCommand.SearchNotes(query),
                        title = "Поиск заметок: \"$query\"",
                        description = "Найти заметки по запросу \"$query\""
                    )
                }
            }
        }

        if (matchesAny(lower, listOf("очистить поиск", "сбросить поиск", "очисти поиск", "сбрось поиск", "все заметки"))) {
            return VoiceCommandDetection(
                command = VoiceCommand.ClearNotesSearch,
                title = "Очистить поиск заметок",
                description = "Сбросить строку поиска и показать все заметки"
            )
        }

        // 6. Complete Task by Name
        val completePrefixes = listOf(
            "выполни задачу ", "выполнить задачу ", "отметь выполненной ", "отметить выполненной ",
            "отметь задачу ", "сделано ", "сделана задача ", "заверши задачу "
        )
        for (prefix in completePrefixes) {
            if (lower.startsWith(prefix)) {
                val taskName = text.substring(prefix.length).trim()
                if (taskName.isNotEmpty()) {
                    return VoiceCommandDetection(
                        command = VoiceCommand.CompleteTask(taskName),
                        title = "Отметить выполненной",
                        description = "Задача: \"$taskName\""
                    )
                }
            }
        }

        // 7. Delete Task by Name
        val deletePrefixes = listOf("удали задачу ", "удалить задачу ", "удали задачу с названием ")
        for (prefix in deletePrefixes) {
            if (lower.startsWith(prefix)) {
                val taskName = text.substring(prefix.length).trim()
                if (taskName.isNotEmpty()) {
                    return VoiceCommandDetection(
                        command = VoiceCommand.DeleteTask(taskName),
                        title = "Удалить задачу",
                        description = "Задача: \"$taskName\""
                    )
                }
            }
        }

        // 8. Explicit Command to Create Note ("создай заметку ...", "запиши заметку ...")
        val createNotePrefixes = listOf(
            "создай заметку ", "создать заметку ", "добавь заметку ", "добавить заметку ",
            "запиши заметку ", "новая заметка ", "напиши в блокнот ", "заметка "
        )
        for (prefix in createNotePrefixes) {
            if (lower.startsWith(prefix)) {
                val noteBody = text.substring(prefix.length).trim()
                if (noteBody.isNotEmpty()) {
                    val lines = noteBody.lines()
                    val title = lines.firstOrNull()?.take(50) ?: "Заметка"
                    return VoiceCommandDetection(
                        command = VoiceCommand.CreateNoteDirectly(title, noteBody),
                        title = "Создать заметку",
                        description = "\"$noteBody\""
                    )
                }
            }
        }

        // 9. Explicit Command to Create Task ("создай задачу ...", "добавь задачу ...", "напомни ...")
        val createTaskPrefixes = listOf(
            "создай задачу ", "создать задачу ", "добавь задачу ", "добавить задачу ",
            "напомни мне ", "напомни ", "запиши задачу ", "поставь задачу "
        )
        for (prefix in createTaskPrefixes) {
            if (lower.startsWith(prefix)) {
                val taskBody = text.substring(prefix.length).trim()
                if (taskBody.isNotEmpty()) {
                    val parsed = SmartVoiceParser.parse(taskBody)
                    return VoiceCommandDetection(
                        command = VoiceCommand.CreateTaskDirectly(parsed),
                        title = "Добавить задачу",
                        description = "${parsed.cleanedTitle} (${DateUtils.getRelativeDayLabel(parsed.suggestedEpochDay)}${if (parsed.suggestedTimeString.isNotEmpty()) " в " + parsed.suggestedTimeString else ""})"
                    )
                }
            }
        }

        return null
    }

    private fun matchesAny(text: String, patterns: List<String>): Boolean {
        return patterns.any { p -> text == p || text.startsWith("$p ") }
    }

    private fun getNextWeekdayEpochDay(targetCalDay: Int): Long {
        val cal = Calendar.getInstance()
        val currentCalDay = cal.get(Calendar.DAY_OF_WEEK)
        var daysUntil = (targetCalDay - currentCalDay + 7) % 7
        if (daysUntil == 0) daysUntil = 7
        cal.add(Calendar.DAY_OF_YEAR, daysUntil)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis / (24 * 60 * 60 * 1000L)
    }
}
