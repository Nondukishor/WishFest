package com.nipu.wishfest

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import org.junit.Assert.assertNotEquals
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test

class CreationNavigationTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun createFromTemplatesOffersTheSameOptionsAndOpensAi() {
        compose.onNodeWithText("Create").performClick()
        compose.onNodeWithText("Browse templates").performClick()
        compose.onNodeWithText("Find your next design").assertIsDisplayed()
        compose.onNodeWithText("Create").performClick()
        compose.onNodeWithText("Start from scratch").assertIsDisplayed()
        compose.onNodeWithText("Browse templates").assertIsDisplayed()
        compose.onNodeWithText("Create with AI").performClick()
        compose.onNodeWithText("Create a custom card").assertIsDisplayed()
        compose.onNodeWithText("Occasion").assertIsDisplayed()
    }

    @Test
    fun blankCardOpensEditorWithoutRequiringAiInput() {
        compose.onNodeWithText("Create").performClick()
        compose.onNodeWithText("Start from scratch").performClick()
        compose.onNodeWithText("Edit card").assertIsDisplayed()
        compose.onNodeWithText("Export").assertIsDisplayed()
        compose.onNodeWithText("Write your wish").assertIsDisplayed()
    }
    @Test
    fun devotionalImageCanBeAddedAndEdited() {
        compose.onNodeWithText("Create").performClick()
        compose.onNodeWithText("Start from scratch").performClick()
        compose.onNodeWithText("Elements").performClick()
        compose.onNodeWithText("Devotional images").assertIsDisplayed()
        compose.onNodeWithText("Lakshmi").performClick()
        compose.onNodeWithText("Selected image · Lakshmi").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Image size", substring = true).assertIsDisplayed()
        val before = compose.onNodeWithText("Image size", substring = true)
            .fetchSemanticsNode().config[SemanticsProperties.Text].first().text
        compose.onNodeWithContentDescription("Resize selected image").performTouchInput {
            swipe(center, center + Offset(60f, 60f), durationMillis = 350)
        }
        val after = compose.onNodeWithText("Image size", substring = true)
            .fetchSemanticsNode().config[SemanticsProperties.Text].first().text
        assertNotEquals("Dragging the handle must resize the image", before, after)
    }

}
