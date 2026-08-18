package com.rugplayer.app.thumbnail

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Generates video thumbnails via the same OS-level APIs the system Gallery
 * and Files apps use ([ContentResolver.loadThumbnail] / the legacy
 * [MediaStore.Video.Thumbnails]), instead of decoding a frame ourselves.
 * This is far more resilient across devices than manual frame decoding —
 * it works even when a device's video codec setup makes direct frame
 * extraction unreliable.
 */
class VideoThumbnailFetcher(
    private val context: Context,
    private val uri: Uri,
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        val bitmap = loadThumbnailBitmap() ?: return@withContext null
        DrawableResult(
            drawable = BitmapDrawable(context.resources, bitmap),
            isSampled = true,
            dataSource = DataSource.DISK,
        )
    }

    private fun loadThumbnailBitmap(): Bitmap? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver.loadThumbnail(uri, Size(640, 360), null)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Video.Thumbnails.getThumbnail(
                context.contentResolver,
                ContentUris.parseId(uri),
                MediaStore.Video.Thumbnails.MINI_KIND,
                null,
            )
        }
    }.getOrNull()

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            val mimeType = runCatching { context.contentResolver.getType(data) }.getOrNull()
            if (mimeType == null || !mimeType.startsWith("video/")) return null
            return VideoThumbnailFetcher(context, data)
        }
    }
}
