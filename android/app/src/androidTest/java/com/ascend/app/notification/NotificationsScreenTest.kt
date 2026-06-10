package com.ascend.app.notification

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for the stateless NotificationsScreenContent composable.
 * No ViewModel/Hilt needed — state goes in, callbacks come out.
 *
 * Run with: ./gradlew connectedDebugAndroidTest (device/emulator required)
 */
@RunWith(AndroidJUnit4::class)
class NotificationsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    // Deterministic day-bucket timestamps regardless of wall-clock time:
    // "today" = 1 min ago, "yesterday" = 1 h before start of today.
    private val startOfToday = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun sampleItems() = listOf(
        NotifItem(
            id = "n1", type = NotifType.LEVEL_UP,
            title = "Level 24 reached!", body = "You've ascended to the next tier.",
            timestamp = System.currentTimeMillis() - 60_000, isRead = false, xpDelta = 500
        ),
        NotifItem(
            id = "n2", type = NotifType.QUEST_COMPLETE,
            title = "Quest Cleared", body = "Refactor auth service — well done.",
            timestamp = System.currentTimeMillis() - 120_000, isRead = false, xpDelta = 150
        ),
        NotifItem(
            id = "n3", type = NotifType.GOAL_MILESTONE,
            title = "Goal at 50%", body = "Climb Mt. Fuji — halfway there!",
            timestamp = startOfToday - 3_600_000, isRead = true
        )
    )

    @Composable
    private fun Content(
        items: List<NotifItem>,
        selectedFilter: NotifType? = null,
        onBack: () -> Unit = {},
        onFilterChange: (NotifType?) -> Unit = {},
        onMarkAllRead: () -> Unit = {},
        onClearAll: () -> Unit = {},
        onItemClick: (NotifItem) -> Unit = {}
    ) {
        MaterialTheme {
            NotificationsScreenContent(
                items = items,
                unreadCount = items.count { !it.isRead },
                selectedFilter = selectedFilter,
                onBack = onBack,
                onFilterChange = onFilterChange,
                onMarkAllRead = onMarkAllRead,
                onClearAll = onClearAll,
                onItemClick = onItemClick,
                onSwipeDelete = {}
            )
        }
    }

    // ── Rendering ───────────────────────────────────────────────

    @Test
    fun emptyState_showsPlaceholder() {
        composeRule.setContent { Content(items = emptyList()) }

        composeRule.onNodeWithText("NO TRANSMISSIONS").assertIsDisplayed()
        composeRule.onNodeWithText("The System is quiet for now.").assertIsDisplayed()
    }

    @Test
    fun populatedList_rendersTitlesAndBodies() {
        composeRule.setContent { Content(items = sampleItems()) }

        composeRule.onNodeWithText("Level 24 reached!").assertIsDisplayed()
        composeRule.onNodeWithText("You've ascended to the next tier.").assertIsDisplayed()
        composeRule.onNodeWithText("Quest Cleared").assertIsDisplayed()
        composeRule.onNodeWithText("NO TRANSMISSIONS").assertDoesNotExist()
    }

    @Test
    fun xpChip_shownOnlyWhenXpDeltaPresent() {
        composeRule.setContent { Content(items = sampleItems()) }

        composeRule.onNodeWithText("+500 XP").assertExists()
        composeRule.onNodeWithText("+150 XP").assertExists()
        // n3 has no xpDelta — exactly two chips on screen.
        composeRule.onAllNodesWithText(" XP", substring = true).assertCountEquals(2)
    }

    @Test
    fun unreadBadge_showsCount() {
        composeRule.setContent { Content(items = sampleItems()) } // 2 unread

        composeRule.onNodeWithText("2").assertIsDisplayed()
    }

    @Test
    fun unreadBadge_absentWhenAllRead() {
        val allRead = sampleItems().map { it.copy(isRead = true) }
        composeRule.setContent { Content(items = allRead) }

        composeRule.onNodeWithText("0").assertDoesNotExist()
    }

    @Test
    fun dayHeaders_groupByRecency() {
        composeRule.setContent { Content(items = sampleItems()) }

        composeRule.onNodeWithText("TODAY").assertExists()      // n1, n2
        composeRule.onNodeWithText("YESTERDAY").assertExists()  // n3
    }

    // ── Filtering ───────────────────────────────────────────────

    @Test
    fun activeFilter_showsOnlyMatchingType() {
        composeRule.setContent {
            Content(items = sampleItems(), selectedFilter = NotifType.LEVEL_UP)
        }

        composeRule.onNodeWithText("Level 24 reached!").assertIsDisplayed()
        composeRule.onNodeWithText("Quest Cleared").assertDoesNotExist()
        composeRule.onNodeWithText("Goal at 50%").assertDoesNotExist()
    }

    @Test
    fun activeFilter_withNoMatches_showsFilteredEmptyState() {
        composeRule.setContent {
            Content(items = sampleItems(), selectedFilter = NotifType.FRIEND_ACTIVITY)
        }

        composeRule.onNodeWithText("NO SOCIAL TRANSMISSIONS").assertIsDisplayed()
    }

    @Test
    fun filterPill_clickEmitsType() {
        var selected: NotifType? = NotifType.SYSTEM // sentinel
        composeRule.setContent {
            Content(items = sampleItems(), onFilterChange = { selected = it })
        }

        // "LEVEL UP" appears both as a filter pill and as the n1 card's type
        // label; tree traversal order puts the pill row first.
        composeRule.onAllNodesWithText("LEVEL UP").onFirst().performClick()
        assertEquals(NotifType.LEVEL_UP, selected)

        composeRule.onNodeWithText("ALL").performClick()
        assertEquals(null, selected)
    }

    // ── Callbacks ───────────────────────────────────────────────

    @Test
    fun itemClick_emitsClickedItem() {
        var clicked: NotifItem? = null
        composeRule.setContent {
            Content(items = sampleItems(), onItemClick = { clicked = it })
        }

        composeRule.onNodeWithText("Quest Cleared").performClick()
        assertEquals("n2", clicked?.id)
    }

    @Test
    fun topBarButtons_invokeCallbacks() {
        var backs = 0
        var markAlls = 0
        var clears = 0
        composeRule.setContent {
            Content(
                items = sampleItems(),
                onBack = { backs++ },
                onMarkAllRead = { markAlls++ },
                onClearAll = { clears++ }
            )
        }

        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithContentDescription("Mark all read").performClick()
        composeRule.onNodeWithContentDescription("Clear all").performClick()

        assertEquals(1, backs)
        assertEquals(1, markAlls)
        assertEquals(1, clears)
    }

    @Test
    fun readItem_stillRenders() {
        composeRule.setContent { Content(items = sampleItems()) }

        // n3 is read — must still be in the list, just unhighlighted.
        composeRule.onNodeWithText("Goal at 50%").assertExists()
        assertTrue(sampleItems()[2].isRead)
    }
}
