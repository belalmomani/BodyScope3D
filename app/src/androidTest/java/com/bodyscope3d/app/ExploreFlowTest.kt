package com.bodyscope3d.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

/** تفترض لغة الجهاز الإنجليزية. اختبارات واجهة تعمل على جهاز/محاكي فقط (غير منفَّذة في CI الحالي). */
class ExploreFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun waitHome() = rule.waitUntil(8_000) { rule.onAllNodesWithText("Explore the human body").fetchSemanticsNodes().isNotEmpty() }

    @Test fun homeShowsAfterSplash() { waitHome(); rule.onNodeWithText("Explore anatomy").assertExists() }

    @Test fun selectRegionFromListThenReturnToFullBody() {
        waitHome()
        rule.onNodeWithText("Head").performScrollTo().performClick()
        rule.onNodeWithText("Confirm region").assertExists()
        rule.onNodeWithText("Back to full body").performScrollTo().performClick()
        rule.onNodeWithText("Confirm region").assertDoesNotExist()
    }

    @Test fun anatomyScreenOpensWithoutCrash() {
        waitHome(); rule.onNodeWithText("Explore anatomy").performClick()
        rule.onNodeWithText("Skeleton").assertExists()
    }
}
