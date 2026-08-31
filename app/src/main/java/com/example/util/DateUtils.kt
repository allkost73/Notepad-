package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val russianLocale = Locale("ru", "RU")

    fun getTodayEpochDay(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis / (24 * 60 * 60 * 1000L)
    }

    fun epochDayToMillis(epochDay: Long): Long {
        return epochDay * (24 * 60 * 60 * 1000L)
    }

    fun millisToEpochDay(millis: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis / (24 * 60 * 60 * 1000L)
    }

    fun formatDayMonth(epochDay: Long): String {
        val date = Date(epochDayToMillis(epochDay))
        val sdf = SimpleDateFormat("d MMMM", russianLocale)
        return sdf.format(date)
    }

    fun formatFullDateWithWeekday(epochDay: Long): String {
        val date = Date(epochDayToMillis(epochDay))
        val sdf = SimpleDateFormat("EEEE, d MMMM", russianLocale)
        val result = sdf.format(date)
        return result.replaceFirstChar { if (it.isLowerCase()) it.titlecase(russianLocale) else it.toString() }
    }

    fun formatWeekdayShort(epochDay: Long): String {
        val date = Date(epochDayToMillis(epochDay))
        val sdf = SimpleDateFormat("EE", russianLocale)
        val result = sdf.format(date)
        return result.replaceFirstChar { if (it.isLowerCase()) it.titlecase(russianLocale) else it.toString() }
    }

    fun formatDayNumber(epochDay: Long): String {
        val date = Date(epochDayToMillis(epochDay))
        val sdf = SimpleDateFormat("d", russianLocale)
        return sdf.format(date)
    }

    fun formatMonthYear(epochDay: Long): String {
        val date = Date(epochDayToMillis(epochDay))
        val sdf = SimpleDateFormat("LLLL yyyy", russianLocale)
        val result = sdf.format(date)
        return result.replaceFirstChar { if (it.isLowerCase()) it.titlecase(russianLocale) else it.toString() }
    }

    fun formatNoteTimestamp(millis: Long): String {
        val date = Date(millis)
        val sdf = SimpleDateFormat("d MMM, HH:mm", russianLocale)
        return sdf.format(date)
    }

    fun isToday(epochDay: Long): Boolean = epochDay == getTodayEpochDay()
    fun isTomorrow(epochDay: Long): Boolean = epochDay == getTodayEpochDay() + 1
    fun isYesterday(epochDay: Long): Boolean = epochDay == getTodayEpochDay() - 1

    fun getRelativeDayLabel(epochDay: Long): String = when {
        isToday(epochDay) -> "Сегодня"
        isTomorrow(epochDay) -> "Завтра"
        isYesterday(epochDay) -> "Вчера"
        else -> formatDayMonth(epochDay)
    }
}
