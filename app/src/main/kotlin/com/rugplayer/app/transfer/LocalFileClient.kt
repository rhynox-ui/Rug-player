package com.rugplayer.app.transfer

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder

data class TransferProgress(val bytesRead: Long, val totalBytes: Long) {
    val fraction: Float get() = if (totalBytes > 0) (bytesRead.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
}

/**
 * Pulls a file from another phone's [LocalFileServer] straight into the
 * shared video library — no intermediate server, no account.
 */
object LocalFileClient {

    suspend fun download(
        context: Context,
        urlString: String,
        onProgress: (TransferProgress) -> Unit,
    ): Result<Uri> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 20_000
                requestMethod = "GET"
            }
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(IOException("Server responded ${connection.responseCode}"))
            }

            val totalBytes = connection.contentLengthLong
            val fileName = extractFileName(connection, url)

            val targetUri = createMediaStoreEntry(context, fileName)
                ?: return@withContext Result.failure(IOException("Could not create a library entry"))

            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                connection.inputStream.use { input ->
                    val buffer = ByteArray(64 * 1024)
                    var readTotal = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        out.write(buffer, 0, read)
                        readTotal += read
                        onProgress(TransferProgress(readTotal, totalBytes))
                    }
                }
            } ?: return@withContext Result.failure(IOException("Could not open the destination file"))

            finalizeMediaStoreEntry(context, targetUri)
            Result.success(targetUri)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    private fun extractFileName(connection: HttpURLConnection, url: URL): String {
        val disposition = connection.getHeaderField("Content-Disposition")
        val fromHeader = disposition
            ?.substringAfter("filename=\"", "")
            ?.substringBefore("\"", "")
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrDefault(it) }
        return fromHeader ?: url.path.substringAfterLast('/').ifBlank {
            "rug_transfer_${System.currentTimeMillis()}.mp4"
        }
    }

    private fun createMediaStoreEntry(context: Context, fileName: String): Uri? {
        val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/*")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/RugPlayer")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            } else {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                    "RugPlayer",
                )
                if (!dir.exists()) dir.mkdirs()
                put(MediaStore.Video.Media.DATA, File(dir, fileName).absolutePath)
            }
        }
        return context.contentResolver.insert(collection, values)
    }

    private fun finalizeMediaStoreEntry(context: Context, uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }
            context.contentResolver.update(uri, values, null, null)
        }
    }
}
