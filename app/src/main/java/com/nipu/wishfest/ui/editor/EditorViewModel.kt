package com.nipu.wishfest.ui.editor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.nipu.wishfest.domain.models.LayerModel
import com.nipu.wishfest.domain.models.ProjectModel
import com.nipu.wishfest.domain.models.TransformModel
import com.nipu.wishfest.native.NativeEngineBridge
import com.nipu.wishfest.platform.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class EditorUiState(
    val project: ProjectModel? = null,
    val selectedLayerId: String? = null,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isSaving: Boolean = false
)

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepository(application)
    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val undoStack = java.util.ArrayDeque<ProjectModel>()
    private val redoStack = java.util.ArrayDeque<ProjectModel>()
    private var nativeHandle: Long = 0

    fun initProject(project: ProjectModel) {
        if (_uiState.value.project?.id == project.id) return
        if (nativeHandle != 0L) {
            NativeEngineBridge.nDestroyDocument(nativeHandle)
        }

        nativeHandle = NativeEngineBridge.nCreateDocument(
            project.id,
            project.templateId,
            project.canvas.width,
            project.canvas.height,
            project.canvas.backgroundColor
        )

        undoStack.clear()
        redoStack.clear()
        _uiState.value = EditorUiState(
            project = project,
            selectedLayerId = project.layers.firstOrNull()?.id,
            canUndo = NativeEngineBridge.nCanUndo(nativeHandle),
            canRedo = NativeEngineBridge.nCanRedo(nativeHandle)
        )

        repository.saveProject(project)
    }

    fun selectLayer(layerId: String?) {
        _uiState.value = _uiState.value.copy(selectedLayerId = layerId)
    }

    fun replaceTextWithAiWish(generatedWish: String) {
        val currentProject = _uiState.value.project ?: return
        val selectedId = _uiState.value.selectedLayerId

        val dynamicFontSize = when {
            generatedWish.length > 120 -> 34f
            generatedWish.length > 70 -> 42f
            else -> 50f
        }

        val primaryLayerId = currentProject.layers.filterIsInstance<LayerModel.Text>().firstOrNull { it.id == selectedId && it.fontId != "sticker" }?.id
            ?: currentProject.layers.filterIsInstance<LayerModel.Text>().firstOrNull { it.fontId != "sticker" }?.id ?: UUID.randomUUID().toString()

        val primaryTextLayer = (currentProject.layers.find { it.id == primaryLayerId } as? LayerModel.Text)
            ?.copy(
                text = generatedWish,
                fontSize = dynamicFontSize,
                transform = TransformModel(x = 100f, y = 350f, width = 880f, height = 380f, rotation = 0f),
                alignment = "center"
            )
            ?: LayerModel.Text(
                id = primaryLayerId,
                transform = TransformModel(x = 100f, y = 350f, width = 880f, height = 380f, rotation = 0f),
                text = generatedWish,
                fontSize = dynamicFontSize,
                alignment = "center"
            )

        val nonTextLayers = currentProject.layers.filter { it !is LayerModel.Text || it.fontId == "sticker" }
        val updatedLayers = nonTextLayers + primaryTextLayer

        val updatedProject = currentProject.copy(
            layers = updatedLayers,
            updatedAt = System.currentTimeMillis()
        )

        updateAndSaveProject(updatedProject)
        selectLayer(primaryLayerId)
    }

    fun updateSelectedTextLayer(newText: String, color: String? = null, fontId: String? = null, fontSize: Float? = null, alignment: String? = null) {
        val currentProject = _uiState.value.project ?: return
        val layerId = _uiState.value.selectedLayerId ?: return

        val updatedLayers = currentProject.layers.map { layer ->
            if (layer.id == layerId && layer is LayerModel.Text) {
                layer.copy(
                    text = newText,
                    color = color ?: layer.color,
                    fontId = fontId ?: layer.fontId,
                    fontSize = fontSize ?: layer.fontSize,
                    alignment = alignment ?: layer.alignment
                )
            } else layer
        }

        val updatedProject = currentProject.copy(
            layers = updatedLayers,
            updatedAt = System.currentTimeMillis()
        )

        updateAndSaveProject(updatedProject)
    }

    fun updateLayerTransform(layerId: String, newTransform: TransformModel) {
        val currentProject = _uiState.value.project ?: return
        val updatedLayers = currentProject.layers.map { layer ->
            if (layer.id == layerId) {
                when (layer) {
                    is LayerModel.Text -> layer.copy(transform = newTransform)
                    is LayerModel.Image -> layer.copy(transform = newTransform)
                }
            } else layer
        }

        val updatedProject = currentProject.copy(
            layers = updatedLayers,
            updatedAt = System.currentTimeMillis()
        )

        if (nativeHandle != 0L) {
            NativeEngineBridge.nUpdateTransform(
                nativeHandle,
                layerId,
                newTransform.x,
                newTransform.y,
                newTransform.width,
                newTransform.height,
                newTransform.rotation
            )
        }

        updateAndSaveProject(updatedProject)
    }

    fun updateBackgroundColor(color: String) {
        val currentProject = _uiState.value.project ?: return
        val newCanvas = currentProject.canvas.copy(backgroundColor = color)
        val updatedProject = currentProject.copy(canvas = newCanvas, updatedAt = System.currentTimeMillis())
        updateAndSaveProject(updatedProject)
    }

    fun updateBackgroundPhoto(photoPath: String) {
        val currentProject = _uiState.value.project ?: return
        val newCanvas = currentProject.canvas.copy(backgroundPhotoPath = photoPath)
        val updatedProject = currentProject.copy(canvas = newCanvas, updatedAt = System.currentTimeMillis())
        updateAndSaveProject(updatedProject)
    }

    fun updateBackgroundPhotoRotation(rotationDegrees: Float) {
        val currentProject = _uiState.value.project ?: return
        val newCanvas = currentProject.canvas.copy(backgroundPhotoRotation = rotationDegrees)
        val updatedProject = currentProject.copy(canvas = newCanvas, updatedAt = System.currentTimeMillis())
        updateAndSaveProject(updatedProject)
    }

    fun rotateBackgroundPhoto90() {
        val currentProject = _uiState.value.project ?: return
        val newRotation = (currentProject.canvas.backgroundPhotoRotation + 90f) % 360f
        updateBackgroundPhotoRotation(newRotation)
    }

    fun addTextLayer(text: String = "Greeting Message") {
        val currentProject = _uiState.value.project ?: return
        val newId = UUID.randomUUID().toString()
        val newLayer = LayerModel.Text(
            id = newId,
            transform = TransformModel(x = 100f, y = 400f, width = 880f, height = 200f),
            text = text,
            fontId = "default",
            fontSize = 52f,
            color = "#000000",
            alignment = "center"
        )

        val updatedLayers = currentProject.layers + newLayer
        val updatedProject = currentProject.copy(layers = updatedLayers, updatedAt = System.currentTimeMillis())
        updateAndSaveProject(updatedProject)
        selectLayer(newId)
    }

    fun addSticker(emoji: String) {
        val project = _uiState.value.project ?: return
        val layer = LayerModel.Text(
            id = UUID.randomUUID().toString(),
            transform = TransformModel(x = project.canvas.width * .4f, y = project.canvas.height * .35f, width = 200f, height = 240f),
            text = emoji, fontId = "sticker", fontSize = 150f
        )
        updateAndSaveProject(project.copy(layers = project.layers + layer))
        selectLayer(layer.id)
    }

    fun applyCreativeStyle(name: String) {
        val project = _uiState.value.project ?: return
        val (background, frame, ink) = when (name) {
            "Blossom" -> Triple("#FFF1F2", "#DB7093", "#831843")
            "Midnight" -> Triple("gradient:#172554,#4C1D95", "#E5A93C", "#FFFFFF")
            else -> Triple("#FFFBEB", "#C18B37", "#713F12")
        }
        val frameStyle = if (name == "Blossom") "floral" else "double"
        updateAndSaveProject(project.copy(
            canvas = project.canvas.copy(backgroundColor = background, backgroundPhotoPath = "", borderStyle = frameStyle, borderColor = frame),
            layers = project.layers.map { if (it is LayerModel.Text && it.fontId != "sticker") it.copy(color = ink, fontId = "serif") else it }
        ))
    }

    fun updateBorder(style: String, color: String) {
        val project = _uiState.value.project ?: return
        updateAndSaveProject(project.copy(canvas = project.canvas.copy(borderStyle = style, borderColor = color)))
    }

    fun addImageLayer(assetPath: String) {
        val currentProject = _uiState.value.project ?: return
        val newId = UUID.randomUUID().toString()
        val imageSize = minOf(currentProject.canvas.width, currentProject.canvas.height) * .5f
        val newLayer = LayerModel.Image(
            id = newId,
            transform = TransformModel(
                x = (currentProject.canvas.width - imageSize) / 2,
                y = (currentProject.canvas.height - imageSize) / 2,
                width = imageSize, height = imageSize),
            assetPath = assetPath
        )

        val updatedLayers = currentProject.layers + newLayer
        val updatedProject = currentProject.copy(layers = updatedLayers, updatedAt = System.currentTimeMillis())
        updateAndSaveProject(updatedProject)
        selectLayer(newId)
    }

    fun deleteSelectedLayer() {
        val currentProject = _uiState.value.project ?: return
        val layerId = _uiState.value.selectedLayerId ?: return

        val updatedLayers = currentProject.layers.filter { it.id != layerId }
        val updatedProject = currentProject.copy(layers = updatedLayers, updatedAt = System.currentTimeMillis())

        if (nativeHandle != 0L) {
            NativeEngineBridge.nRemoveLayer(nativeHandle, layerId)
        }

        updateAndSaveProject(updatedProject)
        selectLayer(updatedLayers.firstOrNull()?.id)
    }

    fun undo() {
        val current = _uiState.value.project ?: return
        if (undoStack.isEmpty()) return
        redoStack.addLast(current)
        restore(undoStack.removeLast())
    }

    fun redo() {
        val current = _uiState.value.project ?: return
        if (redoStack.isEmpty()) return
        undoStack.addLast(current)
        restore(redoStack.removeLast())
    }

    private fun restore(project: ProjectModel) {
        val selected = _uiState.value.selectedLayerId?.takeIf { id -> project.layers.any { it.id == id } }
        _uiState.value = _uiState.value.copy(project = project, selectedLayerId = selected,
            canUndo = undoStack.isNotEmpty(), canRedo = redoStack.isNotEmpty())
        repository.saveProject(project)
    }

    private fun updateAndSaveProject(project: ProjectModel) {
        _uiState.value.project?.let { undoStack.addLast(it) }
        if (undoStack.size > 100) undoStack.removeFirst()
        redoStack.clear()
        restore(project.copy(updatedAt = System.currentTimeMillis()))
    }

    override fun onCleared() {
        super.onCleared()
        if (nativeHandle != 0L) {
            NativeEngineBridge.nDestroyDocument(nativeHandle)
            nativeHandle = 0L
        }
    }
}
