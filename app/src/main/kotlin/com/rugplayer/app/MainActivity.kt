package com.rugplayer.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.rugplayer.app.data.prefs.AppSettings
import com.rugplayer.app.data.prefs.ThemeMode
import com.rugplayer.app.ui.RugNavHost
import com.rugplayer.app.ui.theme.RugPlayerTheme

class MainActivity : ComponentActivity() {

    companion object {
        /** Action on the now-playing notification's tap intent. */
        const val ACTION_OPEN_NOW_PLAYING = "com.rugplayer.app.ACTION_OPEN_NOW_PLAYING"
    }

    private var openNowPlayingSignal by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (intent?.action == ACTION_OPEN_NOW_PLAYING) openNowPlayingSignal++

        val graph = (application as RugPlayerApp).graph

        setContent {
            val settings by graph.settingsRepository.settings.collectAsState(initial = AppSettings())
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (settings.theme) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }

            RugPlayerTheme(darkTheme = darkTheme) {
                RugNavHost(graph = graph, openNowPlayingSignal = openNowPlayingSignal)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == ACTION_OPEN_NOW_PLAYING) openNowPlayingSignal++
    }
}
