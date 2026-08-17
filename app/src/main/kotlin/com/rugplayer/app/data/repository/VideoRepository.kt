package com.rugplayer.app.data.repository

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.rugplayer.app.data.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

/**
 * Reads the on-device video library straight from MediaStore. Rug Player never
 * copies, uploads, or indexes this data anywhere off the device.
 */
class VideoRepository(private val context: Context) {

    private val collection: Uri =
        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

    private val projection = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.TITLE,
        MediaStore.Video.Media.DURATION,
        MediaStore.Video.Media.SIZE,
        MediaStore.Video.Media.DATE_ADDED,
        MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
        MediaStore.Video.Media.MIME_TYPE,
    )

    fun observeVideos(): Flow<List<VideoItem>> = callbackFlow {
        trySend(queryVideos())

        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                trySend(queryVideos())
            }
        }
        context.contentResolver.registerContentObserver(collection, true, observer)
        awaitClose { context.contentResolver.unregisterContentObserver(observer) }
    }

    suspend fun queryVideosOnce(): List<VideoItem> = withContext(Dispatchers.IO) { queryVideos() }

    private fun queryVideos(): List<VideoItem> {
        val items = mutableListOf<VideoItem>()
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)
                items += VideoItem(
                    id = id,
                    uri = uri,
                    title = cursor.getString(titleCol) ?: cursor.getString(nameCol) ?: "Untitled",
                    displayName = cursor.getString(nameCol) ?: "Untitled",
                    durationMs = cursor.getLong(durationCol),
                    sizeBytes = cursor.getLong(sizeCol),
                    dateAddedSec = cursor.getLong(dateCol),
                    folder = cursor.getString(bucketCol) ?: "Unknown",
                    mimeType = cursor.getString(mimeCol) ?: "video/*",
                )
            }
        }
        return items
    }
}
