package com.rugplayer.app.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun connectMediaController(context: Context): MediaController {
    val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
    val future = MediaController.Builder(context, token).buildAsync()
    return suspendCancellableCoroutine { continuation ->
        future.addListener(
            {
                try {
                    continuation.resume(future.get())
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            },
            MoreExecutors.directExecutor(),
        )
        continuation.invokeOnCancellation { future.cancel(false) }
    }
}
