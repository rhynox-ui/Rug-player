package com.rugplayer.app.ui.components

import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.rugplayer.app.data.model.VideoItem

/**
 * Deletes videos from the shared library by URI. On Android 11+ this hands
 * off to the system's own "delete these files?" confirmation via
 * [MediaStore.createDeleteRequest]; on older versions it deletes directly
 * (items Rug Player doesn't own may silently fail to delete there). URI-based
 * so a cross-folder multi-select can delete without needing every VideoItem
 * loaded at once.
 */
@Composable
fun rememberUriDeleter(onFinished: (deleted: Boolean) -> Unit): (List<Uri>) -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        onFinished(result.resultCode == android.app.Activity.RESULT_OK)
    }

    return remember(context) {
        { uris: List<Uri> ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val sender = MediaStore.createDeleteRequest(context.contentResolver, uris).intentSender
                launcher.launch(IntentSenderRequest.Builder(sender).build())
            } else {
                var allDeleted = true
                for (uri in uris) {
                    val rows = runCatching { context.contentResolver.delete(uri, null, null) }.getOrDefault(0)
                    if (rows <= 0) allDeleted = false
                }
                onFinished(allDeleted)
            }
        }
    }
}

@Composable
fun rememberVideoDeleter(onFinished: (deleted: Boolean) -> Unit): (List<VideoItem>) -> Unit {
    val deleteByUri = rememberUriDeleter(onFinished)
    return { videos -> deleteByUri(videos.map { it.uri }) }
}
