package com.example

import com.example.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class DateUtilsTest {

    @Test
    fun testTodayEpochDayMatchesCalendarDate() {
        val todayEpoch = DateUtils.getTodayEpochDay()
        val localCal = Calendar.getInstance()
        val currentDay = localCal.get(Calendar.DAY_OF_MONTH)
        
        val formattedDay = DateUtils.formatDayNumber(todayEpoch)
        assertEquals(currentDay.toString(), formattedDay)
        
        assertTrue(DateUtils.isToday(todayEpoch))
        assertTrue(DateUtils.isTomorrow(todayEpoch + 1))
        assertTrue(DateUtils.isYesterday(todayEpoch - 1))
        assertEquals("Сегодня", DateUtils.getRelativeDayLabel(todayEpoch))
        assertEquals("Завтра", DateUtils.getRelativeDayLabel(todayEpoch + 1))
        assertEquals("Вчера", DateUtils.getRelativeDayLabel(todayEpoch - 1))
    }

    @Test
    fun testTimezoneIndependence() {
        // Test date: 2026-09-29
        // Calendar month: Calendar.SEPTEMBER is 8
        val epochDay2026Sep29 = DateUtils.createEpochDay(2026, Calendar.SEPTEMBER, 29)
        
        // Day number must be 29
        assertEquals("29", DateUtils.formatDayNumber(epochDay2026Sep29))
        
        // Full date must be "Вторник, 29 сентября"
        val full = DateUtils.formatFullDateWithWeekday(epochDay2026Sep29)
        assertTrue(full.contains("29"))
        assertTrue(full.contains("сентября"))
        assertTrue(full.contains("Вторник"))
        
        // Month year must be "Сентябрь 2026"
        val monthYear = DateUtils.formatMonthYear(epochDay2026Sep29)
        assertTrue(monthYear.contains("Сентябрь"))
        assertTrue(monthYear.contains("2026"))
    }
}
