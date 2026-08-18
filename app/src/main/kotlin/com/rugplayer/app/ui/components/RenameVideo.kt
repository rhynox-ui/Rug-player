package com.rugplayer.app.ui.components

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.rugplayer.app.data.model.VideoItem

/**
 * Renames a video's MediaStore display name. Newer Android versions can
 * refuse a direct write to media this app doesn't own; when that happens as
 * a [RecoverableSecurityException] we hand off to the system's own "allow
 * this app to modify this file?" dialog and retry once the user grants it.
 */
@Composable
fun rememberVideoRenamer(onFinished: (renamed: Boolean) -> Unit): (VideoItem, String) -> Unit {
    val context = LocalContext.current
    var pendingRename by remember { mutableStateOf<Pair<Uri, String>?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val pending = pendingRename
        pendingRename = null
        if (result.resultCode == Activity.RESULT_OK && pending != null) {
            val values = ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME, pending.second) }
            val ok = runCatching { context.contentResolver.update(pending.first, values, null, null) }
                .map { it > 0 }
                .getOrDefault(false)
            onFinished(ok)
        } else {
            onFinished(false)
        }
    }

    return remember(context) {
        { video: VideoItem, newName: String ->
            val values = ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME, newName) }
            try {
                val rows = context.contentResolver.update(video.uri, values, null, null)
                onFinished(rows > 0)
            } catch (e: RecoverableSecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    pendingRename = video.uri to newName
                    launcher.launch(IntentSenderRequest.Builder(e.userAction.actionIntent.intentSender).build())
                } else {
                    onFinished(false)
                }
            } catch (e: SecurityException) {
                onFinished(false)
            }
        }
    }
}
