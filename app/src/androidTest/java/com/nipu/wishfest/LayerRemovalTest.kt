package com.nipu.wishfest

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import com.nipu.wishfest.domain.models.LayerModel
import com.nipu.wishfest.platform.ProjectRepository
import com.nipu.wishfest.ui.editor.EditorViewModel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class LayerRemovalTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun openEditor(): EditorViewModel {
        compose.onNodeWithText("Create").performClick()
        compose.onNodeWithText("Start from scratch").performClick()
        return compose.runOnIdle { ViewModelProvider(compose.activity)[EditorViewModel::class.java] }
    }

    @Test
    fun textStickerAndImageHaveVisibleDeleteAndCanBeRestored() {
        val model = openEditor()
        val additions: List<() -> Unit> = listOf(
            { model.addTextLayer("Remove this text") },
            { model.addSticker("🌸") },
            { model.addImageLayer("devotional/lakshmi.png") }
        )
        for (add in additions) {
            val selected = compose.runOnIdle {
                add()
                model.uiState.value.project!!.layers.first { it.id == model.uiState.value.selectedLayerId }
            }
            compose.onNodeWithText("Delete").assertIsDisplayed().performClick()
            compose.runOnIdle {
                assertTrue(model.uiState.value.project!!.layers.none { it.id == selected.id })
                assertNull(model.uiState.value.selectedLayerId)
            }
            compose.onNodeWithText("Delete").assertIsNotEnabled()
            compose.onNodeWithText("Undo").performClick()
            compose.runOnIdle {
                val project = model.uiState.value.project!!
                assertEquals(selected, project.layers.first { it.id == selected.id })
                assertEquals(project.layers, ProjectRepository(compose.activity).loadProject(project.id)!!.layers)
            }
        }
    }

    @Test
    fun deleteFromPreviewWorksWhileAnotherTabIsOpen() {
        val model = openEditor()
        val selectedId = compose.runOnIdle {
            model.addSticker("✨")
            model.uiState.value.selectedLayerId!!
        }
        compose.onNodeWithText("Background").performClick()
        compose.onNodeWithText("Delete").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(model.uiState.value.project!!.layers.none { it.id == selectedId }) }
        compose.onNodeWithText("Undo").assertIsDisplayed()
    }
}
