package com.nipu.wishfest.rendering

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.nipu.wishfest.domain.models.CanvasModel
import com.nipu.wishfest.domain.models.LayerModel
import com.nipu.wishfest.domain.models.ProjectModel
import java.io.File
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withSave

class SceneRenderer(private val context: Context) {

    fun renderToBitmap(project: ProjectModel, targetWidth: Int, targetHeight: Int): Bitmap {
        val bitmap = createBitmap(targetWidth, targetHeight)
        val canvas = Canvas(bitmap)

        val scaleX = targetWidth.toFloat() / project.canvas.width.toFloat()
        val scaleY = targetHeight.toFloat() / project.canvas.height.toFloat()

        renderCanvasBackground(canvas, project.canvas, targetWidth, targetHeight)

        var lastTextBottomY = 0f

        for (layer in project.layers) {
            if (!layer.visible) continue
            canvas.withSave {
                when (layer) {
                    is LayerModel.Text -> {
                        if (layer.fontId == "sticker") {
                            renderTextLayer(this, layer, scaleX, scaleY, 0f, project.canvas.backgroundColor)
                        } else {
                            lastTextBottomY = renderTextLayer(this, layer, scaleX, scaleY, lastTextBottomY, project.canvas.backgroundColor)
                        }
                    }
                    is LayerModel.Image -> {
                        renderImageLayer(this, layer, scaleX, scaleY)
                    }
                }
            }
        }

        renderBorder(canvas, project.canvas, targetWidth.toFloat(), targetHeight.toFloat())
        renderWatermark(canvas, targetWidth.toFloat(), targetHeight.toFloat())
        return bitmap
    }

