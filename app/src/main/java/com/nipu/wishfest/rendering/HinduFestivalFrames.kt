package com.nipu.wishfest.rendering

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import androidx.core.graphics.withSave
import kotlin.math.cos
import kotlin.math.sin

/** Vector motifs stay sharp in previews and exports without downloading artwork. */
internal object HinduFestivalFrames {
    val lakshmiOptions = linkedMapOf(
        "lakshmi_lotus" to "Lotus & prosperity",
        "lakshmi_alpana" to "Alpana & sacred footprints",
        "lakshmi_diya" to "Diya & mango garland"
    )
    val options = linkedMapOf(
        "royal_arch_garland" to "🐘 Royal Arch & Marigold Garland",
        "mandala_arch" to "👑 Royal Mandala Arch",
        "royal_crest" to "⚜️ Royal Gold Crest",
        "royal_filigree" to "✨ Corner Filigree & Border",
        "diya" to "🪔 Pradip / Diya", "om" to "ॐ Om blessings",
        "toran" to "Marigold toran", "lotus" to "Lotus petals", "rangoli" to "Rangoli",
        "alpana" to "Alpana lace", "mango" to "Mango-leaf toran", "temple" to "Temple arch",
        "shankha" to "Shankha / Conch", "kalash" to "Puja kalash", "bells" to "Temple bells"
    ).apply { putAll(lakshmiOptions) }
    val styles: Set<String> get() = options.keys

