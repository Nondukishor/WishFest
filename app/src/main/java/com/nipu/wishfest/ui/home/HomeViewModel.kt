package com.nipu.wishfest.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nipu.wishfest.domain.models.CanvasModel
import com.nipu.wishfest.domain.models.LayerModel
import com.nipu.wishfest.domain.models.ProjectModel
import com.nipu.wishfest.domain.models.TransformModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.InputStream
import java.util.UUID

data class TemplateItem(
    val id: String,
    val title: String,
    val backgroundColor: String,
    val layers: List<LayerModel>,
    val borderStyle: String = "none",
    val borderColor: String = "#E5A93C"
) {
    val canvas: CanvasModel
        get() = CanvasModel(backgroundColor = backgroundColor, borderStyle = borderStyle, borderColor = borderColor)
}

data class FestivalItem(
    val id: String,
    val name: String,
    val tagline: String,
    val category: String,
    val icon: String,
    val templates: List<TemplateItem>
)

data class HomeUiState(
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val festivals: List<FestivalItem> = emptyList(),
    val filteredFestivals: List<FestivalItem> = emptyList(),
    val categories: List<String> = listOf(
        "All", "Wedding", "Sunday", "New Year", "Hindu", "Islamic",
        "Christian", "Sikh", "Buddhist", "Jewish", "Cultural"
    )
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadCatalog()
    }

    private fun loadCatalog() {
        viewModelScope.launch {
            try {
                val inputStream: InputStream = getApplication<Application>().assets.open("festivals.json")
                val jsonStr = inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(jsonStr)
                val festArr = root.getJSONArray("festivals")

                val festivalList = mutableListOf<FestivalItem>()
                for (i in 0 until festArr.length()) {
                    val festObj = festArr.getJSONObject(i)
                    val id = festObj.getString("id")
                    val name = festObj.getString("name")
                    val tagline = festObj.optString("tagline", "")
                    val category = festObj.optString("category", "General")
                    val icon = festObj.optString("icon", "🎉")

                    val templatesArr = festObj.optJSONArray("templates")
                    val templateList = mutableListOf<TemplateItem>()
                    if (templatesArr != null) {
                        for (j in 0 until templatesArr.length()) {
                            val tObj = templatesArr.getJSONObject(j)
                            val tId = tObj.getString("id")
                            val tTitle = tObj.getString("title")
                            val tBgColor = tObj.optString("backgroundColor", "#FFFFFF")

                            val layersArr = tObj.optJSONArray("layers")
                            val layers = mutableListOf<LayerModel>()
                            if (layersArr != null) {
                                for (k in 0 until layersArr.length()) {
                                    val lObj = layersArr.getJSONObject(k)
                                    val lId = lObj.optString("id", UUID.randomUUID().toString())
                                    val type = lObj.optString("type", "text")
                                    val text = lObj.optString("text", "")
                                    val fontId = lObj.optString("fontId", "default")
                                    val fontSize = lObj.optDouble("fontSize", 48.0).toFloat()
                                    val color = lObj.optString("color", "#000000")
                                    val alignment = lObj.optString("alignment", "center")

                                    val tTransform = lObj.optJSONObject("transform")
                                    val transform = if (tTransform != null) {
                                        TransformModel(
                                            x = tTransform.optDouble("x", 0.0).toFloat(),
                                            y = tTransform.optDouble("y", 0.0).toFloat(),
                                            width = tTransform.optDouble("width", 200.0).toFloat(),
                                            height = tTransform.optDouble("height", 100.0).toFloat(),
                                            rotation = tTransform.optDouble("rotation", 0.0).toFloat()
                                        )
                                    } else TransformModel()

                                    if (type == "text") {
                                        layers.add(
                                            LayerModel.Text(
                                                id = lId,
                                                transform = transform,
                                                text = text,
                                                fontId = fontId,
                                                fontSize = fontSize,
                                                color = color,
                                                alignment = alignment
                                            )
                                        )
                                    }
                                }
                            }
                            templateList.add(TemplateItem(
                                id = tId, title = tTitle, backgroundColor = tBgColor, layers = layers,
                                borderStyle = tObj.optString("borderStyle", "none"),
                                borderColor = tObj.optString("borderColor", "#E5A93C")
                            ))
                        }
                    }
                    festivalList.add(FestivalItem(id, name, tagline, category, icon, templateList))
                }

                _uiState.value = _uiState.value.copy(
                    festivals = festivalList,
                    filteredFestivals = festivalList
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters()
    }

    fun onCategorySelected(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        applyFilters()
    }

    private fun applyFilters() {
        val query = _uiState.value.searchQuery.lowercase()
        val category = _uiState.value.selectedCategory

        val filtered = _uiState.value.festivals.filter { fest ->
            val matchesCat = (category == "All" || fest.category.equals(category, ignoreCase = true))
            val matchesQuery = query.isEmpty() ||
                    fest.name.lowercase().contains(query) ||
                    fest.tagline.lowercase().contains(query)
            matchesCat && matchesQuery
        }

        _uiState.value = _uiState.value.copy(filteredFestivals = filtered)
    }

    fun createProjectFromTemplate(template: TemplateItem): ProjectModel {
        val newId = UUID.randomUUID().toString()
        return ProjectModel(
            id = newId,
            templateId = template.id,
            title = template.title,
            canvas = template.canvas,
            layers = template.layers,
            updatedAt = System.currentTimeMillis()
        )
    }
}
