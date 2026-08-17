package com.rugplayer.app.data.model

import android.net.Uri

data class VideoItem(
    val id: Long,
    val uri: Uri,
    val title: String,
    val displayName: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAddedSec: Long,
    val folder: String,
    val mimeType: String,
)