    fun draw(canvas: Canvas, style: String, width: Float, height: Float, color: Int) {
        val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.style = Paint.Style.STROKE
            strokeWidth = 3.5f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        canvas.drawRoundRect(28f, 28f, width - 28f, height - 28f, 24f, 24f, outline)
        outline.alpha = 160
        canvas.drawRoundRect(44f, 44f, width - 44f, height - 44f, 18f, 18f, outline)
        outline.alpha = 255

        fun mandala(cx: Float, cy: Float, radius: Float) = canvas.withSave {
            translate(cx, cy)
            val numPetals = 16
            for (i in 0 until numPetals) {
                canvas.withSave {
                    rotate(i * (360f / numPetals))
                    val petal = Path().apply {
                        moveTo(0f, -radius * 0.35f)
                        cubicTo(-radius * 0.25f, -radius * 0.7f, -radius * 0.15f, -radius, 0f, -radius)
                        cubicTo(radius * 0.15f, -radius, radius * 0.25f, -radius * 0.7f, 0f, -radius * 0.35f)
                        close()
                    }
                    fill.alpha = 55
                    canvas.drawPath(petal, fill)
                    fill.alpha = 255
                    canvas.drawPath(petal, outline)
                    canvas.drawCircle(0f, -radius - 6f, 3.5f, fill)
                }
            }
            for (i in 0 until 12) {
                canvas.withSave {
                    rotate(i * 30f)
                    canvas.drawOval(-radius * 0.12f, -radius * 0.45f, radius * 0.12f, -radius * 0.2f, outline)
                }
            }
            canvas.drawCircle(0f, 0f, radius * 0.38f, outline)
            canvas.drawCircle(0f, 0f, radius * 0.18f, fill)
        }

        fun cornerFiligree(cx: Float, cy: Float, flipX: Boolean, flipY: Boolean) = canvas.withSave {
            translate(cx, cy)
            scale(if (flipX) -1f else 1f, if (flipY) -1f else 1f)
            val p = Path().apply {
                moveTo(20f, 20f)
                cubicTo(45f, 20f, 75f, 35f, 85f, 75f)
                moveTo(20f, 20f)
                cubicTo(20f, 45f, 35f, 75f, 75f, 85f)
                moveTo(20f, 35f)
                cubicTo(40f, 35f, 60f, 50f, 65f, 85f)
                moveTo(35f, 20f)
                cubicTo(35f, 40f, 50f, 60f, 85f, 65f)
            }
            canvas.drawPath(p, outline)
            canvas.drawCircle(30f, 30f, 6f, fill)
            canvas.drawCircle(50f, 50f, 4f, fill)
        }

        fun elephant(cx: Float, cy: Float, flipX: Boolean) = canvas.withSave {
            translate(cx, cy)
            if (flipX) scale(-1f, 1f)
            val body = Path().apply {
                moveTo(20f, 0f)
                cubicTo(35f, -25f, 65f, -25f, 80f, 0f)
                cubicTo(90f, 15f, 85f, 35f, 75f, 45f)
                lineTo(75f, 70f); lineTo(60f, 70f); lineTo(60f, 45f)
                lineTo(40f, 45f); lineTo(40f, 70f); lineTo(25f, 70f)
                lineTo(25f, 40f); cubicTo(10f, 35f, -10f, 25f, -25f, -10f)
                cubicTo(-20f, -25f, 5f, -20f, 10f, -10f)
                close()
            }
            fill.alpha = 230
            canvas.drawPath(body, fill)
            fill.alpha = 255
        }

        fun marigoldGarlandColumn(x: Float) {
            val step = 32f
            var y = 60f
            val leafPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = Color.rgb(16, 185, 129) }
            val marigoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = Color.rgb(245, 158, 11) }
            val marigoldDark = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = Color.rgb(217, 119, 6) }

            while (y < height - 60f) {
                canvas.drawCircle(x, y, 16f, marigoldDark)
                for (i in 0 until 6) {
                    val a = i * Math.PI / 3
                    canvas.drawCircle(x + cos(a).toFloat() * 10f, y + sin(a).toFloat() * 10f, 7f, marigoldPaint)
                }
                if (y.toInt() % 128 < 32) {
                    val leaf = Path().apply {
                        moveTo(x - 22f, y); quadTo(x, y + 10f, x + 22f, y); quadTo(x, y - 10f, x - 22f, y); close()
                    }
                    canvas.drawPath(leaf, leafPaint)
                }
                y += step
            }
        }

        fun diya(x: Float, y: Float) = canvas.withSave {
            translate(x, y)
            val bowl = Path().apply {
                moveTo(-42f, 0f); quadTo(0f, 58f, 42f, 0f); close()
            }
            canvas.drawPath(bowl, fill)
            canvas.drawOval(-42f, -8f, 42f, 8f, outline)
            canvas.drawLine(0f, -2f, 0f, -18f, outline)
            val flame = Path().apply {
                moveTo(0f, -16f)
                cubicTo(-28f, -34f, -3f, -51f, 0f, -70f)
                cubicTo(7f, -48f, 29f, -31f, 0f, -16f)
                close()
            }
            val flamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = Color.rgb(255, 160, 36) }
            canvas.drawPath(flame, flamePaint)
            flamePaint.color = Color.rgb(255, 231, 146)
            canvas.drawOval(-5f, -38f, 6f, -19f, flamePaint)
        }

        fun flower(x: Float, y: Float, radius: Float) {
            for (i in 0 until 8) {
                val angle = i * Math.PI / 4
                canvas.drawCircle(x + cos(angle).toFloat() * radius * .58f,
                    y + sin(angle).toFloat() * radius * .58f, radius * .38f, fill)
            }
            canvas.drawCircle(x, y, radius * .25f, outline)
        }

        fun lotus(x: Float, y: Float) = canvas.withSave {
            translate(x, y)
            for (angle in listOf(-55f, -28f, 0f, 28f, 55f)) {
                canvas.withSave {
                    rotate(angle)
                    val petal = Path().apply {
                        moveTo(0f, 15f)
                        cubicTo(-30f, -5f, -22f, -38f, 0f, -60f)
                        cubicTo(22f, -38f, 30f, -5f, 0f, 15f)
                        close()
                    }
                    fill.alpha = 45
                    canvas.drawPath(petal, fill)
                    canvas.drawPath(petal, outline)
                    fill.alpha = 255
                }
            }
            canvas.drawArc(-53f, 3f, 53f, 32f, 0f, 180f, false, outline)
        }

        fun om(x: Float, baseline: Float) {
            val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                textSize = 76f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            }
            canvas.drawText("ॐ", x, baseline, text)
            canvas.drawLine(x - 110f, baseline - 22f, x - 60f, baseline - 22f, outline)
            canvas.drawLine(x + 60f, baseline - 22f, x + 110f, baseline - 22f, outline)
        }

        fun mangoLeaf(x: Float, y: Float, angle: Float = 0f) = canvas.withSave {
            translate(x, y)
            rotate(angle)
            val leaf = Path().apply {
                moveTo(0f, 0f)
                cubicTo(-27f, 23f, -20f, 57f, 0f, 83f)
                cubicTo(20f, 57f, 27f, 23f, 0f, 0f)
                close()
            }
            fill.alpha = 55
            canvas.drawPath(leaf, fill)
            canvas.drawPath(leaf, outline)
            fill.alpha = 255
            canvas.drawLine(0f, 6f, 0f, 72f, outline)
            for (yOffset in listOf(24f, 40f, 56f)) {
                canvas.drawLine(0f, yOffset, -12f, yOffset - 10f, outline)
                canvas.drawLine(0f, yOffset, 12f, yOffset - 10f, outline)
            }
        }

        fun conch(x: Float, y: Float) = canvas.withSave {
            translate(x, y)
            val shell = Path().apply {
                moveTo(26f, -48f)
                cubicTo(-12f, -60f, -50f, -22f, -38f, 16f)
                cubicTo(-31f, 36f, -5f, 34f, 3f, 49f)
                lineTo(21f, 57f); lineTo(15f, 29f)
                cubicTo(47f, 7f, 45f, -27f, 26f, -48f)
                close()
            }
            fill.alpha = 40
            canvas.drawPath(shell, fill)
            canvas.drawPath(shell, outline)
            fill.alpha = 255
            val spiral = Path().apply {
                moveTo(23f, -25f)
                cubicTo(-6f, -47f, -38f, -7f, -13f, 8f)
                cubicTo(8f, 19f, 26f, -7f, 8f, -15f)
                cubicTo(-3f, -21f, -9f, -4f, 1f, -2f)
            }
            canvas.drawPath(spiral, outline)
            canvas.drawLine(-20f, 25f, 12f, 29f, outline)
            canvas.drawLine(4f, 41f, 18f, 45f, outline)
        }

        fun bell(x: Float, y: Float) = canvas.withSave {
            translate(x, y)
            canvas.drawLine(0f, -48f, 0f, -12f, outline)
            canvas.drawCircle(0f, -8f, 5f, outline)
            val body = Path().apply {
                moveTo(-30f, 42f)
                cubicTo(-14f, 27f, -25f, 0f, 0f, 0f)
                cubicTo(25f, 0f, 14f, 27f, 30f, 42f)
                close()
            }
            fill.alpha = 55
            canvas.drawPath(body, fill)
            fill.alpha = 255
            canvas.drawPath(body, outline)
            canvas.drawOval(-32f, 36f, 32f, 49f, outline)
            canvas.drawCircle(0f, 53f, 6f, fill)
            canvas.drawLine(-12f, 12f, -13f, 29f, outline)
        }

        fun footprints(x: Float, y: Float) = canvas.withSave {
            translate(x, y)
            for (side in listOf(-1f, 1f)) {
                canvas.withSave {
                    translate(side * 22f, 0f)
                    rotate(side * 10f)
                    // The toes point inward toward the card, welcoming Lakshmi home.
                    canvas.drawOval(-9f, -8f, 9f, 26f, fill)
                    canvas.drawCircle(0f, 28f, 7f, fill)
                    for (toe in 0..4) {
                        val radius = 5f - toe * .65f
                        canvas.drawCircle(side * (-8f + toe * 4.7f), -16f + toe * 2f, radius, fill)
                    }
                }
            }
        }

        when (style) {
            "lakshmi_lotus" -> {
                // Symmetrical lotus medallions and coin-like beads leave the wish clear.
                lotus(width / 2, 140f)
                lotus(115f, height - 92f)
                lotus(width - 115f, height - 92f)
                for (x in listOf(76f, width - 76f)) {
                    var y = 170f
                    while (y < height - 185f) {
                        canvas.drawCircle(x, y, 9f, outline)
                        canvas.drawCircle(x, y, 3f, fill)
                        y += 52f
                    }
                }
                canvas.drawLine(150f, 82f, width / 2 - 90f, 82f, outline)
                canvas.drawLine(width / 2 + 90f, 82f, width - 150f, 82f, outline)
                diya(width / 2, height - 75f)
            }
            "lakshmi_alpana" -> {
                for (x in listOf(115f, width - 115f)) {
                    mandala(x, 120f, 50f)
                    mandala(x, height - 125f, 50f)
                }
                for (x in listOf(74f, width - 74f)) {
                    var y = 220f
                    while (y < height - 205f) {
                        canvas.drawOval(x - 7f, y - 14f, x + 7f, y + 14f, outline)
                        canvas.drawCircle(x, y + 23f, 3f, fill)
                        y += 56f
                    }
                }
                lotus(width / 2, 135f)
                footprints(width / 2, height - 115f)
                canvas.drawLine(205f, height - 100f, width / 2 - 80f, height - 100f, outline)
                canvas.drawLine(width / 2 + 80f, height - 100f, width - 205f, height - 100f, outline)
            }
            "lakshmi_diya" -> {
                val garland = Path().apply {
                    moveTo(85f, 72f)
                    quadTo(width / 2, 150f, width - 85f, 72f)
                }
                canvas.drawPath(garland, outline)
                for (i in 0..8) {
                    val t = i / 8f
                    val x = 85f + t * (width - 170f)
                    val y = 72f + 156f * t * (1f - t)
                    flower(x, y, 12f)
                    if (i % 2 == 1) {
                        canvas.withSave { translate(x, y + 12f); scale(.55f, .55f); mangoLeaf(0f, 0f) }
                    }
                }
                for (x in listOf(115f, width - 115f)) {
                    diya(x, height - 95f)
                    cornerFiligree(if (x < width / 2) 0f else width, height, x > width / 2, true)
                }
                lotus(width / 2, height - 85f)
                for (x in listOf(72f, width - 72f)) {
                    var y = 210f
                    while (y < height - 230f) { canvas.drawCircle(x, y, 3f, fill); y += 34f }
                }
            }
            "royal_arch_garland" -> {
                marigoldGarlandColumn(36f)
                marigoldGarlandColumn(width - 36f)

                mandala(width / 2, 85f, 120f)
                elephant(130f, 110f, false)
                elephant(width - 130f, 110f, true)

                val archWindow = Path().apply {
                    moveTo(72f, height - 60f)
                    lineTo(72f, 260f)
                    cubicTo(72f, 180f, width / 2 - 80f, 160f, width / 2, 160f)
                    cubicTo(width / 2 + 80f, 160f, width - 72f, 180f, width - 72f, 260f)
                    lineTo(width - 72f, height - 60f)
                    close()
                }
                val windowFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = Color.WHITE }
                canvas.drawPath(archWindow, windowFill)
                canvas.drawPath(archWindow, outline)
            }
            "mandala_arch" -> {
                mandala(width / 2, height - 120f, 220f)
                cornerFiligree(0f, 0f, false, false)
                cornerFiligree(width, 0f, true, false)
                cornerFiligree(0f, height, false, true)
                cornerFiligree(width, height, true, true)
            }
            "royal_crest" -> {
                mandala(width / 2, 160f, 180f)
                cornerFiligree(0f, 0f, false, false)
                cornerFiligree(width, 0f, true, false)
                cornerFiligree(0f, height, false, true)
                cornerFiligree(width, height, true, true)
                diya(width / 2, height - 80f)
            }
            "royal_filigree" -> {
                cornerFiligree(0f, 0f, false, false)
                cornerFiligree(width, 0f, true, false)
                cornerFiligree(0f, height, false, true)
                cornerFiligree(width, height, true, true)
                for (x in listOf(80f, width - 80f)) {
                    flower(x, 120f, 18f)
                    flower(x, height - 120f, 18f)
                }
            }
            "alpana" -> {
                for (x in listOf(100f, width - 100f)) {
                    mandala(x, 110f, 49f)
                }
                mandala(width / 2, height - 90f, 42f)
                for (x in listOf(76f, width - 76f)) {
                    var y = 205f
                    while (y < height - 170f) {
                        canvas.drawOval(x - 7f, y - 13f, x + 7f, y + 13f, outline)
                        canvas.drawCircle(x, y + 23f, 2.5f, fill)
                        y += 54f
                    }
                }
                canvas.drawLine(182f, 76f, width - 182f, 76f, outline)
            }
            "mango" -> {
                val count = 10
                val span = (width - 180f) / count
                val string = Path().apply {
                    moveTo(70f, 70f)
                    quadTo(width / 2, 138f, width - 70f, 70f)
                }
                canvas.drawPath(string, outline)
                for (i in 0 until count) {
                    val t = (i + .5f) / count
                    mangoLeaf(90f + (i + .5f) * span, 70f + 136f * t * (1f - t), if (i % 2 == 0) -12f else 12f)
                }
                flower(78f, 78f, 20f)
                flower(width - 78f, 78f, 20f)
                lotus(width / 2, height - 80f)
            }
            "temple" -> {
                val arch = Path().apply {
                    moveTo(78f, height - 145f); lineTo(78f, 194f)
                    cubicTo(width * .23f, 194f, width * .35f, 145f, width / 2, 76f)
                    cubicTo(width * .65f, 145f, width * .77f, 194f, width - 78f, 194f)
                    lineTo(width - 78f, height - 145f)
                }
                canvas.drawPath(arch, outline)
                for (x in listOf(78f, width - 78f)) {
                    canvas.drawLine(x - 12f, 210f, x - 12f, height - 165f, outline)
                    canvas.drawLine(x + 12f, 210f, x + 12f, height - 165f, outline)
                    for (y in listOf(210f, height - 160f)) {
                        canvas.drawRoundRect(x - 22f, y - 7f, x + 22f, y + 7f, 3f, 3f, outline)
                    }
                }
                bell(width / 2, 130f)
                diya(width / 2, height - 70f)
            }
            "shankha" -> {
                conch(116f, 132f)
                conch(width - 116f, 132f)
                lotus(width / 2, height - 80f)
                for (x in listOf(76f, width - 76f)) {
                    var y = 235f
                    while (y < height - 165f) {
                        canvas.drawCircle(x, y, 4f, outline)
                        y += 36f
                    }
                }
                canvas.drawLine(200f, 82f, width - 200f, 82f, outline)
            }
            "kalash" -> {
                canvas.withSave {
                    translate(width / 2, 145f)
                    for (angle in listOf(-55f, -28f, 28f, 55f)) {
                        canvas.withSave {
                            rotate(angle, 0f, -15f)
                            val leaf = Path().apply {
                                moveTo(0f, -15f); quadTo(-28f, -40f, 0f, -70f)
                                quadTo(28f, -40f, 0f, -15f); close()
                            }
                            canvas.drawPath(leaf, outline)
                        }
                    }
                    canvas.drawOval(-17f, -77f, 17f, -23f, outline)
                    val pot = Path().apply {
                        moveTo(-28f, -12f); lineTo(-25f, 5f)
                        cubicTo(-62f, 28f, -46f, 63f, 0f, 66f)
                        cubicTo(46f, 63f, 62f, 28f, 25f, 5f)
                        lineTo(28f, -12f); close()
                    }
                    fill.alpha = 45
                    canvas.drawPath(pot, fill)
                    fill.alpha = 255
                    canvas.drawPath(pot, outline)
                    canvas.drawOval(-29f, -18f, 29f, -6f, outline)
                    canvas.drawLine(-38f, 29f, 38f, 29f, outline)
                    canvas.drawLine(-39f, 38f, 39f, 38f, outline)
                    flower(0f, 46f, 10f)
                }
                flower(94f, 94f, 26f)
                flower(width - 94f, 94f, 26f)
                diya(width / 2, height - 70f)
            }
            "bells" -> {
                for (i in 0..4) {
                    val x = 110f + i * (width - 220f) / 4
                    bell(x, if (i % 2 == 0) 122f else 150f)
                }
                canvas.drawLine(80f, 70f, width - 80f, 70f, outline)
                for (x in listOf(76f, width - 76f)) {
                    var y = 235f
                    while (y < height - 165f) { flower(x, y, 10f); y += 40f }
                }
                lotus(width / 2, height - 80f)
            }
            "diya" -> {
                diya(110f, 145f)
                diya(width - 110f, 145f)
                diya(width / 2, height - 70f)
                for (x in listOf(72f, width - 72f)) {
                    var y = 220f
                    while (y < height - 160f) {
                        canvas.drawCircle(x, y, 4f, fill)
                        y += 44f
                    }
                }
            }
            "om" -> {
                om(width / 2, 142f)
                diya(115f, height - 80f)
                diya(width - 115f, 145f)
                for (x in listOf(78f, width - 78f)) {
                    flower(x, 78f, 23f)
                }
            }
            "toran" -> {
                val segments = 6
                val span = (width - 160f) / segments
                for (i in 0 until segments) {
                    val start = 80f + i * span
                    val swag = Path().apply {
                        moveTo(start, 72f); quadTo(start + span / 2, 164f, start + span, 72f)
                    }
                    canvas.drawPath(swag, outline)
                    for (j in 0..8) {
                        val t = j / 8f
                        flower(start + span * t, 72f + 184f * t * (1f - t), 9f)
                    }
                    val leaf = Path().apply {
                        val cx = start + span / 2
                        moveTo(cx, 126f); quadTo(cx - 20f, 147f, cx, 172f)
                        quadTo(cx + 20f, 147f, cx, 126f); close()
                    }
                    fill.alpha = 190
                    canvas.drawPath(leaf, fill)
                    fill.alpha = 255
                }
                for (x in listOf(78f, width - 78f)) {
                    canvas.drawLine(x, 80f, x, height - 160f, outline)
                    var y = 85f
                    while (y < height - 160f) { flower(x, y, 14f); y += 28f }
                }
            }
            "lotus" -> {
                lotus(115f, 140f)
                lotus(width - 115f, 140f)
                lotus(width / 2, height - 80f)
                canvas.drawLine(190f, 88f, width - 190f, 88f, outline)
                for (x in listOf(72f, width - 72f)) {
                    var y = 230f
                    while (y < height - 160f) {
                        canvas.drawCircle(x, y, 3f, fill)
                        y += 32f
                    }
                }
            }
            "rangoli" -> {
                for (x in listOf(90f, width - 90f)) {
                    for (y in listOf(100f, height - 140f)) {
                        canvas.withSave {
                            translate(x, y)
                            for (i in 0 until 12) {
                                canvas.withSave {
                                    rotate(i * 30f)
                                    canvas.drawOval(-9f, -48f, 9f, -14f, outline)
                                    canvas.drawCircle(0f, -57f, 3f, fill)
                                }
                            }
                            canvas.drawCircle(0f, 0f, 12f, outline)
                            canvas.drawCircle(0f, 0f, 4f, fill)
                        }
                    }
                }
                flower(width / 2, 80f, 24f)
            }
        }
    }
}
