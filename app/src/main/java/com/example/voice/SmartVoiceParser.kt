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

        var workingText = trimmed
        var targetEpochDay = DateUtils.getTodayEpochDay()
        var timeStr = ""
        var priority = Priority.MEDIUM
        var isTask = false

        // 1. Strip common dictation prefix verbs ("напомни мне", "добавь задачу", "нужно", etc.)
        val actionPrefixes = listOf(
            "(?i)^напомни мне\\s+",
            "(?i)^напомни\\s+",
            "(?i)^добавь задачу\\s+",
            "(?i)^добавить задачу\\s+",
            "(?i)^создай задачу\\s+",
            "(?i)^создать задачу\\s+",
            "(?i)^запиши задачу\\s+",
            "(?i)^поставь задачу\\s+",
            "(?i)^запиши в план\\s+",
            "(?i)^запиши\\s+",
            "(?i)^записать\\s+",
            "(?i)^нужно\\s+",
            "(?i)^надо\\s+",
            "(?i)^не забыть\\s+",
            "(?i)^планирую\\s+"
        )
        for (regex in actionPrefixes) {
            if (workingText.contains(Regex(regex))) {
                workingText = workingText.replaceFirst(Regex(regex), "").trim()
                isTask = true
            }
        }

        val lower = workingText.lowercase()

        // 2. Check for date keywords in Russian
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
        } else if (lower.contains("на выходных") || lower.contains("в выходные")) {
            targetEpochDay = getNextWeekdayEpochDay(Calendar.SATURDAY)
            workingText = workingText.replace(Regex("(?i)\\b(на выходных|в выходные)\\b"), "").trim()
            isTask = true
        }

        // Check for days of the week in Russian
        val weekdays = listOf(
            listOf("в понедельник", "во вторник", "в среду", "в четверг", "в пятницу", "в субботу", "в воскресенье"),
            listOf("понедельник", "вторник", "среду", "четверг", "пятницу", "субботу", "воскресенье")
        )
        val calDays = listOf(
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
        )

        for (i in calDays.indices) {
            val phraseWithPreposition = weekdays[0][i]
            val phraseWithoutPreposition = weekdays[1][i]
            if (workingText.contains(Regex("(?i)\\b$phraseWithPreposition\\b"))) {
                workingText = workingText.replace(Regex("(?i)\\b$phraseWithPreposition\\b"), "").trim()
                targetEpochDay = getNextWeekdayEpochDay(calDays[i])
                isTask = true
                break
            } else if (workingText.contains(Regex("(?i)\\b$phraseWithoutPreposition\\b"))) {
                workingText = workingText.replace(Regex("(?i)\\b$phraseWithoutPreposition\\b"), "").trim()
                targetEpochDay = getNextWeekdayEpochDay(calDays[i])
                isTask = true
                break
            }
        }

        // 3. Check for time in Russian
        // Format: "в 14:30", "15.00", "в 14 30"
        val timeColonPattern = Pattern.compile("(?i)\\b(?:в|к)?\\s*([0-1]?[0-9]|2[0-3])[:.\\s]([0-5][0-9])\\b")
        val colonMatcher = timeColonPattern.matcher(workingText)
        if (colonMatcher.find()) {
            val h = colonMatcher.group(1)?.toIntOrNull() ?: 0
            val m = colonMatcher.group(2)?.toIntOrNull() ?: 0
            timeStr = String.format("%02d:%02d", h, m)
            workingText = colonMatcher.replaceFirst("").trim()
            isTask = true
        } else {
            // Check for words like "в 9 утра", "в 18 часов", "в 6 вечера", "в 3 часа"
            val timeHourPattern = Pattern.compile("(?i)\\b(?:в|к)?\\s*([0-1]?[0-9]|2[0-3])\\s*(?:часов|часа|час|ч|утра|вечера|дня)\\b")
            val hourMatcher = timeHourPattern.matcher(workingText)
            if (hourMatcher.find()) {
                val matched = hourMatcher.group(0) ?: ""
                var h = hourMatcher.group(1)?.toIntOrNull() ?: 0
                if ((matched.contains("вечера") || matched.contains("дня")) && h in 1..11) {
                    h += 12
                }
                timeStr = String.format("%02d:00", h)
                workingText = hourMatcher.replaceFirst("").trim()
                isTask = true
            } else {
                // Word hours: "в полдень", "утром", "днем", "вечером", "в обед"
                if (workingText.contains(Regex("(?i)\\b(в полдень|в обед)\\b"))) {
                    timeStr = "13:00"
                    workingText = workingText.replace(Regex("(?i)\\b(в полдень|в обед)\\b"), "").trim()
                    isTask = true
                } else if (workingText.contains(Regex("(?i)\\b(утром|с утра)\\b"))) {
                    timeStr = "09:00"
                    workingText = workingText.replace(Regex("(?i)\\b(утром|с утра)\\b"), "").trim()
                    isTask = true
                } else if (workingText.contains(Regex("(?i)\\bвечером\\b"))) {
                    timeStr = "19:00"
                    workingText = workingText.replace(Regex("(?i)\\bвечером\\b"), "").trim()
                    isTask = true
                } else if (workingText.contains(Regex("(?i)\\bднем\\b"))) {
                    timeStr = "14:00"
                    workingText = workingText.replace(Regex("(?i)\\bднем\\b"), "").trim()
                    isTask = true
                }
            }
        }

        // 4. Check for Priority keywords
        val lowerUpdated = workingText.lowercase()
        if (lowerUpdated.contains("срочно") || lowerUpdated.contains("очень важно") || lowerUpdated.contains("высокий приоритет") || lowerUpdated.contains("главная задача") || lowerUpdated.contains("критично")) {
            priority = Priority.HIGH
            workingText = workingText.replace(Regex("(?i)\\b(срочно|очень важно|высокий приоритет|главная задача|критично)\\b"), "").trim()
        } else if (lowerUpdated.contains("не срочно") || lowerUpdated.contains("низкий приоритет") || lowerUpdated.contains("когда будет время") || lowerUpdated.contains("не к спеху")) {
            priority = Priority.LOW
            workingText = workingText.replace(Regex("(?i)\\b(не срочно|низкий приоритет|когда будет время|не к спеху)\\b"), "").trim()
        }

        // 5. Check for Category keywords
        var category = TaskCategory.WORK
        val catLower = workingText.lowercase()
        if (catLower.contains("идея") || catLower.contains("мысль") || catLower.contains("задумк") || catLower.contains("проект")) {
            category = TaskCategory.IDEAS
        } else if (catLower.contains("купить") || catLower.contains("магазин") || catLower.contains("заказать") || catLower.contains("список покупок") || catLower.contains("продукты")) {
            category = TaskCategory.SHOPPING
            isTask = true
        } else if (catLower.contains("врач") || catLower.contains("больниц") || catLower.contains("аптек") || catLower.contains("таблетк") || catLower.contains("тренировк") || catLower.contains("спорт") || catLower.contains("бег") || catLower.contains("стоматолог")) {
            category = TaskCategory.HEALTH
            isTask = true
        } else if (catLower.contains("урок") || catLower.contains("учеб") || catLower.contains("лекци") || catLower.contains("экзамен") || catLower.contains("дз") || catLower.contains("книг") || catLower.contains("курс")) {
            category = TaskCategory.STUDY
            isTask = true
        } else if (catLower.contains("семья") || catLower.contains("дом") || catLower.contains("уборк") || catLower.contains("позвонить") || catLower.contains("личн") || catLower.contains("маме") || catLower.contains("папе")) {
            category = TaskCategory.PERSONAL
        }

        // 6. Clean up dangling prepositions and punctuation leftovers
        workingText = workingText
            .replace(Regex("^[\\s,.-]+"), "")
            .replace(Regex("[\\s,.-]+$"), "")
            .replace(Regex("^(в|к|на|до|с|по)\\s+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+(в|к|на|до|с|по)$", RegexOption.IGNORE_CASE), "")
            .trim()

        if (workingText.isEmpty()) {
            workingText = trimmed
        }

        // Capitalize first letter
        val cleanedTitle = workingText.replaceFirstChar {
            if (it.isLowerCase()) it.uppercase(java.util.Locale("ru", "RU")) else it.toString()
        }

        if (!isTask && trimmed.length > 80) {
            isTask = false
        } else if (isTask || timeStr.isNotEmpty() || cleanedTitle.length < 70) {
            isTask = true
        }

        return ParsedVoiceResult(
            rawText = trimmed,
            cleanedTitle = cleanedTitle,
            description = if (trimmed != cleanedTitle) "Распознано: \"$trimmed\"" else "",
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
