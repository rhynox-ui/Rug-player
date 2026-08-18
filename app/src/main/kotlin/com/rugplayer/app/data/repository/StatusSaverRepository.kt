package com.rugplayer.app.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class StatusItem(
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val lastModified: Long,
) {
    val isVideo: Boolean get() = mimeType.startsWith("video/")
}

/**
 * Lists and saves WhatsApp (or WhatsApp Business) status media from a
 * user-granted SAF tree. Rug Player never touches this folder without an
 * explicit folder pick — there's no broad storage permission involved.
 */
class StatusSaverRepository(private val context: Context) {

    fun listStatuses(treeUri: Uri): List<StatusItem> {
        val tree = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        return tree.listFiles()
            .filter { it.isFile && it.type != null && (it.type!!.startsWith("video/") || it.type!!.startsWith("image/")) }
            .map {
                StatusItem(
                    uri = it.uri,
                    name = it.name ?: "status",
                    mimeType = it.type ?: "application/octet-stream",
                    sizeBytes = it.length(),
                    lastModified = it.lastModified(),
                )
            }
            .sortedByDescending { it.lastModified }
    }

    suspend fun saveStatus(item: StatusItem): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val targetUri = createMediaStoreEntry(item)
                ?: error("Could not create a library entry")

            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                context.contentResolver.openInputStream(item.uri)?.use { input ->
                    input.copyTo(out)
                } ?: error("Could not read the status file")
            } ?: error("Could not open the destination file")

            finalizeMediaStoreEntry(targetUri)
            targetUri
        }
    }

    private fun createMediaStoreEntry(item: StatusItem): Uri? {
        val isVideo = item.isVideo
        val collection = if (isVideo) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }
        val relativeDir = if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES

        val values = ContentValues().apply {
            put(if (isVideo) MediaStore.Video.Media.DISPLAY_NAME else MediaStore.Images.Media.DISPLAY_NAME, item.name)
            put(if (isVideo) MediaStore.Video.Media.MIME_TYPE else MediaStore.Images.Media.MIME_TYPE, item.mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    if (isVideo) MediaStore.Video.Media.RELATIVE_PATH else MediaStore.Images.Media.RELATIVE_PATH,
                    "$relativeDir/RugPlayer Statuses",
                )
                put(if (isVideo) MediaStore.Video.Media.IS_PENDING else MediaStore.Images.Media.IS_PENDING, 1)
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(relativeDir), "RugPlayer Statuses")
                if (!dir.exists()) dir.mkdirs()
                put(if (isVideo) MediaStore.Video.Media.DATA else MediaStore.Images.Media.DATA, File(dir, item.name).absolutePath)
            }
        }
        return context.contentResolver.insert(collection, values)
    }

    private fun finalizeMediaStoreEntry(uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.IS_PENDING, 0)
            }
            context.contentResolver.update(uri, values, null, null)
        }
    }
}
