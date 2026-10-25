package com.nipu.wishfest.platform

import android.content.Context
import android.util.AtomicFile
import com.nipu.wishfest.domain.models.CanvasModel
import com.nipu.wishfest.domain.models.LayerModel
import com.nipu.wishfest.domain.models.ProjectModel
import com.nipu.wishfest.domain.models.TransformModel
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ProjectRepository(private val context: Context) {

    private val projectsDir: File
        get() = File(context.filesDir, "projects").apply { if (!exists()) mkdirs() }

    fun getProjectFile(projectId: String): File {
        val projFolder = File(projectsDir, projectId).apply { if (!exists()) mkdirs() }
        return File(projFolder, "document.json")
    }

    fun saveProject(project: ProjectModel) {
        val file = getProjectFile(project.id)
        val jsonString = serializeProjectToJson(project)

        val atomicFile = AtomicFile(file)
        var stream: FileOutputStream? = null
        try {
            stream = atomicFile.startWrite()
            stream.write(jsonString.toByteArray(Charsets.UTF_8))
            atomicFile.finishWrite(stream)
        } catch (e: Exception) {
            e.printStackTrace()
            if (stream != null) {
                atomicFile.failWrite(stream)
            }
        }
    }

    fun loadProject(projectId: String): ProjectModel? {
        val file = getProjectFile(projectId)
        if (!file.exists()) return null
        return try {
            val jsonStr = file.readText(Charsets.UTF_8)
            deserializeJsonToProject(jsonStr)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun listAllProjects(): List<ProjectModel> {
        val list = mutableListOf<ProjectModel>()
        val folders = projectsDir.listFiles() ?: return emptyList()
        for (folder in folders) {
            if (folder.isDirectory) {
                val docFile = File(folder, "document.json")
                if (docFile.exists()) {
                    try {
                        val jsonStr = docFile.readText(Charsets.UTF_8)
                        val proj = deserializeJsonToProject(jsonStr)
                        list.add(proj)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        return list.sortedByDescending { it.updatedAt }
    }

    fun deleteProject(projectId: String): Boolean {
        val folder = File(projectsDir, projectId)
        return folder.deleteRecursively()
    }

    fun duplicateProject(projectId: String): ProjectModel? {
        val original = loadProject(projectId) ?: return null
        val newId = UUID.randomUUID().toString()
        val duplicated = original.copy(
            id = newId,
            title = if (original.title.contains("Copy")) "${original.title} 2" else "${original.title} Copy",
            updatedAt = System.currentTimeMillis()
        )
        saveProject(duplicated)
        return duplicated
    }

    fun serializeProjectToJson(project: ProjectModel): String {
        val jsonObj = JSONObject()
        jsonObj.put("schemaVersion", 1)
        jsonObj.put("id", project.id)
        jsonObj.put("templateId", project.templateId)
        jsonObj.put("title", project.title)
        jsonObj.put("updatedAt", project.updatedAt)

        val canvasObj = JSONObject()
        canvasObj.put("width", project.canvas.width)
        canvasObj.put("height", project.canvas.height)
        canvasObj.put("backgroundColor", project.canvas.backgroundColor)
        canvasObj.put("backgroundPhotoPath", project.canvas.backgroundPhotoPath)
        canvasObj.put("backgroundPhotoRotation", project.canvas.backgroundPhotoRotation)
        canvasObj.put("borderStyle", project.canvas.borderStyle)
        canvasObj.put("borderColor", project.canvas.borderColor)
        jsonObj.put("canvas", canvasObj)

        val layersArr = JSONArray()
        for (layer in project.layers) {
            val layerObj = JSONObject()
            layerObj.put("id", layer.id)
            layerObj.put("type", if (layer is LayerModel.Text) "text" else "image")
            layerObj.put("opacity", layer.opacity)
            layerObj.put("visible", layer.visible)
            layerObj.put("locked", layer.locked)

            val transformObj = JSONObject()
            transformObj.put("x", layer.transform.x)
            transformObj.put("y", layer.transform.y)
            transformObj.put("width", layer.transform.width)
            transformObj.put("height", layer.transform.height)
            transformObj.put("rotation", layer.transform.rotation)
            layerObj.put("transform", transformObj)

            if (layer is LayerModel.Text) {
                layerObj.put("text", layer.text)
                layerObj.put("fontId", layer.fontId)
                layerObj.put("fontSize", layer.fontSize)
                layerObj.put("color", layer.color)
                layerObj.put("alignment", layer.alignment)
            } else if (layer is LayerModel.Image) {
                layerObj.put("assetPath", layer.assetPath)
                layerObj.put("tintColor", layer.tintColor)
            }
            layersArr.put(layerObj)
        }
        jsonObj.put("layers", layersArr)

        return jsonObj.toString(2)
    }

    companion object {
        fun deserializeJsonToProject(jsonStr: String): ProjectModel {
            val jsonObj = JSONObject(jsonStr)
            val id = jsonObj.optString("id", UUID.randomUUID().toString())
            val templateId = jsonObj.optString("templateId", "default")
            val title = jsonObj.optString("title", "Greeting Card")
            val updatedAt = jsonObj.optLong("updatedAt", System.currentTimeMillis())

            val canvasObj = jsonObj.optJSONObject("canvas")
            val canvas = if (canvasObj != null) {
                CanvasModel(
                    width = canvasObj.optInt("width", 1080),
                    height = canvasObj.optInt("height", 1080),
                    backgroundColor = canvasObj.optString("backgroundColor", "#FFFFFF"),
                    backgroundPhotoPath = canvasObj.optString("backgroundPhotoPath", ""),
                    backgroundPhotoRotation = canvasObj.optDouble("backgroundPhotoRotation", 0.0).toFloat(),
                    borderStyle = canvasObj.optString("borderStyle", "none"),
                    borderColor = canvasObj.optString("borderColor", "#E5A93C")
                )
            } else CanvasModel()

            val layers = mutableListOf<LayerModel>()
            val layersArr = jsonObj.optJSONArray("layers") ?: JSONArray()
            for (i in 0 until layersArr.length()) {
                val layerObj = layersArr.getJSONObject(i)
                val layerId = layerObj.optString("id", UUID.randomUUID().toString())
                val type = layerObj.optString("type", "text")

                val tObj = layerObj.optJSONObject("transform")
                val transform = if (tObj != null) {
                    TransformModel(
                        x = tObj.optDouble("x", 0.0).toFloat(),
                        y = tObj.optDouble("y", 0.0).toFloat(),
                        width = tObj.optDouble("width", 200.0).toFloat(),
                        height = tObj.optDouble("height", 100.0).toFloat(),
                        rotation = tObj.optDouble("rotation", 0.0).toFloat()
                    )
                } else TransformModel()

                val opacity = layerObj.optDouble("opacity", 1.0).toFloat()
                val visible = layerObj.optBoolean("visible", true)
                val locked = layerObj.optBoolean("locked", false)

                if (type == "text") {
                    layers.add(
                        LayerModel.Text(
                            id = layerId,
                            transform = transform,
                            text = layerObj.optString("text", ""),
                            fontId = layerObj.optString("fontId", "default"),
                            fontSize = layerObj.optDouble("fontSize", 48.0).toFloat(),
                            color = layerObj.optString("color", "#000000"),
                            alignment = layerObj.optString("alignment", "center"),
                            opacity = opacity,
                            visible = visible,
                            locked = locked
                        )
                    )
                } else if (type == "image") {
                    layers.add(
                        LayerModel.Image(
                            id = layerId,
                            transform = transform,
                            assetPath = layerObj.optString("assetPath", ""),
                            tintColor = layerObj.optString("tintColor", ""),
                            opacity = opacity,
                            visible = visible,
                            locked = locked
                        )
                    )
                }
            }

            return ProjectModel(
                id = id,
                templateId = templateId,
                title = title,
                canvas = canvas,
                layers = layers,
                updatedAt = updatedAt
            )
        }
    }

    fun deserializeJsonToProject(jsonStr: String): ProjectModel {
        return Companion.deserializeJsonToProject(jsonStr)
    }
}
