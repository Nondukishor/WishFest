package com.nipu.wishfest.platform

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class AssetManager(private val context: Context) {

    fun copyUriToProjectAsset(projectId: String, sourceUri: Uri): String? {
        val projAssetsFolder = File(context.filesDir, "projects/$projectId/assets").apply {
            if (!exists()) mkdirs()
        }
        val filename = "photo_${UUID.randomUUID().toString().take(8)}.jpg"
        val destFile = File(projAssetsFolder, filename)

        return try {
            val inputStream: InputStream = context.contentResolver.openInputStream(sourceUri) ?: return null
            val outputStream = FileOutputStream(destFile)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun copyAssetToAppPrivate(assetPath: String, destFileName: String): String? {
        val destFile = File(context.filesDir, destFileName)
        if (destFile.exists()) return destFile.absolutePath

        return try {
            val inputStream = context.assets.open(assetPath)
            val outputStream = FileOutputStream(destFile)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
