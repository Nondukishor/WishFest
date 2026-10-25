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

    @Test
    fun lakshmiSpellingsProduceRelevantHeadingAndOfflineWish() = runBlocking {
        val generator = AiWishGenerator { null }
        for (occasion in listOf("Laksmi Puja", "Lakshmi Puja", "Laxmi Puja", "লক্ষ্মী পূজা")) {
            val card = generator.generateCreativeCard(occasion, "Mom")
            val texts = card.layers.filterIsInstance<com.nipu.wishfest.domain.models.LayerModel.Text>()
            assertEquals("HAPPY LAKSHMI PUJA", texts.first().text)
            assertTrue(texts.last().text.contains("Goddess Lakshmi"))
            assertTrue(texts.last().text.contains("Mom"))
            assertTrue(texts.none { it.text.contains("Bijoya", ignoreCase = true) })
        }
    }

    @Test
    fun otherPujasKeepTheirOwnOccasion() = runBlocking {
        val generator = AiWishGenerator { null }
        for (occasion in listOf("Saraswati Puja", "Kali Puja", "Satyanarayan Puja", "Birthday for a 5 year old")) {
            val card = generator.generateCreativeCard(occasion, language = "Hindi")
            val texts = card.layers.filterIsInstance<com.nipu.wishfest.domain.models.LayerModel.Text>()
            assertEquals(occasion.uppercase(), texts.first().text)
            assertTrue(texts.last().text.contains(occasion))
        }
    }

    @Test
    fun bengaliLakshmiCardUsesMatchingLanguage() = runBlocking {
        val card = AiWishGenerator { null }.generateCreativeCard("Laksmi Puja", language = "Bengali (বাংলা)")
        val texts = card.layers.filterIsInstance<com.nipu.wishfest.domain.models.LayerModel.Text>()
        assertEquals("শুভ লক্ষ্মী পূজা", texts.first().text)
        assertTrue(texts.last().text.contains("মা লক্ষ্মী"))
    }

    @Test
    fun modelPromptRequiresRequestedOccasion() = runBlocking {
        var capturedPrompt = ""
        val generator = AiWishGenerator { capturedPrompt = it; "Happy Lakshmi Puja!" }
        generator.generateCreativeCard("Laksmi Puja")
        assertTrue(capturedPrompt.contains("Laksmi Puja"))
        assertTrue(capturedPrompt.contains("never substitute a different festival"))
    }

    @Test(expected = CancellationException::class)
    fun cancellationDoesNotCreateAnUnwantedCard(): Unit = runBlocking {
        val generator = AiWishGenerator { throw CancellationException("Screen closed") }
        generator.generateWish("Birthday")
        Unit
    }
}
