package com.nipu.wishfest

import com.nipu.wishfest.domain.models.CanvasModel
import com.nipu.wishfest.domain.models.LayerModel
import com.nipu.wishfest.domain.models.ProjectModel
import com.nipu.wishfest.domain.models.TransformModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class CardDocumentUnitTest {

    @Test
    fun testAddAndRemoveLayerInProjectModel() {
        val initialProject = ProjectModel(
            id = UUID.randomUUID().toString(),
            templateId = "test-template",
            title = "Test Card",
            canvas = CanvasModel(1080, 1080, "#FFFFFF")
        )

        val layer1 = LayerModel.Text(
            id = "layer-1",
            transform = TransformModel(x = 50f, y = 50f, width = 400f, height = 100f),
            text = "Hello WishFest"
        )

        val projectWithLayer = initialProject.copy(
            layers = initialProject.layers + layer1
        )

        assertEquals(1, projectWithLayer.layers.size)
        assertEquals("layer-1", projectWithLayer.layers[0].id)
        assertEquals("Hello WishFest", (projectWithLayer.layers[0] as LayerModel.Text).text)

        val projectAfterRemoval = projectWithLayer.copy(
            layers = projectWithLayer.layers.filter { it.id != "layer-1" }
        )

        assertTrue(projectAfterRemoval.layers.isEmpty())
    }

    @Test
    fun testLayerTransformModification() {
        val layer = LayerModel.Text(
            id = "layer-1",
            transform = TransformModel(x = 0f, y = 0f, width = 100f, height = 100f, rotation = 0f),
            text = "Transform Test"
        )

        val newTransform = TransformModel(x = 150f, y = 200f, width = 300f, height = 150f, rotation = 45f)
        val updatedLayer = layer.copy(transform = newTransform)

        assertEquals(150f, updatedLayer.transform.x)
        assertEquals(200f, updatedLayer.transform.y)
        assertEquals(300f, updatedLayer.transform.width)
        assertEquals(150f, updatedLayer.transform.height)
        assertEquals(45f, updatedLayer.transform.rotation)
    }
}
