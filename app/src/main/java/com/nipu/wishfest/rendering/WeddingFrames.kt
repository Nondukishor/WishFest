package com.nipu.wishfest.rendering

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.core.graphics.withSave
import kotlin.math.cos
import kotlin.math.sin

/** Wedding ornaments share the card renderer, so previews and exported invitations match. */
internal object WeddingFrames {
    val options = linkedMapOf(
        "wedding_arch" to "Floral arch",
        "wedding_garland" to "Wedding garland",
        "wedding_mandap" to "Royal mandap",
        "wedding_rings" to "Together forever",
        "wedding_lace" to "Pearl lace",
        "wedding_botanical" to "Botanical vows"
    )
    val styles: Set<String> get() = options.keys

    fun draw(canvas: Canvas, style: String, width: Float, height: Float, color: Int) {
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.style = Paint.Style.STROKE
            strokeWidth = 3f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        fun frame(inset: Float, radius: Float = 18f) =
            canvas.drawRoundRect(inset, inset, width - inset, height - inset, radius, radius, line)
        fun bloom(x: Float, y: Float, radius: Float) {
            for (i in 0 until 7) {
                val a = i * Math.PI * 2 / 7
                fill.alpha = 95
                canvas.drawCircle(x + cos(a).toFloat() * radius * .55f,
                    y + sin(a).toFloat() * radius * .55f, radius * .45f, fill)
            }
            fill.alpha = 255
            canvas.drawCircle(x, y, radius * .23f, fill)
        }
        fun sprig(x: Float, y: Float, angle: Float, length: Float) = canvas.withSave {
            translate(x, y)
            rotate(angle)
            val stem = Path().apply {
                moveTo(0f, 0f); quadTo(13f, -length * .5f, 0f, -length)
            }
            canvas.drawPath(stem, line)
            for (i in 1..5) {
                val py = -length * i / 6
                for (side in listOf(-1f, 1f)) {
                    val leaf = Path().apply {
                        moveTo(5f, py)
                        quadTo(side * 35f, py - 2f, side * 30f, py - 27f)
                        quadTo(side * 9f, py - 27f, 5f, py)
                    }
                    fill.alpha = 65
                    canvas.drawPath(leaf, fill)
                    canvas.drawPath(leaf, line)
                    fill.alpha = 255
                }
            }
        }
        frame(34f)
        when (style) {
            "wedding_arch" -> {
                val arch = Path().apply {
                    moveTo(80f, height - 150f); lineTo(80f, 240f)
                    cubicTo(80f, 20f, width - 80f, 20f, width - 80f, 240f)
                    lineTo(width - 80f, height - 150f)
                }
                canvas.drawPath(arch, line)
                for (x in listOf(112f, width - 112f)) {
                    sprig(x, 210f, if (x < width / 2) 35f else -35f, 135f)
                    bloom(x, 155f, 29f)
                    bloom(x + if (x < width / 2) 40f else -40f, 111f, 20f)
                }
                bloom(width / 2, 78f, 23f)
                sprig(width / 2 - 10f, height - 70f, -72f, 125f)
                sprig(width / 2 + 10f, height - 70f, 72f, 125f)
            }
            "wedding_garland" -> {
                frame(55f)
                val segments = 5
                val span = (width - 160f) / segments
                for (i in 0 until segments) {
                    val start = 80f + i * span
                    val swag = Path().apply {
                        moveTo(start, 82f); quadTo(start + span / 2, 190f, start + span, 82f)
                    }
                    canvas.drawPath(swag, line)
                    for (j in 0..10) {
                        val t = j / 10f
                        bloom(start + span * t, 82f + 216f * t * (1f - t), 10f)
                    }
                }
                for (x in listOf(78f, width - 78f)) {
                    canvas.drawLine(x, 82f, x, height - 165f, line)
                    var y = 91f
                    while (y < height - 165f) { bloom(x, y, 12f); y += 23f }
                }
                bloom(width / 2, height - 85f, 30f)
            }
            "wedding_mandap" -> {
                frame(51f)
                val canopy = Path().apply {
                    moveTo(75f, 185f)
                    cubicTo(width * .24f, 185f, width * .37f, 125f, width / 2, 68f)
                    cubicTo(width * .63f, 125f, width * .76f, 185f, width - 75f, 185f)
                }
                canvas.drawPath(canopy, line)
                for (x in listOf(80f, width - 80f)) {
                    canvas.drawRoundRect(x - 15f, 185f, x + 15f, height - 150f, 8f, 8f, line)
                    for (y in listOf(194f, height - 157f)) {
                        canvas.drawRect(x - 27f, y - 8f, x + 27f, y + 8f, line)
                    }
                    bloom(x, 176f, 24f)
                }
                val drape = Path().apply {
                    moveTo(100f, 170f); quadTo(width / 2, 240f, width - 100f, 170f)
                }
                canvas.drawPath(drape, line)
                for (i in 1..7) {
                    val t = i / 8f
                    val x = 100f + (width - 200f) * t
                    val y = 170f + 140f * t * (1f - t)
                    canvas.drawLine(x, y, x, y + 11f, line)
                    canvas.drawCircle(x, y + 16f, 4f, fill)
                }
                bloom(width / 2, height - 85f, 25f)
            }
            "wedding_rings" -> {
                frame(52f)
                line.strokeWidth = 5f
                canvas.drawCircle(width / 2 - 23f, 117f, 37f, line)
                canvas.drawCircle(width / 2 + 23f, 117f, 37f, line)
                line.strokeWidth = 3f
                val gem = Path().apply {
                    moveTo(width / 2 + 23f, 62f); lineTo(width / 2 + 34f, 73f)
                    lineTo(width / 2 + 23f, 84f); lineTo(width / 2 + 12f, 73f); close()
                }
                canvas.drawPath(gem, fill)
                canvas.drawLine(110f, 120f, width / 2 - 90f, 120f, line)
                canvas.drawLine(width / 2 + 90f, 120f, width - 110f, 120f, line)
                sprig(width / 2 - 8f, height - 75f, -65f, 125f)
                sprig(width / 2 + 8f, height - 75f, 65f, 125f)
            }
            "wedding_lace" -> {
                frame(62f, 28f)
                fun edge(length: Float) {
                    val count = (length / 40f).toInt().coerceAtLeast(1)
                    val step = length / count
                    for (i in 0 until count) {
                        canvas.drawArc(i * step, -12f, (i + 1) * step, 12f, 0f, 180f, false, line)
                        canvas.drawCircle((i + .5f) * step, 22f, 2.5f, fill)
                    }
                }
                canvas.withSave { translate(40f, 40f); edge(width - 80f) }
                canvas.withSave { translate(width - 40f, 40f); rotate(90f); edge(height - 80f) }
                canvas.withSave { translate(width - 40f, height - 40f); rotate(180f); edge(width - 80f) }
                canvas.withSave { translate(40f, height - 40f); rotate(270f); edge(height - 80f) }
                for (x in listOf(105f, width - 105f)) bloom(x, 110f, 27f)
            }
            "wedding_botanical" -> {
                frame(54f)
                for (right in listOf(false, true)) {
                    val x = if (right) width - 92f else 92f
                    val angle = if (right) -20f else 20f
                    sprig(x, 225f, angle, 145f)
                    sprig(x, height - 170f, -angle, 145f)
                    bloom(x, 150f, 18f)
                }
                canvas.drawOval(width / 2 - 35f, 77f, width / 2 + 35f, 130f, line)
                sprig(width / 2 - 8f, height - 75f, -76f, 120f)
                sprig(width / 2 + 8f, height - 75f, 76f, 120f)
            }
        }
    }
}
