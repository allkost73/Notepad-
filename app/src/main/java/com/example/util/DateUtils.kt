package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateUtils {
    private val russianLocale = Locale("ru", "RU")
    val utcTimeZone: TimeZone = TimeZone.getTimeZone("UTC")

    /**
     * Returns the epoch day (number of days since 1970-01-01) for TODAY in the user's local timezone.
     */
    fun getTodayEpochDay(): Long {
        return millisToEpochDay(System.currentTimeMillis())
    }

    /**
     * Converts an epoch day into UTC milliseconds for 00:00:00 UTC.
     */
    fun epochDayToMillis(epochDay: Long): Long {
        return epochDay * (24 * 60 * 60 * 1000L)
    }

    /**
     * Converts a millisecond timestamp (such as System.currentTimeMillis()) into the corresponding
     * civil calendar epoch day in the device's local timezone.
     */
    fun millisToEpochDay(millis: Long): Long {
        val localCal = Calendar.getInstance().apply {
            timeInMillis = millis
        }
        val year = localCal.get(Calendar.YEAR)
        val month = localCal.get(Calendar.MONTH)
        val day = localCal.get(Calendar.DAY_OF_MONTH)

        val utcCal = Calendar.getInstance(utcTimeZone).apply {
            clear()
            set(year, month, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return utcCal.timeInMillis / (24 * 60 * 60 * 1000L)
    }

    /**
     * Creates an epoch day from civil year, month (0-based Calendar.MONTH), and day of month.
     */
    fun createEpochDay(year: Int, month: Int, dayOfMonth: Int): Long {
        val utcCal = Calendar.getInstance(utcTimeZone).apply {
            clear()
            set(year, month, dayOfMonth, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return utcCal.timeInMillis / (24 * 60 * 60 * 1000L)
    }

    fun getCurrentYear(): Int {
        return Calendar.getInstance().get(Calendar.YEAR)
    }

    fun formatDayMonth(epochDay: Long): String {
        val date = Date(epochDayToMillis(epochDay))
        val sdf = SimpleDateFormat("d MMMM", russianLocale).apply {
            timeZone = utcTimeZone
        }
        return sdf.format(date)
    }

    fun formatFullDateWithWeekday(epochDay: Long): String {
        val date = Date(epochDayToMillis(epochDay))
        val sdf = SimpleDateFormat("EEEE, d MMMM", russianLocale).apply {
            timeZone = utcTimeZone
        }
        val result = sdf.format(date)
        return result.replaceFirstChar { if (it.isLowerCase()) it.titlecase(russianLocale) else it.toString() }
    }

    fun formatWeekdayShort(epochDay: Long): String {
        val date = Date(epochDayToMillis(epochDay))
        val sdf = SimpleDateFormat("EE", russianLocale).apply {
            timeZone = utcTimeZone
        }
        val result = sdf.format(date)
        return result.replaceFirstChar { if (it.isLowerCase()) it.titlecase(russianLocale) else it.toString() }
    }

    fun formatDayNumber(epochDay: Long): String {
        val date = Date(epochDayToMillis(epochDay))
        val sdf = SimpleDateFormat("d", russianLocale).apply {
            timeZone = utcTimeZone
        }
        return sdf.format(date)
    }

    fun formatMonthYear(epochDay: Long): String {
        val date = Date(epochDayToMillis(epochDay))
        val sdf = SimpleDateFormat("LLLL yyyy", russianLocale).apply {
            timeZone = utcTimeZone
        }
        val result = sdf.format(date)
        return result.replaceFirstChar { if (it.isLowerCase()) it.titlecase(russianLocale) else it.toString() }
    }

    fun formatNoteTimestamp(millis: Long): String {
        val date = Date(millis)
        val sdf = SimpleDateFormat("d MMM, HH:mm", russianLocale).apply {
            timeZone = TimeZone.getDefault()
        }
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

    fun getNextWeekdayEpochDay(targetCalDay: Int): Long {
        val cal = Calendar.getInstance()
        val currentCalDay = cal.get(Calendar.DAY_OF_WEEK)
        var daysUntil = (targetCalDay - currentCalDay + 7) % 7
        if (daysUntil == 0) daysUntil = 7
        return getTodayEpochDay() + daysUntil
    }
}
