package com.rugplayer.app

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.decode.VideoFrameDecoder

class RugPlayerApp : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)

        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .components { add(VideoFrameDecoder.Factory()) }
                .build()
        )
    }
}
