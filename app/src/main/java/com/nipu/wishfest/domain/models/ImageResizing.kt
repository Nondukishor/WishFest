package com.nipu.wishfest.domain.models

import kotlin.math.cos
import kotlin.math.sin

/** Keep image proportions and center, shifting inward only when a canvas edge requires it. */
fun resizeImage(transform: TransformModel, width: Float, canvasWidth: Int, canvasHeight: Int): TransformModel {
    val ratio = transform.height.coerceAtLeast(1f) / transform.width.coerceAtLeast(1f)
    val maximum = minOf(canvasWidth.toFloat(), canvasHeight / ratio).coerceAtLeast(1f)
    val minimum = minOf(minOf(canvasWidth, canvasHeight) * .08f, maximum)
    val newWidth = width.coerceIn(minimum, maximum)
    val newHeight = newWidth * ratio
    return transform.copy(
        x = (transform.x + (transform.width - newWidth) / 2).coerceIn(0f, (canvasWidth - newWidth).coerceAtLeast(0f)),
        y = (transform.y + (transform.height - newHeight) / 2).coerceIn(0f, (canvasHeight - newHeight).coerceAtLeast(0f)),
        width = newWidth, height = newHeight
    )
}

/** Project a corner drag onto the image diagonal, including an already rotated image. */
fun resizeImageFromCorner(transform: TransformModel, dx: Float, dy: Float, canvasWidth: Int, canvasHeight: Int): TransformModel {
    val radians = Math.toRadians(transform.rotation.toDouble())
    val localX = dx * cos(radians).toFloat() + dy * sin(radians).toFloat()
    val localY = -dx * sin(radians).toFloat() + dy * cos(radians).toFloat()
    val ratio = transform.height.coerceAtLeast(1f) / transform.width.coerceAtLeast(1f)
    val change = 2f * (localX + localY * ratio) / (1f + ratio * ratio)
    return resizeImage(transform, transform.width + change, canvasWidth, canvasHeight)
}
