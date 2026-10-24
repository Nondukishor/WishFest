package com.nipu.wishfest

import com.nipu.wishfest.ai.AiWishGenerator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiWishGeneratorTest {
    @Test
    fun missingAiStillCreatesPersonalizedWish() = runBlocking {
        val generator = AiWishGenerator { null }
        val wish = generator.generateWish("Diwali", "Mom")
        assertTrue(wish.contains("Diwali"))
        assertTrue(wish.contains("Mom"))
    }

    @Test
    fun failingAiStillCreatesCardWithEditableLayers() = runBlocking {
        val generator = AiWishGenerator { throw IllegalStateException("AI service missing") }
        val card = generator.generateCreativeCard("Eid", "Sara")
        assertEquals(3, card.layers.size)
        val body = card.layers.last() as com.nipu.wishfest.domain.models.LayerModel.Text
        assertTrue(body.text.contains("Eid Mubarak"))
        assertTrue(body.text.contains("Sara"))
    }

    @Test
    fun offlineWishesRespectLanguage() = runBlocking {
        val generator = AiWishGenerator { null }
        val wish = generator.generateWish("Eid", "Nipu", language = "Bengali (বাংলা)")
        assertTrue(wish.contains("ঈদ"))
        assertTrue(wish.contains("Nipu"))
    }

    @Test
    fun emptyAiResponseUsesOfflineWish() = runBlocking {
        val generator = AiWishGenerator { "  " }
        assertTrue(generator.generateWish("Diwali", "Mom").contains("Mom"))
    }

    @Test
    fun successfulAiResponseUsesGeneratedText() = runBlocking {
        val generator = AiWishGenerator { "  A lovely wish for you!  " }
        assertEquals("A lovely wish for you!", generator.generateWish("Birthday"))
    }

    @Test(expected = CancellationException::class)
    fun cancellationDoesNotCreateAnUnwantedCard(): Unit = runBlocking {
        val generator = AiWishGenerator { throw CancellationException("Screen closed") }
        generator.generateWish("Birthday")
        Unit
    }
}
