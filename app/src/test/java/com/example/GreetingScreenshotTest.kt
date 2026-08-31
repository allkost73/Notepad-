package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.DailyTask
import com.example.data.model.Priority
import com.example.data.model.TaskCategory
import com.example.ui.components.DailyProgressCard
import com.example.ui.components.TaskCard
import com.example.ui.theme.MyApplicationTheme
import com.example.util.DateUtils
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun progress_card_screenshot() {
        composeTestRule.setContent {
            MyApplicationTheme {
                DailyProgressCard(
                    selectedEpochDay = DateUtils.getTodayEpochDay(),
                    totalTasks = 4,
                    completedTasks = 3
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
    }
}
