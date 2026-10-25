package com.nipu.wishfest

import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.platform.app.InstrumentationRegistry
import com.nipu.wishfest.domain.models.CanvasModel
import com.nipu.wishfest.domain.models.DevotionalArtworks
import com.nipu.wishfest.domain.models.LayerModel
import com.nipu.wishfest.domain.models.ProjectModel
import com.nipu.wishfest.domain.models.TransformModel
import com.nipu.wishfest.platform.ProjectRepository
import com.nipu.wishfest.rendering.SceneRenderer
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class DevotionalArtworkTest {
    @Test
    fun allArtworkIsTransparentAndSurvivesSavingAndExport() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = ProjectRepository(context)
        val renderer = SceneRenderer(context)
        for (artwork in DevotionalArtworks.items) {
            val source = context.assets.open(artwork.assetPath).use { BitmapFactory.decodeStream(it) }
            assertNotNull(source)
            requireNotNull(source)
            assertTrue("${artwork.name} requires transparency", source.hasAlpha())
            assertEquals(0, Color.alpha(source.getPixel(0, 0)))
            assertEquals(0, Color.alpha(source.getPixel(source.width - 1, source.height - 1)))
            source.recycle()
            val project = ProjectModel(UUID.randomUUID().toString(), "artwork-test", canvas = CanvasModel(
                backgroundColor = "#FFFFFF"), layers = listOf(LayerModel.Image(
                    "deity", TransformModel(200f, 200f, 600f, 600f, 15f), artwork.assetPath)))
            try {
                repository.saveProject(project)
                val restored = requireNotNull(repository.loadProject(project.id))
                assertEquals(project.layers, restored.layers)
                for (width in listOf(360, 1080)) {
                    val image = renderer.renderToBitmap(restored, width, width)
                    assertEquals("Transparent corners preserve the card", Color.WHITE, image.getPixel(0, 0))
                    assertNotEquals("Artwork should be present in export", Color.WHITE, image.getPixel(width / 2, width / 2))
                    image.recycle()
                }
            } finally {
                repository.deleteProject(project.id)
            }
        }
    }
}
