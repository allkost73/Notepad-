package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Priority
import com.example.data.model.TaskCategory
import com.example.voice.SmartVoiceParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Ежедневник и Заметки", appName)
    }

    @Test
    fun `smart voice parser detects date, time, priority and category`() {
        val voiceInput = "Завтра в 15:30 срочно купить продукты в магазине"
        val parsed = SmartVoiceParser.parse(voiceInput)

        assertTrue(parsed.isSuggestedAsTask)
        assertEquals("15:30", parsed.suggestedTimeString)
        assertEquals(Priority.HIGH, parsed.suggestedPriority)
        assertEquals(TaskCategory.SHOPPING, parsed.suggestedCategory)
        assertTrue(parsed.cleanedTitle.isNotBlank())
    }

    @Test
    fun `smart voice parser handles simple notes`() {
        val noteVoice = "Идея для новой книги про приключения в космосе и путешествия во времени"
        val parsed = SmartVoiceParser.parse(noteVoice)

        assertNotNull(parsed.cleanedTitle)
        assertEquals(TaskCategory.IDEAS, parsed.suggestedCategory)
    }
}
