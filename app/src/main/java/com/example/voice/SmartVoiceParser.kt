package com.example.voice

import com.example.data.model.Priority
import com.example.data.model.TaskCategory
import com.example.util.DateUtils
import java.util.Calendar
import java.util.regex.Pattern

data class ParsedVoiceResult(
    val rawText: String,
    val cleanedTitle: String,
    val description: String,
    val suggestedEpochDay: Long,
    val suggestedTimeString: String,
    val suggestedPriority: Priority,
    val suggestedCategory: TaskCategory,
    val isSuggestedAsTask: Boolean
)

object SmartVoiceParser {

    fun parse(text: String): ParsedVoiceResult {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return ParsedVoiceResult(
                rawText = "",
                cleanedTitle = "",
                description = "",
                suggestedEpochDay = DateUtils.getTodayEpochDay(),
                suggestedTimeString = "",
                suggestedPriority = Priority.MEDIUM,
                suggestedCategory = TaskCategory.WORK,
                isSuggestedAsTask = true
            )
        }

        val lower = trimmed.lowercase()
        var workingText = trimmed
        var targetEpochDay = DateUtils.getTodayEpochDay()
        var timeStr = ""
        var priority = Priority.MEDIUM
        var isTask = false

        // Check for date keywords in Russian
        if (lower.contains("послезавтра")) {
            targetEpochDay = DateUtils.getTodayEpochDay() + 2
            workingText = workingText.replace(Regex("(?i)\\bпослезавтра\\b"), "").trim()
            isTask = true
        } else if (lower.contains("завтра")) {
            targetEpochDay = DateUtils.getTodayEpochDay() + 1
            workingText = workingText.replace(Regex("(?i)\\bзавтра\\b"), "").trim()
            isTask = true
        } else if (lower.contains("сегодня")) {
            targetEpochDay = DateUtils.getTodayEpochDay()
            workingText = workingText.replace(Regex("(?i)\\bсегодня\\b"), "").trim()
            isTask = true
        } else if (lower.contains("вчера")) {
            targetEpochDay = DateUtils.getTodayEpochDay() - 1
            workingText = workingText.replace(Regex("(?i)\\bвчера\\b"), "").trim()
        }

        // Check for days of the week in Russian
        val weekdays = listOf(
            "в понедельник" to Calendar.MONDAY,
            "во вторник" to Calendar.TUESDAY,
            "в среду" to Calendar.WEDNESDAY,
            "в четверг" to Calendar.THURSDAY,
            "в пятницу" to Calendar.FRIDAY,
            "в субботу" to Calendar.SATURDAY,
            "в воскресенье" to Calendar.SUNDAY
        )

        for ((phrase, calDay) in weekdays) {
            if (lower.contains(phrase)) {
                workingText = workingText.replace(Regex("(?i)\\b$phrase\\b"), "").trim()
                targetEpochDay = getNextWeekdayEpochDay(calDay)
                isTask = true
                break
            }
        }

        // Check for time: e.g. "в 14:30", "в 15 00", "в 9 утра", "в 18 часов"
        val timeColonPattern = Pattern.compile("(?i)\\b(?:в|к)?\\s*([0-1]?[0-9]|2[0-3])[:.]([0-5][0-9])\\b")
        val colonMatcher = timeColonPattern.matcher(workingText)
        if (colonMatcher.find()) {
            val h = colonMatcher.group(1)?.toIntOrNull() ?: 0
            val m = colonMatcher.group(2)?.toIntOrNull() ?: 0
            timeStr = String.format("%02d:%02d", h, m)
            workingText = colonMatcher.replaceFirst("").trim()
            isTask = true
        } else {
            val timeHourPattern = Pattern.compile("(?i)\\b(?:в|к)?\\s*([0-1]?[0-9]|2[0-3])\\s*(?:часов|часа|час|ч|утра|вечера|дня)?\\b")
            val hourMatcher = timeHourPattern.matcher(workingText)
            if (hourMatcher.find()) {
                val matched = hourMatcher.group(0) ?: ""
                if (matched.contains("утра") || matched.contains("вечера") || matched.contains("дня") || matched.contains("час") || matched.startsWith("в ") || matched.startsWith("к ")) {
                    var h = hourMatcher.group(1)?.toIntOrNull() ?: 0
                    if ((matched.contains("вечера") || matched.contains("дня")) && h in 1..11) {
                        h += 12
                    }
                    timeStr = String.format("%02d:00", h)
                    workingText = hourMatcher.replaceFirst("").trim()
                    isTask = true
                }
            }
        }

        // Check for Priority keywords
        if (lower.contains("срочно") || lower.contains("очень важно") || lower.contains("высокий приоритет") || lower.contains("главная задача")) {
            priority = Priority.HIGH
            workingText = workingText.replace(Regex("(?i)\\b(срочно|очень важно|высокий приоритет|главная задача)\\b"), "").trim()
        } else if (lower.contains("не срочно") || lower.contains("низкий приоритет") || lower.contains("когда будет время")) {
            priority = Priority.LOW
            workingText = workingText.replace(Regex("(?i)\\b(не срочно|низкий приоритет|когда будет время)\\b"), "").trim()
        }

        // Check for Category
        var category = TaskCategory.WORK
        if (lower.contains("идея") || lower.contains("мысль") || lower.contains("задумк") || lower.contains("проект")) {
            category = TaskCategory.IDEAS
        } else if (lower.contains("купить") || lower.contains("магазин") || lower.contains("заказать") || lower.contains("список покупок")) {
            category = TaskCategory.SHOPPING
            isTask = true
        } else if (lower.contains("врач") || lower.contains("больниц") || lower.contains("аптек") || lower.contains("таблетк") || lower.contains("тренировк") || lower.contains("спорт") || lower.contains("бег")) {
            category = TaskCategory.HEALTH
            isTask = true
        } else if (lower.contains("урок") || lower.contains("учеб") || lower.contains("лекци") || lower.contains("экзамен") || lower.contains("дз") || lower.contains("книг")) {
            category = TaskCategory.STUDY
            isTask = true
        } else if (lower.contains("семья") || lower.contains("дом") || lower.contains("уборк") || lower.contains("позвонить мам") || lower.contains("личн")) {
            category = TaskCategory.PERSONAL
        }

        // Clean up punctuation leftovers
        workingText = workingText.replace(Regex("^[\\s,.-]+"), "").replace(Regex("[\\s,.-]+$"), "").trim()
        if (workingText.isEmpty()) {
            workingText = trimmed
        }

        // Capitalize first letter
        val cleanedTitle = workingText.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }

        // If no time and no date keyword, and long text (> 100 chars), might be a note
        if (!isTask && trimmed.length > 80) {
            isTask = false
        } else if (isTask || timeStr.isNotEmpty() || cleanedTitle.length < 70) {
            isTask = true
        }

        return ParsedVoiceResult(
            rawText = trimmed,
            cleanedTitle = cleanedTitle,
            description = if (trimmed != cleanedTitle) "Распознано голосом: \"$trimmed\"" else "",
            suggestedEpochDay = targetEpochDay,
            suggestedTimeString = timeStr,
            suggestedPriority = priority,
            suggestedCategory = category,
            isSuggestedAsTask = isTask
        )
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
