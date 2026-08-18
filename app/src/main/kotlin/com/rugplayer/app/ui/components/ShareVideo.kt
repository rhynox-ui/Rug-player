package com.rugplayer.app.ui.components

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import com.rugplayer.app.data.model.VideoItem

/** The content Uri for a video's MediaStore row id — works without loading the full VideoItem. */
fun videoUri(id: Long): Uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

/** Hands off to Android's own share sheet, which lists every app that can take a video. */
fun shareVideo(context: Context, video: VideoItem) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = video.mimeType
        putExtra(Intent.EXTRA_STREAM, video.uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(sendIntent, video.title).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}

/** Same as [shareVideo] but for a multi-select spanning any number of videos, possibly across folders. */
fun shareVideos(context: Context, ids: Collection<Long>) {
    if (ids.isEmpty()) return
    val uris = ArrayList(ids.map(::videoUri))
    val sendIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "video/*"
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share ${ids.size} videos").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}
