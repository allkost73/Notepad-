package com.example

import com.example.data.model.Priority
import com.example.data.model.TaskCategory
import com.example.ui.planner.MainTab
import com.example.ui.planner.TaskFilter
import com.example.voice.SmartVoiceParser
import com.example.voice.VoiceCommand
import com.example.voice.VoiceCommandParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceCommandTest {

    @Test
    fun testVoiceNavigationCommands() {
        val cmdNotebook = VoiceCommandParser.detectCommand("открой заметки")
        assertNotNull(cmdNotebook)
        assertTrue(cmdNotebook!!.command is VoiceCommand.SwitchTab)
        assertEquals(MainTab.NOTEBOOK, (cmdNotebook.command as VoiceCommand.SwitchTab).tab)

        val cmdPlanner = VoiceCommandParser.detectCommand("покажи ежедневник")
        assertNotNull(cmdPlanner)
        assertTrue(cmdPlanner!!.command is VoiceCommand.SwitchTab)
        assertEquals(MainTab.PLANNER, (cmdPlanner.command as VoiceCommand.SwitchTab).tab)
    }

    @Test
    fun testVoiceDateNavigationCommands() {
        val cmdTomorrow = VoiceCommandParser.detectCommand("задачи на завтра")
        assertNotNull(cmdTomorrow)
        assertTrue(cmdTomorrow!!.command is VoiceCommand.SelectDate)

        val cmdToday = VoiceCommandParser.detectCommand("покажи сегодня")
        assertNotNull(cmdToday)
        assertTrue(cmdToday!!.command is VoiceCommand.SelectDate)

        val cmdNextDay = VoiceCommandParser.detectCommand("следующий день")
        assertNotNull(cmdNextDay)
        assertTrue(cmdNextDay!!.command is VoiceCommand.NextDay)
    }

    @Test
    fun testVoiceFilterAndSearchCommands() {
        val cmdFilter = VoiceCommandParser.detectCommand("покажи выполненные")
        assertNotNull(cmdFilter)
        assertTrue(cmdFilter!!.command is VoiceCommand.SetFilter)
        assertEquals(TaskFilter.COMPLETED, (cmdFilter.command as VoiceCommand.SetFilter).filter)

        val cmdSearch = VoiceCommandParser.detectCommand("найди заметку проект")
        assertNotNull(cmdSearch)
        assertTrue(cmdSearch!!.command is VoiceCommand.SearchNotes)
        assertEquals("проект", (cmdSearch.command as VoiceCommand.SearchNotes).query)
    }

    @Test
    fun testSmartVoiceRussianNaturalLanguage() {
        val parsed = SmartVoiceParser.parse("напомни завтра в 14:30 купить продукты срочно")
        println("parsed.cleanedTitle = [${parsed.cleanedTitle}]")
        assertEquals("14:30", parsed.suggestedTimeString)
        assertEquals(TaskCategory.SHOPPING, parsed.suggestedCategory)
        assertEquals(Priority.HIGH, parsed.suggestedPriority)
        assertTrue("Expected to contain 'Купить продукты', was: '${parsed.cleanedTitle}'", 
            parsed.cleanedTitle.contains("Купить продукты", ignoreCase = true))

        val parsedHealth = SmartVoiceParser.parse("в пятницу в 18:00 тренировка")
        assertEquals("18:00", parsedHealth.suggestedTimeString)
        assertEquals(TaskCategory.HEALTH, parsedHealth.suggestedCategory)
        assertTrue(parsedHealth.cleanedTitle.contains("Тренировка", ignoreCase = true))
    }
}
