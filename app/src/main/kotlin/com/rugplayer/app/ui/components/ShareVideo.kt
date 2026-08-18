package com.rugplayer.app.ui.components

import android.content.Context
import android.content.Intent
import com.rugplayer.app.data.model.VideoItem

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
