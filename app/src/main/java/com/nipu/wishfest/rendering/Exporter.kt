package com.nipu.wishfest.rendering

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.nipu.wishfest.domain.models.ProjectModel
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

enum class ExportPreset(val width: Int, val height: Int, val label: String) {
    SQUARE_1080(1080, 1080, "Square (1:1)"),
    PORTRAIT_1350(1080, 1350, "Portrait (4:5)"),
    STORY_1920(1080, 1920, "Story (9:16)")
}

class Exporter(private val context: Context) {

    private val renderer = SceneRenderer(context)

    fun renderExportBitmap(project: ProjectModel, preset: ExportPreset): Bitmap {
        return renderer.renderToBitmap(project, preset.width, preset.height)
    }

    fun saveToGallery(bitmap: Bitmap, filename: String = "WishFest_${System.currentTimeMillis()}"): Uri? {
        val imageCollection =
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$filename.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.IS_PENDING, 1)
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/WishFest")
        }

        val uri = context.contentResolver.insert(imageCollection, contentValues) ?: return null

        try {
            val outputStream: OutputStream? = context.contentResolver.openOutputStream(uri)
            if (outputStream != null) {
                outputStream.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                }
            }

            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, contentValues, null, null)
            return uri
        } catch (e: Exception) {
            e.printStackTrace()
            context.contentResolver.delete(uri, null, null)
            return null
        }
    }

    fun shareImage(bitmap: Bitmap, title: String = "Share Greeting Card") {
        val shareCacheDir = File(context.cacheDir, "share").apply { if (!exists()) mkdirs() }
        val shareFile = File(shareCacheDir, "wishfest_card_${System.currentTimeMillis()}.jpg")

        try {
            FileOutputStream(shareFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                shareFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Created with WishFest Greetings!")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