    private fun renderWatermark(canvas: Canvas, width: Float, height: Float) {
        val unit = minOf(width, height) / 1080f
        val label = "Created with WishFest"
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(235, 255, 255, 255)
            textSize = 20f * unit
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }
        val right = width - 78f * unit
        val bottom = height - 78f * unit
        val horizontalPadding = 12f * unit
        val verticalPadding = 7f * unit
        val metrics = textPaint.fontMetrics
        val baseline = bottom - verticalPadding - metrics.descent
        val top = baseline + metrics.ascent - verticalPadding
        // A subtle backing keeps the small signature readable over photos and gradients.
        val backing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(110, 0, 0, 0)
        }
        canvas.drawRoundRect(
            right - textPaint.measureText(label) - horizontalPadding * 2,
            top, right, bottom, 8f * unit, 8f * unit, backing
        )
        canvas.drawText(label, right - horizontalPadding, baseline, textPaint)
    }

    private fun renderBorder(canvas: Canvas, model: CanvasModel, width: Float, height: Float) {
        if (model.borderStyle == "none") return
        val unit = minOf(width, height) / 1080f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = try { Color.parseColor(model.borderColor) } catch (_: Exception) { Color.YELLOW }
            style = Paint.Style.STROKE
            strokeWidth = 6f * unit
        }
        if (model.borderStyle in WeddingFrames.styles) {
            canvas.withSave {
                scale(unit, unit)
                WeddingFrames.draw(this, model.borderStyle, width / unit, height / unit, paint.color)
            }
            return
        }
        if (model.borderStyle in HinduFestivalFrames.styles) {
            canvas.withSave {
                scale(unit, unit)
                HinduFestivalFrames.draw(this, model.borderStyle, width / unit, height / unit, paint.color)
            }
            return
        }
        if (model.borderStyle in setOf("scallop", "postage", "deco", "confetti", "ribbon", "hearts")) {
            canvas.withSave {
                scale(unit, unit)
                renderCreativeBorder(this, model.borderStyle, width / unit, height / unit, paint)
            }
            return
        }
        val inset = 36f * unit
        if (model.borderStyle == "dashed") {
            paint.pathEffect = android.graphics.DashPathEffect(floatArrayOf(18f * unit, 14f * unit), 0f)
        }
        canvas.drawRoundRect(inset, inset, width - inset, height - inset, 32f * unit, 32f * unit, paint)
        paint.pathEffect = null
        if (model.borderStyle == "double" || model.borderStyle == "floral") {
            paint.strokeWidth = 2f * unit
            val inner = 55f * unit
            canvas.drawRoundRect(inner, inner, width - inner, height - inner, 24f * unit, 24f * unit, paint)
        }
        if (model.borderStyle == "floral") {
            paint.style = Paint.Style.FILL
            for (cx in listOf(inset, width - inset)) for (cy in listOf(inset, height - inset)) {
                for (i in 0 until 6) {
                    val angle = i * Math.PI / 3
                    canvas.drawCircle(cx + kotlin.math.cos(angle).toFloat() * 20f * unit,
                        cy + kotlin.math.sin(angle).toFloat() * 20f * unit, 13f * unit, paint)
                }
                paint.color = Color.WHITE
                canvas.drawCircle(cx, cy, 9f * unit, paint)
                paint.color = try { Color.parseColor(model.borderColor) } catch (_: Exception) { Color.YELLOW }
            }
        }
    }

    private fun renderCreativeBorder(canvas: Canvas, style: String, width: Float, height: Float, paint: Paint) {
        paint.strokeWidth = 4f
        paint.strokeCap = Paint.Cap.ROUND
        fun outline(inset: Float, radius: Float = 0f) {
            canvas.drawRoundRect(inset, inset, width - inset, height - inset, radius, radius, paint)
        }
        // Draw each motif in local edge coordinates so rectangular cards work too.
        fun edges(draw: (Float) -> Unit) {
            canvas.withSave { translate(36f, 36f); draw(width - 72f) }
            canvas.withSave { translate(width - 36f, 36f); rotate(90f); draw(height - 72f) }
            canvas.withSave { translate(width - 36f, height - 36f); rotate(180f); draw(width - 72f) }
            canvas.withSave { translate(36f, height - 36f); rotate(270f); draw(height - 72f) }
        }
        when (style) {
            "scallop" -> {
                outline(58f, 24f)
                edges { length ->
                    val count = (length / 44f).toInt().coerceAtLeast(1)
                    val step = length / count
                    for (i in 0 until count) {
                        canvas.drawArc(i * step, -16f, (i + 1) * step, 16f, 0f, 180f, false, paint)
                    }
                }
            }
            "postage" -> {
                paint.strokeWidth = 3f
                outline(60f)
                edges { length ->
                    val count = (length / 34f).toInt().coerceAtLeast(1)
                    val step = length / count
                    val path = android.graphics.Path().apply {
                        moveTo(0f, 0f)
                        for (i in 0 until count) {
                            lineTo(i * step + step * .2f, 0f)
                            cubicTo(i * step + step * .2f, 18f, i * step + step * .8f, 18f, i * step + step * .8f, 0f)
                            lineTo((i + 1) * step, 0f)
                        }
                    }
                    canvas.drawPath(path, paint)
                }
            }
            "deco" -> {
                outline(36f)
                outline(52f)
                for (right in listOf(false, true)) for (bottom in listOf(false, true)) {
                    canvas.withSave {
                        translate(if (right) width else 0f, if (bottom) height else 0f)
                        scale(if (right) -1f else 1f, if (bottom) -1f else 1f)
                        val path = android.graphics.Path().apply {
                            moveTo(36f, 145f); lineTo(78f, 145f); lineTo(78f, 78f)
                            lineTo(145f, 78f); lineTo(145f, 36f)
                            moveTo(52f, 115f); lineTo(100f, 115f); lineTo(100f, 52f)
                        }
                        canvas.drawPath(path, paint)
                    }
                }
            }
            "confetti" -> {
                outline(70f, 28f)
                edges { length ->
                    val count = (length / 42f).toInt().coerceAtLeast(1)
                    for (i in 0..count) {
                        canvas.withSave {
                            translate(i * length / count, if (i % 2 == 0) -9f else 9f)
                            rotate((i * 37 % 120).toFloat())
                            if (i % 3 == 0) {
                                canvas.drawCircle(0f, 0f, 5f, paint)
                            } else {
                                canvas.drawLine(-5f, -7f, 5f, 7f, paint)
                            }
                        }
                    }
                }
            }
            "ribbon" -> {
                outline(48f, 20f)
                for (right in listOf(false, true)) for (bottom in listOf(false, true)) {
                    canvas.withSave {
                        translate(if (right) width else 0f, if (bottom) height else 0f)
                        scale(if (right) -1f else 1f, if (bottom) -1f else 1f)
                        paint.style = Paint.Style.FILL
                        val path = android.graphics.Path().apply {
                            moveTo(18f, 106f); lineTo(106f, 18f); lineTo(160f, 18f)
                            lineTo(18f, 160f); close()
                        }
                        canvas.drawPath(path, paint)
                        paint.style = Paint.Style.STROKE
                    }
                }
            }
            "hearts" -> {
                outline(62f, 30f)
                edges { length ->
                    val count = (length / 76f).toInt().coerceAtLeast(1)
                    for (i in 0 until count) {
                        canvas.withSave {
                            translate((i + .5f) * length / count, 0f)
                            val heart = android.graphics.Path().apply {
                                moveTo(0f, 12f)
                                cubicTo(-28f, -4f, -12f, -22f, 0f, -10f)
                                cubicTo(12f, -22f, 28f, -4f, 0f, 12f)
                                close()
                            }
                            canvas.drawPath(heart, paint)
                        }
                    }
                }
            }
        }
    }

    private fun isLightBackground(bgColor: String): Boolean {
        return try {
            val parseColor = if (bgColor.startsWith("gradient:")) {
                val hexes = bgColor.removePrefix("gradient:").split(",")
                Color.parseColor(hexes[0].trim())
            } else if (bgColor.startsWith("#")) {
                Color.parseColor(bgColor.trim())
            } else {
                Color.WHITE
            }
            val red = Color.red(parseColor)
            val green = Color.green(parseColor)
            val blue = Color.blue(parseColor)
            (red * 0.299 + green * 0.587 + blue * 0.114) > 180
        } catch (e: Exception) {
            false
        }
    }

    private fun renderCanvasBackground(canvas: Canvas, canvasModel: CanvasModel, targetWidth: Int, targetHeight: Int) {
        if (canvasModel.backgroundPhotoPath.isNotEmpty() && File(canvasModel.backgroundPhotoPath).exists()) {
            val bgBitmap = BitmapFactory.decodeFile(canvasModel.backgroundPhotoPath)
            if (bgBitmap != null) {
                val matrix = Matrix()
                val srcWidth = bgBitmap.width.toFloat()
                val srcHeight = bgBitmap.height.toFloat()
                
                // Aspect-Fill Center-Crop scaling to prevent stretching
                val scale = maxOf(targetWidth.toFloat() / srcWidth, targetHeight.toFloat() / srcHeight)
                val dx = (targetWidth.toFloat() - srcWidth * scale) / 2f
                val dy = (targetHeight.toFloat() - srcHeight * scale) / 2f
                
                matrix.postScale(scale, scale)
                matrix.postTranslate(dx, dy)
                
                if (canvasModel.backgroundPhotoRotation != 0f) {
                    matrix.postRotate(canvasModel.backgroundPhotoRotation, targetWidth / 2f, targetHeight / 2f)
                }
                
                val bgPaint = Paint().apply {
                    isAntiAlias = true
                    isFilterBitmap = true
                }
                canvas.drawBitmap(bgBitmap, matrix, bgPaint)
                return
            }
        }

        val paint = Paint().apply {
            style = Paint.Style.FILL
            if (canvasModel.backgroundColor.startsWith("gradient:")) {
                val hexes = canvasModel.backgroundColor.removePrefix("gradient:").split(",")
                if (hexes.size >= 2) {
                    val c1 = try { Color.parseColor(hexes[0].trim()) } catch (e: Exception) { Color.RED }
                    val c2 = try { Color.parseColor(hexes[1].trim()) } catch (e: Exception) { Color.YELLOW }
                    shader = android.graphics.LinearGradient(
                        0f, 0f, targetWidth.toFloat(), targetHeight.toFloat(),
                        c1, c2,
                        android.graphics.Shader.TileMode.CLAMP
                    )
                } else {
                    color = Color.WHITE
                }
            } else {
                color = try {
                    Color.parseColor(canvasModel.backgroundColor)
                } catch (e: Exception) {
                    Color.WHITE
                }
            }
        }
        canvas.drawRect(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat(), paint)
    }

    private fun renderTextLayer(
        canvas: Canvas,
        layer: LayerModel.Text,
        scaleX: Float,
        scaleY: Float,
        previousTextBottomY: Float,
        backgroundColor: String
    ): Float {
        val originalPosY = layer.transform.y * scaleY
        val width = layer.transform.width * scaleX

        // Smart contrast protection for light card backgrounds
        val effectiveColorStr = if (isLightBackground(backgroundColor) &&
            (layer.color.equals("#FFFFFF", ignoreCase = true) || layer.color.equals("#FFF8DC", ignoreCase = true))) {
            "#1B4332"
        } else {
            layer.color
        }

        val textPaint = TextPaint().apply {
            isAntiAlias = true
            color = try {
                Color.parseColor(effectiveColorStr)
            } catch (e: Exception) {
                Color.BLACK
            }
            textSize = layer.fontSize * scaleX
            alpha = (layer.opacity * 255).toInt()
            typeface = getTypefaceForFontId(layer.fontId)
        }

        val alignment = when (layer.alignment.lowercase()) {
            "left" -> Layout.Alignment.ALIGN_NORMAL
            "right" -> Layout.Alignment.ALIGN_OPPOSITE
            else -> Layout.Alignment.ALIGN_CENTER
        }

        val staticLayout = StaticLayout.Builder
            .obtain(layer.text, 0, layer.text.length, textPaint, width.toInt().coerceAtLeast(10))
            .setAlignment(alignment)
            .setLineSpacing(0f, 1f)
            .setIncludePad(true)
            .build()

        val adjustedPosY = originalPosY

        val posX = layer.transform.x * scaleX
        canvas.translate(posX, adjustedPosY)

        if (layer.transform.rotation != 0f) {
            canvas.rotate(layer.transform.rotation, width / 2f, staticLayout.height / 2f)
        }

        staticLayout.draw(canvas)

        return adjustedPosY + staticLayout.height
    }

    private fun renderImageLayer(canvas: Canvas, layer: LayerModel.Image, scaleX: Float, scaleY: Float) {
        if (layer.assetPath.isEmpty()) return
        val file = File(layer.assetPath)
        val imgBitmap = if (file.exists()) {
            BitmapFactory.decodeFile(file.absolutePath)
        } else {
            try {
                context.assets.open(layer.assetPath).use {
                    BitmapFactory.decodeStream(it)
                }
            } catch (e: Exception) {
                null
            }
        } ?: return

        val posX = layer.transform.x * scaleX
        val posY = layer.transform.y * scaleY
        val width = layer.transform.width * scaleX
        val height = layer.transform.height * scaleY

        canvas.translate(posX, posY)
        if (layer.transform.rotation != 0f) {
            canvas.rotate(layer.transform.rotation, width / 2f, height / 2f)
        }

        val matrix = Matrix().apply {
            val scale = minOf(width / imgBitmap.width.toFloat(), height / imgBitmap.height.toFloat())
            val dx = (width - imgBitmap.width * scale) / 2f
            val dy = (height - imgBitmap.height * scale) / 2f
            postScale(scale, scale)
            postTranslate(dx, dy)
        }

        val paint = Paint().apply {
            isAntiAlias = true
            alpha = (layer.opacity * 255).toInt()
        }

        canvas.drawBitmap(imgBitmap, matrix, paint)
    }

    private fun getTypefaceForFontId(fontId: String): Typeface {
        return try {
            when (fontId.lowercase()) {
                "bengali", "noto-sans-bengali" -> {
                    Typeface.createFromAsset(context.assets, "fonts/NotoSansBengali-Regular.ttf")
                }
                "arabic", "noto-naskh-arabic" -> {
                    Typeface.createFromAsset(context.assets, "fonts/NotoNaskhArabic-Regular.ttf")
                }
                "serif", "elegant" -> Typeface.SERIF
                "cursive", "calligraphy" -> Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
                "bold", "headline" -> Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
                "monospace", "retro" -> Typeface.MONOSPACE
                else -> Typeface.DEFAULT
            }
        } catch (e: Exception) {
            Typeface.DEFAULT
        }
    }
}
