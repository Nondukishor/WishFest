package com.nipu.wishfest

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.platform.app.InstrumentationRegistry
import com.nipu.wishfest.domain.models.CanvasModel
import com.nipu.wishfest.domain.models.ProjectModel
import com.nipu.wishfest.rendering.HinduFestivalFrames
import com.nipu.wishfest.rendering.SceneRenderer
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LakshmiFrameRenderTest {
    @Test
    fun framesRenderDistinctlyAtPreviewAndExportSizesWithClearMessageArea() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val renderer = SceneRenderer(context)
        val hashes = mutableSetOf<Int>()
        val sheet = Bitmap.createBitmap(1080, 430, Bitmap.Config.ARGB_8888)
        val sheetCanvas = Canvas(sheet)
        sheetCanvas.drawColor(Color.rgb(24, 24, 32))
        val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 17f
            textAlign = Paint.Align.CENTER
        }
        HinduFestivalFrames.lakshmiOptions.entries.forEachIndexed { index, (style, name) ->
            assertTrue(style in HinduFestivalFrames.styles)
            for (height in listOf(1080, 1440)) {
                val project = ProjectModel(style, "frame-test", canvas = CanvasModel(
                    height = height, backgroundColor = "#62142E", borderStyle = style))
                for (width in listOf(360, 1080)) {
                    val bitmap = renderer.renderToBitmap(project, width, width * height / 1080)
                    assertEquals(Color.rgb(98, 20, 46), bitmap.getPixel(width / 2, bitmap.height / 2))
                    if (height == 1080 && width == 360) {
                        val pixels = IntArray(width * bitmap.height)
                        bitmap.getPixels(pixels, 0, width, 0, 0, width, bitmap.height)
                        hashes.add(pixels.contentHashCode())
                        sheetCanvas.drawBitmap(bitmap, index * 360f, 0f, null)
                        sheetCanvas.drawText(name, index * 360f + 180f, 400f, label)
                    }
                    bitmap.recycle()
                }
            }
        }
        assertEquals(3, hashes.size)
        File(context.cacheDir, "lakshmi-frames.png").outputStream().use {
            sheet.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        sheet.recycle()
    }
}
