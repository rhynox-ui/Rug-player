package com.rugplayer.app.player

import android.content.Context
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class NowPlaying(
    val videoId: Long? = null,
    val streamUrl: String? = null,
    val title: String = "",
    val isPlaying: Boolean = false,
)

/**
 * Owns the single MediaController connection to [PlaybackService] for the
 * app's whole lifetime. Player screens attach to and detach from it as they
 * come and go, but the connection — and playback — outlives any one screen.
 * That's what lets a video keep playing after the user backs out to the
 * library, and be resumed from the mini player or the notification instead
 * of restarting from scratch.
 */
class PlaybackController(private val appContext: Context) {

    var controller: MediaController? = null
        private set

    private val connectMutex = Mutex()

    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _nowPlaying.update { it?.copy(isPlaying = isPlaying) }
        }
    }

    suspend fun connect(): MediaController {
        controller?.let { return it }
        return connectMutex.withLock {
            controller ?: connectMediaController(appContext).also {
                it.addListener(listener)
                controller = it
            }
        }
    }

    fun setNowPlaying(videoId: Long?, streamUrl: String?, title: String) {
        _nowPlaying.value = NowPlaying(
            videoId = videoId,
            streamUrl = streamUrl,
            title = title,
            isPlaying = controller?.isPlaying ?: false,
        )
    }

    /** Stops playback entirely and hides the mini player / now-playing state. */
    fun stopAndClear() {
        controller?.apply {
            pause()
            stop()
            clearMediaItems()
        }
        _nowPlaying.value = null
    }
}
