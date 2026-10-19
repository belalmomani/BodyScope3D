package com.bodyscope3d.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class ExploreFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun selectRegionIsolateAndReturn() {
        rule.onNodeWithText("Explore body").performClick()
        rule.onNodeWithText("Head").performClick()
        rule.onNodeWithText("Full body").assertExists().performClick()
        rule.onNodeWithText("Full body").assertDoesNotExist()
    }
    @Test fun appDoesNotCrashWithoutInternalModels() {
        rule.onNodeWithText("Internal anatomy").performClick()
        rule.onNodeWithText("No internal anatomy assets", substring = true).assertExists()
    }
}
