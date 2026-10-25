package com.nipu.wishfest

import com.nipu.wishfest.domain.models.TransformModel
import com.nipu.wishfest.domain.models.resizeImage
import com.nipu.wishfest.domain.models.resizeImageFromCorner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageResizingTest {
    @Test
    fun resizePreservesProportionsAndCenter() {
        val result = resizeImage(TransformModel(200f, 200f, 400f, 600f), 200f, 1080, 1440)
        assertEquals(200f, result.width, .01f)
        assertEquals(300f, result.height, .01f)
        assertEquals(400f, result.x + result.width / 2, .01f)
        assertEquals(500f, result.y + result.height / 2, .01f)
    }

    @Test
    fun tallImageCannotGrowBeyondCanvasHeight() {
        val result = resizeImage(TransformModel(300f, 0f, 200f, 600f), 2000f, 1080, 1080)
        assertEquals(360f, result.width, .01f)
        assertEquals(1080f, result.height, .01f)
        assertEquals(0f, result.y, .01f)
    }

    @Test
    fun imageStaysVisibleAtEdgesAndCannotInvert() {
        val original = TransformModel(800f, 800f, 200f, 200f)
        val enlarged = resizeImage(original, 600f, 1080, 1080)
        assertTrue(enlarged.x >= 0 && enlarged.x + enlarged.width <= 1080)
        assertTrue(enlarged.y >= 0 && enlarged.y + enlarged.height <= 1080)
        val shrunk = resizeImage(original, -100f, 1080, 1080)
        assertEquals(86.4f, shrunk.width, .01f)
        assertEquals(shrunk.width, shrunk.height, .01f)
    }

    @Test
    fun cornerDragGrowsAndShrinksAnImage() {
        val original = TransformModel(300f, 300f, 400f, 400f)
        assertEquals(500f, resizeImageFromCorner(original, 50f, 50f, 1080, 1080).width, .01f)
        assertEquals(300f, resizeImageFromCorner(original, -50f, -50f, 1080, 1080).width, .01f)
    }

    @Test
    fun cornerDragAccountsForImageRotation() {
        val original = TransformModel(300f, 300f, 400f, 400f, 90f)
        val resized = resizeImageFromCorner(original, -50f, 50f, 1080, 1080)
        assertEquals(500f, resized.width, .01f)
        assertEquals(90f, resized.rotation, .01f)
    }
}
