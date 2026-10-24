package com.nipu.wishfest.domain.models

data class CanvasModel(
    val width: Int = 1080,
    val height: Int = 1080,
    val backgroundColor: String = "#FFFFFF",
    val backgroundPhotoPath: String = "",
    val backgroundPhotoRotation: Float = 0f,
    val borderStyle: String = "none",
    val borderColor: String = "#E5A93C"
)

enum class LayerType {
    TEXT,
    IMAGE
}

data class TransformModel(
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 200f,
    val height: Float = 100f,
    val rotation: Float = 0f
)

sealed class LayerModel {
    abstract val id: String
    abstract val type: LayerType
    abstract val transform: TransformModel
    abstract val opacity: Float
    abstract val visible: Boolean
    abstract val locked: Boolean

    data class Text(
        override val id: String,
        override val transform: TransformModel,
        val text: String,
        val fontId: String = "default",
        val fontSize: Float = 48f,
        val color: String = "#000000",
        val alignment: String = "center",
        override val opacity: Float = 1f,
        override val visible: Boolean = true,
        override val locked: Boolean = false
    ) : LayerModel() {
        override val type: LayerType = LayerType.TEXT
    }

    data class Image(
        override val id: String,
        override val transform: TransformModel,
        val assetPath: String,
        override val opacity: Float = 1f,
        override val visible: Boolean = true,
        override val locked: Boolean = false
    ) : LayerModel() {
        override val type: LayerType = LayerType.IMAGE
    }
}

data class ProjectModel(
    val id: String,
    val templateId: String,
    val title: String = "Greeting Card",
    val canvas: CanvasModel,
    val layers: List<LayerModel> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis()
)
