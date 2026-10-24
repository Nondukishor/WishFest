package com.nipu.wishfest

import com.nipu.wishfest.domain.models.CanvasModel
import com.nipu.wishfest.domain.models.LayerModel
import com.nipu.wishfest.domain.models.ProjectModel
import com.nipu.wishfest.domain.models.TransformModel
import com.nipu.wishfest.platform.ProjectRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ProjectRepositoryTest {

    @Test
    fun decorationsRestoreWithStickerTransforms() {
        val project = ProjectRepository.deserializeJsonToProject("""
            {"id":"decorated", "canvas":{"borderStyle":"floral", "borderColor":"#DB7093"},
             "layers":[{"id":"sticker", "type":"text", "fontId":"sticker", "text":"💖",
               "fontSize":150, "transform":{"x":120,"y":200,"width":200,"height":240,"rotation":30}}]}
        """.trimIndent())
        assertEquals("floral", project.canvas.borderStyle)
        assertEquals("#DB7093", project.canvas.borderColor)
        val sticker = project.layers.single() as LayerModel.Text
        assertEquals("sticker", sticker.fontId)
        assertEquals("💖", sticker.text)
        assertEquals(30f, sticker.transform.rotation)
        assertEquals(150f, sticker.fontSize)
    }

    @Test
    fun olderCardsHaveNoBorderByDefault() {
        val project = ProjectRepository.deserializeJsonToProject("""{"id":"old","canvas":{}}""")
        assertEquals("none", project.canvas.borderStyle)
        assertEquals("#E5A93C", project.canvas.borderColor)
    }

    @Test
    fun testProjectDeserialization() {
        val jsonStr = """
            {
              "id": "proj-12345",
              "templateId": "durga-01",
              "title": "Subho Bijoya Card",
              "updatedAt": 1700000000000,
              "canvas": {
                "width": 1080,
                "height": 1080,
                "backgroundColor": "#8B0000",
                "backgroundPhotoPath": "",
                "backgroundPhotoRotation": 0.0
              },
              "layers": [
                {
                  "id": "layer-text-1",
                  "type": "text",
                  "text": "শুভ বিজয়া",
                  "fontId": "bengali",
                  "fontSize": 64.0,
                  "color": "#FFD700",
                  "alignment": "center",
                  "opacity": 1.0,
                  "visible": true,
                  "locked": false,
                  "transform": {
                    "x": 100.0,
                    "y": 300.0,
                    "width": 880.0,
                    "height": 200.0,
                    "rotation": 0.0
                  }
                }
              ]
            }
        """.trimIndent()

        val restored = ProjectRepository.deserializeJsonToProject(jsonStr)

        assertNotNull(restored)
        assertEquals("proj-12345", restored.id)
        assertEquals("durga-01", restored.templateId)
        assertEquals("Subho Bijoya Card", restored.title)
        assertEquals("#8B0000", restored.canvas.backgroundColor)
        assertEquals(1, restored.layers.size)

        val textLayer = restored.layers[0] as LayerModel.Text
        assertEquals("শুভ বিজয়া", textLayer.text)
        assertEquals("#FFD700", textLayer.color)
        assertEquals(64f, textLayer.fontSize)
    }

    @Test
    fun testSerializationRoundTripConsistency() {
        val original = ProjectModel(
            id = "proj-999",
            templateId = "eid-01",
            title = "Eid Mubarak Card",
            canvas = CanvasModel(backgroundColor = "#1A4331", backgroundPhotoRotation = 90f),
            layers = listOf(
                LayerModel.Text(
                    id = "l1",
                    transform = TransformModel(x = 100f, y = 200f, width = 800f, height = 150f, rotation = 15f),
                    text = "Eid Mubarak",
                    fontId = "arabic",
                    color = "#E5A93C"
                )
            )
        )

        // Dummy repository call
        val jsonStr = """
            {
              "schemaVersion": 1,
              "id": "${original.id}",
              "templateId": "${original.templateId}",
              "title": "${original.title}",
              "updatedAt": ${original.updatedAt},
              "canvas": {
                "width": ${original.canvas.width},
                "height": ${original.canvas.height},
                "backgroundColor": "${original.canvas.backgroundColor}",
                "backgroundPhotoPath": "${original.canvas.backgroundPhotoPath}",
                "backgroundPhotoRotation": ${original.canvas.backgroundPhotoRotation}
              },
              "layers": [
                {
                  "id": "l1",
                  "type": "text",
                  "text": "Eid Mubarak",
                  "fontId": "arabic",
                  "fontSize": 48.0,
                  "color": "#E5A93C",
                  "alignment": "center",
                  "opacity": 1.0,
                  "visible": true,
                  "locked": false,
                  "transform": {
                    "x": 100.0,
                    "y": 200.0,
                    "width": 800.0,
                    "height": 150.0,
                    "rotation": 15.0
                  }
                }
              ]
            }
        """.trimIndent()

        val restored = ProjectRepository.deserializeJsonToProject(jsonStr)

        assertEquals(original.id, restored.id)
        assertEquals(original.title, restored.title)
        assertEquals(original.canvas.backgroundColor, restored.canvas.backgroundColor)
        assertEquals(90f, restored.canvas.backgroundPhotoRotation)
        assertEquals(1, restored.layers.size)

        val layer = restored.layers[0] as LayerModel.Text
        assertEquals("Eid Mubarak", layer.text)
        assertEquals("arabic", layer.fontId)
        assertEquals("#E5A93C", layer.color)
        assertEquals(15f, layer.transform.rotation)
    }
}
