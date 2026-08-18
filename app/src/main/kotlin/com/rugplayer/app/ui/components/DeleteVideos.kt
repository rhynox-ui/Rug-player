package com.rugplayer.app.ui.components

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
 * Deletes videos from the shared library. On Android 11+ this hands off to
 * the system's own "delete these files?" confirmation via
 * [MediaStore.createDeleteRequest]; on older versions it deletes directly
 * (items Rug Player doesn't own may silently fail to delete there).
 */
@Composable
fun rememberVideoDeleter(onFinished: (deleted: Boolean) -> Unit): (List<VideoItem>) -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        onFinished(result.resultCode == android.app.Activity.RESULT_OK)
    }

    return remember(context) {
        { videos: List<VideoItem> ->
            val uris = videos.map { it.uri }
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
