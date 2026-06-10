package com.ascend.app.ui.dashboard

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ascend.app.domain.model.Habit
import com.ascend.app.domain.model.Quest
import com.ascend.app.domain.model.QuestStatus
import com.ascend.app.domain.model.QuestType
import com.ascend.app.domain.model.User
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for the stateless DashboardScreenContent composable.
 *
 * The dashboard uses infinite glow animations (GlowBar pulse), so the test
 * clock is driven manually — autoAdvance would make waitForIdle hang forever.
 *
 * Run with: ./gradlew connectedDebugAndroidTest (device/emulator required)
 */
@RunWith(AndroidJUnit4::class)
class DashboardScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val user = User(
        id = "u1", email = "hunter@ascend.app", username = "ShadowMonarch",
        level = 12, currentXp = 350, xpToNext = 800, avatarUrl = null
    )

    private val quest = Quest(
        id = "q1", title = "Morning 5k Run",
        description = "Run five kilometers before 9am.",
        type = QuestType.DAILY, difficulty = 3, xpReward = 120,
        status = QuestStatus.ACTIVE, skillArea = "Fitness", isAiGenerated = true
    )

    private val habit = Habit(
        id = "h1", title = "Read 20 pages", frequency = "daily",
        xpReward = 40, currentStreak = 12, longestStreak = 30,
        completedToday = false
    )

    @Composable
    private fun Content(
        quests: List<Quest>,
        habits: List<Habit> = emptyList(),
        onIntent: (DashboardIntent) -> Unit = {}
    ) {
        MaterialTheme {
            DashboardScreenContent(
                user = user,
                activeQuests = quests,
                todayHabits = habits,
                isGenerating = false,
                snackbarHostState = remember { SnackbarHostState() },
                onIntent = onIntent,
                onNavigate = {}
            )
        }
    }

    private fun setContentPaused(content: @Composable () -> Unit) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent(content)
        composeRule.mainClock.advanceTimeBy(200)
    }

    @Test
    fun questCard_rendersTitleDescriptionAndActions() {
        setContentPaused { Content(quests = listOf(quest)) }

        composeRule.onNodeWithText("Morning 5k Run").assertExists()
        composeRule.onNodeWithText("Run five kilometers before 9am.").assertExists()
        composeRule.onNodeWithText("SKIP").assertExists()
        composeRule.onNodeWithText("COMPLETE QUEST").assertExists()
    }

    @Test
    fun completeQuestButton_emitsCompleteIntent() {
        var intent: DashboardIntent? = null
        setContentPaused { Content(quests = listOf(quest), onIntent = { intent = it }) }

        composeRule.onNodeWithText("COMPLETE QUEST").performClick()
        composeRule.mainClock.advanceTimeByFrame()

        assertEquals(DashboardIntent.CompleteQuest("q1"), intent)
    }

    @Test
    fun skipButton_emitsSkipIntent() {
        var intent: DashboardIntent? = null
        setContentPaused { Content(quests = listOf(quest), onIntent = { intent = it }) }

        composeRule.onNodeWithText("SKIP").performClick()
        composeRule.mainClock.advanceTimeByFrame()

        assertEquals(DashboardIntent.SkipQuest("q1"), intent)
    }

    @Test
    fun noActiveQuests_showsEmptyPanel() {
        setContentPaused { Content(quests = emptyList()) }

        composeRule.onNodeWithText("All quests cleared. Generate more.").assertExists()
    }

    @Test
    fun habitCard_rendersTitleStreakAndReward() {
        setContentPaused { Content(quests = emptyList(), habits = listOf(habit)) }

        composeRule.onNodeWithText("Read 20 pages").assertExists()
        composeRule.onNodeWithText("🔥 12d").assertExists()
        composeRule.onNodeWithText("+40 XP").assertExists()
    }

    @Test
    fun header_showsUsername() {
        setContentPaused { Content(quests = emptyList()) }

        // HunterHeaderPanel renders the name uppercased.
        composeRule.onNodeWithText("SHADOWMONARCH").assertExists()
    }
}
