package com.nipu.wishfest.ui.export

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import com.nipu.wishfest.domain.models.ProjectModel
import com.nipu.wishfest.rendering.ExportPreset
import com.nipu.wishfest.rendering.Exporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ExportUiState(
    val selectedPreset: ExportPreset = ExportPreset.SQUARE_1080,
    val previewBitmap: Bitmap? = null,
    val isExporting: Boolean = false,
    val savedUri: Uri? = null,
    val statusMessage: String? = null
)

class ExportViewModel(application: Application) : AndroidViewModel(application) {

    private val exporter = Exporter(application)
    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    fun updatePreset(project: ProjectModel, preset: ExportPreset) {
        val bitmap = exporter.renderExportBitmap(project, preset)
        _uiState.value = _uiState.value.copy(
            selectedPreset = preset,
            previewBitmap = bitmap,
            savedUri = null,
            statusMessage = null
        )
    }

    fun saveToGallery(project: ProjectModel) {
        val preset = _uiState.value.selectedPreset
        val bitmap = _uiState.value.previewBitmap ?: exporter.renderExportBitmap(project, preset)

        _uiState.value = _uiState.value.copy(isExporting = true)
        val uri = exporter.saveToGallery(bitmap, filename = "WishFest_${project.title.replace(" ", "_")}")
        _uiState.value = _uiState.value.copy(
            isExporting = false,
            savedUri = uri,
            statusMessage = if (uri != null) "Saved card to Pictures/WishFest!" else "Failed to save card."
        )
    }

    fun shareImage(project: ProjectModel) {
        val preset = _uiState.value.selectedPreset
        val bitmap = _uiState.value.previewBitmap ?: exporter.renderExportBitmap(project, preset)
        exporter.shareImage(bitmap, title = "Share ${project.title}")
    }
}
