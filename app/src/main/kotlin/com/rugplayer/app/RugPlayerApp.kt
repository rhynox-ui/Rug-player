package com.rugplayer.app

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import com.rugplayer.app.thumbnail.VideoThumbnailFetcher

class RugPlayerApp : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)

        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .components {
                    // Primary: the OS's own thumbnail generator (same one Files/
                    // Gallery use) — far more resilient than decoding a frame
                    // ourselves. Falls through to manual frame decoding below
                    // only if that's unavailable for a given file.
                    add(VideoThumbnailFetcher.Factory(this@RugPlayerApp))
                    add(VideoFrameDecoder.Factory())
                }
                .build()
        )
    }
}
