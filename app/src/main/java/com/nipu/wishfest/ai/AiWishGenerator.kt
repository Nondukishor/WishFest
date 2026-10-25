package com.nipu.wishfest.ai

import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.common.FeatureStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import com.nipu.wishfest.domain.models.CanvasModel
import com.nipu.wishfest.domain.models.LayerModel
import com.nipu.wishfest.domain.models.ProjectModel
import com.nipu.wishfest.domain.models.TransformModel
import java.util.UUID

private data class CardDesignConfig(
    val bgColor: String,
    val fontStyle: String,
    val headingText: String,
    val dividerText: String,
    val headingColor: String = "#FFD700",
    val textColor: String = "#FFFFFF"
)

/** A null result means on-device generation is unavailable; greetings still work offline. */
class AiWishGenerator(
    private val onDeviceGenerate: suspend (String) -> String? = ::generateOnDeviceWish
) {

    suspend fun generateWish(
        festivalName: String,
        recipient: String = "Friend",
        tone: String = "Warm",
        language: String = "English"
    ): String {
        val prompt = "Write a concise, $tone $festivalName greeting card wish for my $recipient in 1 or 2 lines in $language language. Include a blessing appropriate to this occasion. Keep the greeting strictly about the occasion in the user input; never substitute a different festival or celebration. Respect common spelling variations. Return only the greeting text. For example, Laksmi Puja means Lakshmi Puja, not Durga Puja or Subho Bijoya."
        val generated = try {
            // Never leave card creation waiting indefinitely for a missing or busy AI service.
            withTimeoutOrNull(12_000L) { onDeviceGenerate(prompt) }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        return generated?.trim()?.takeIf { it.isNotEmpty() }
            ?: fallbackWish(festivalName, recipient, tone, language)
    }

    suspend fun generateCreativeCard(
        userPrompt: String,
        recipient: String = "Loved One",
        language: String = "English"
    ): ProjectModel {
        val promptLower = userPrompt.lowercase()
        val langLower = language.lowercase()
        val wishText = generateWish(userPrompt, recipient, "Warm", language)

        val config = when {
            isLakshmiPuja(userPrompt) -> {
                CardDesignConfig(
                    bgColor = "gradient:#7C2D12,#D97706",
                    fontStyle = if (langLower.contains("bengali") || langLower.contains("বাংলা")) "bengali" else "serif",
                    headingText = when {
                        langLower.contains("bengali") || langLower.contains("বাংলা") -> "শুভ লক্ষ্মী পূজা"
                        langLower.contains("hindi") || langLower.contains("हिंदी") -> "शुभ लक्ष्मी पूजा"
                        langLower.contains("tamil") || langLower.contains("தமிழ்") -> "இனிய லட்சுமி பூஜை வாழ்த்துக்கள்"
                        else -> "HAPPY LAKSHMI PUJA"
                    },
                    dividerText = "— 🪔 ✦ 🪔 —",
                    textColor = "#FFF8DC"
                )
            }
            promptLower.contains("durga") || promptLower.contains("bijoya") -> {
                CardDesignConfig(
                    bgColor = "gradient:#5C061C,#E5A93C",
                    fontStyle = if (langLower.contains("bengali")) "bengali" else "serif",
                    headingText = if (langLower.contains("hindi")) "दुर्गा पूजा एवं विजयादशमी" else if (langLower.contains("tamil")) "துர்க்கா பூஜை வாழ்த்துக்கள்" else if (langLower.contains("bengali") || langLower.contains("বাংলা")) "শুভ দুর্গা পূজা ও বিজয়ার শুভেচ্ছা" else "HAPPY DURGA PUJA & SUBHO BIJOYA",
                    dividerText = "— 🌸 ✦ 🌸 —",
                    headingColor = "#FFD700",
                    textColor = "#FFF8DC"
                )
            }
            promptLower.contains("eid") -> {
                CardDesignConfig(
                    bgColor = "gradient:#064E3B,#10B981",
                    fontStyle = if (langLower.contains("arabic")) "arabic" else if (langLower.contains("bengali")) "bengali" else "default",
                    headingText = if (langLower.contains("arabic")) "عيد مبارك سعيد" else if (langLower.contains("hindi")) "ईद मुबारक" else if (langLower.contains("tamil")) "ஈத் முபாரக் வாழ்த்துக்கள்" else "EID MUBARAK",
                    dividerText = "— 🌙 ✦ 🌙 —",
                    headingColor = "#FDE047",
                    textColor = "#FFFFFF"
                )
            }
            promptLower.contains("sunday") -> {
                CardDesignConfig(
                    bgColor = "gradient:#FFF7ED,#FDE68A",
                    fontStyle = "serif",
                    headingText = if (langLower.contains("german")) "GESEGNETEN SONNTAG" else if (langLower.contains("hindi")) "शुभ रविवार" else "BLESSED SUNDAY",
                    dividerText = "— 🌿 ✦ 🌿 —",
                    headingColor = "#854D0E",
                    textColor = "#2F3E46"
                )
            }
            promptLower.contains("diwali") || promptLower.contains("deepavali") -> {
                CardDesignConfig(
                    bgColor = "gradient:#2B1055,#8B5CF6",
                    fontStyle = if (langLower.contains("bengali")) "bengali" else "serif",
                    headingText = if (langLower.contains("hindi")) "शुभ दीपावली" else if (langLower.contains("tamil")) "இனிய தீபாவளி வாழ்த்துக்கள்" else "HAPPY DIWALI",
                    dividerText = "— 🪔 ✦ 🪔 —",
                    headingColor = "#FACC15",
                    textColor = "#FFFFFF"
                )
            }
            isNewYear(userPrompt) || promptLower.contains("boishakh") -> {
                CardDesignConfig(
                    bgColor = "gradient:#0284C7,#38BDF8",
                    fontStyle = "serif",
                    headingText = if (langLower.contains("german")) "FROHES NEUES JAHR 2026" else if (langLower.contains("hindi")) "नया साल मुबारक 2026" else if (langLower.contains("tamil")) "இனிய புத்தாண்டு வாழ்த்துக்கள்" else "HAPPY NEW YEAR 2026",
                    dividerText = "— ✨ ✦ ✨ —",
                    headingColor = "#FFFFFF",
                    textColor = "#F0F9FF"
                )
            }
            else -> {
                CardDesignConfig(
                    bgColor = "gradient:#991B1B,#F97316",
                    fontStyle = "cursive",
                    headingText = if (userPrompt.isNotEmpty()) userPrompt.uppercase() else "SPECIAL CELEBRATION",
                    dividerText = "— 🎉 ✦ 🎉 —",
                    headingColor = "#FFD700",
                    textColor = "#FFFFFF"
                )
            }
        }

        val layers = listOf(
            LayerModel.Text(
                id = "t_heading",
                transform = TransformModel(x = 100f, y = 260f, width = 880f, height = 120f, rotation = 0f),
                text = config.headingText,
                fontId = config.fontStyle,
                fontSize = 58f,
                color = config.headingColor,
                alignment = "center"
            ),
            LayerModel.Text(
                id = "t_divider",
                transform = TransformModel(x = 100f, y = 400f, width = 880f, height = 60f, rotation = 0f),
                text = config.dividerText,
                fontId = "default",
                fontSize = 32f,
                color = config.headingColor,
                alignment = "center"
            ),
            LayerModel.Text(
                id = "t_body",
                transform = TransformModel(x = 100f, y = 480f, width = 880f, height = 280f, rotation = 0f),
                text = wishText,
                fontId = config.fontStyle,
                fontSize = 38f,
                color = config.textColor,
                alignment = "center"
            )
        )

        return ProjectModel(
            id = UUID.randomUUID().toString(),
            templateId = "ai-gen-${UUID.randomUUID().toString().take(6)}",
            title = if (userPrompt.isNotEmpty()) userPrompt else "AI Custom Card",
            canvas = CanvasModel(backgroundColor = config.bgColor),
            layers = layers,
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun fallbackWish(festivalName: String, recipient: String, tone: String, language: String): String {
        val festLower = festivalName.lowercase()
        val langLower = language.lowercase()

        if (isLakshmiPuja(festivalName)) {
            return when {
                langLower.contains("bengali") || langLower.contains("বাংলা") ->
                    "শুভ লক্ষ্মী পূজা, প্রিয় $recipient! মা লক্ষ্মীর আশীর্বাদে আপনার ঘর সুখ, শান্তি ও সমৃদ্ধিতে ভরে উঠুক।"
                langLower.contains("hindi") || langLower.contains("हिंदी") ->
                    "शुभ लक्ष्मी पूजा, प्रिय $recipient! माँ लक्ष्मी आपके घर में सुख, शांति और समृद्धि लाएं।"
                langLower.contains("tamil") || langLower.contains("தமிழ்") ->
                    "இனிய லட்சுமி பூஜை வாழ்த்துக்கள் $recipient! லட்சுமி தேவியின் அருளால் உங்கள் இல்லத்தில் மகிழ்ச்சியும் வளமும் பெருகட்டும்."
                langLower.contains("german") || langLower.contains("deutsch") ->
                    "Alles Gute zur Lakshmi Puja, $recipient! Möge Göttin Lakshmi dein Zuhause mit Frieden, Glück und Wohlstand segnen."
                langLower.contains("arabic") || langLower.contains("عربية") ->
                    "أطيب التمنيات بمناسبة لاكشمي بوجا يا $recipient! أتمنى لك ولعائلتك السعادة والسلام والازدهار."
                else -> "Happy Lakshmi Puja, dear $recipient! May Goddess Lakshmi bless your home with happiness, peace, and prosperity."
            }
        }

        return when {
            langLower.contains("hindi") || langLower.contains("हिंदी") -> {
                when {
                    festLower.contains("durga") || festLower.contains("bijoya") ->
                        "दुर्गा पूजा एवं विजयादशमी की हार्दिक शुभकामनाएं प्रिय $recipient! माता रानी आपके जीवन में सुख और शांति लाएं।"
                    festLower.contains("diwali") || festLower.contains("deepavali") ->
                        "दीपावली की हार्दिक शुभकामनाएं $recipient! दीपों का यह पर्व आपके जीवन में प्रकाश और समृद्धि भर दे।"
                    festLower.contains("eid") ->
                        "ईद मुबारक $recipient! अल्लाह आपके जीवन में खुशियां, शांति और बरकत अता फरमाए।"
                    isNewYear(festivalName) ->
                        "नव वर्ष 2026 की हार्दिक शुभकामनाएं प्रिय $recipient! नया साल आपके लिए सुख, समृद्धि और अच्छी सेहत लाए।"
                    else ->
                        "आपको और आपके परिवार को $festivalName की हार्दिक शुभकामनाएं प्रिय $recipient!"
                }
            }
            langLower.contains("tamil") || langLower.contains("தமிழ்") -> {
                when {
                    festLower.contains("diwali") || festLower.contains("deepavali") ->
                        "இனிய தீபாவளி நல்வாழ்த்துக்கள் அன்பு $recipient! உங்கள் வாழ்வில் ஒளியும் மகிழ்ச்சியும் பெருகட்டும்."
                    festLower.contains("eid") ->
                        "இனிய ஈத் முபாரக் வாழ்த்துக்கள் $recipient! உங்கள் வாழ்வில் அமைதியும் மகிழ்ச்சியும் நிலவட்டும்."
                    isNewYear(festivalName) ->
                        "இனிய புத்தாண்டு நல்வாழ்த்துக்கள் $recipient! இந்த ஆண்டு உங்களுக்கு ஆரோக்கியத்தையும் வெற்றியையும் தரட்டும்."
                    else ->
                        "உங்களுக்கும் உங்கள் குடும்பத்தினருக்கும் இனிய $festivalName நல்வாழ்த்துக்கள் $recipient!"
                }
            }
            langLower.contains("german") || langLower.contains("deutsch") -> {
                when {
                    isNewYear(festivalName) ->
                        "Frohes neues Jahr 2026, lieber $recipient! Möge das neue Jahr dir Gesundheit, Glück und viel Erfolg bringen."
                    festLower.contains("sunday") ->
                        "Einen gesegneten und friedvollen Sonntag, lieber $recipient! Genieße diesen wunderschönen Tag."
                    else ->
                        "Herzliche Grüße zum $festivalName, lieber $recipient! Ich wünsche dir und deiner Familie Freude und viel Segen."
                }
            }
            langLower.contains("bengali") || langLower.contains("বাংলা") -> {
                when {
                    festLower.contains("durga") || festLower.contains("bijoya") ->
                        "শুভ বিজয়ার প্রীতি ও আন্তরিক শুভেচ্ছা প্রিয় $recipient! মা দুর্গার আশীর্ষে তোমার জীবন আনন্দে ভরে উঠুক।"
                    festLower.contains("eid") ->
                        "পবিত্র ঈদুল ফিতরের হার্দিক শুভেচ্ছা প্রিয় $recipient! ঈদ মোবারক।"
                    isNewYear(festivalName) || festLower.contains("boishakh") ->
                        "শুভ নববর্ষ প্রিয় $recipient! নতুন বছরের প্রতিটি দিন কাটুক অনাবিল আনন্দে।"
                    else ->
                        "আপনাকে ও আপনার পরিবারকে $festivalName-এর প্রীতি ও শুভেচ্ছা প্রিয় $recipient!"
                }
            }
            langLower.contains("arabic") || langLower.contains("عربية") -> {
                if (festLower.contains("eid")) {
                    "تقبل الله منا ومنكم صالح الأعمال. أتمنى لك ولعائلتك عيداً مباركاً وسعيداً يا $recipient."
                } else {
                    "أطيب التمنيات بمناسبة $festivalName يا $recipient! أتمنى لك ولعائلتك السعادة والسلام."
                }
            }
            else -> {
                when {
                    festLower.contains("durga") || festLower.contains("bijoya") ->
                        "Warm Durga Puja and Subho Bijoya wishes, dear $recipient! May Maa Durga bless your home with health, peace, and joy."
                    festLower.contains("eid") ->
                        "Eid Mubarak to my dear $recipient! May this sacred occasion bring peace, prosperity, and endless blessings."
                    festLower.contains("diwali") || festLower.contains("deepavali") ->
                        "Happy Diwali & Deepavali dear $recipient! May the light of diyas fill your life with warmth and happiness."
                    isNewYear(festivalName) ->
                        "Happy New Year 2026, dear $recipient! May the year ahead be filled with new hope, good health, and success."
                    festLower.contains("sunday") ->
                        "Wishing you a peaceful and blessed Sunday, dear $recipient! Enjoy a relaxing day."
                    else ->
                        "Wishing you a magnificent and blessed $festivalName, dear $recipient! May happiness surround you always."
                }
            }
        }
    }
}

private suspend fun generateOnDeviceWish(prompt: String): String? {
    val client = Generation.getClient()
    return try {
        // Unavailable, downloading and downloadable models all use offline wishes.
        // Model downloads are not a prerequisite for using the card editor.
        if (client.checkStatus() != FeatureStatus.AVAILABLE) null
        else client.generateContent(prompt).candidates.firstOrNull()?.text
    } finally {
        client.close()
    }
}

/** Match the named occasion, never the generic word "puja" alone. */
private fun isLakshmiPuja(input: String): Boolean =
    Regex("(?iu)(?<![\\p{L}])(lakshmi|laksmi|laxmi|lakshm[iy]|লক্ষ্মী|লক্ষী|लक्ष्मी|लक्ष्मि)(?![\\p{L}])")
        .containsMatchIn(input)

private fun isNewYear(input: String): Boolean =
    Regex("(?i)\\bnew\\s+year\\b|নববর্ষ|नव\\s*वर्ष").containsMatchIn(input)
