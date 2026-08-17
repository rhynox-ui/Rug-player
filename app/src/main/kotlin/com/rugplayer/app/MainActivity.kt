package com.rugplayer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.rugplayer.app.data.prefs.AppSettings
import com.rugplayer.app.data.prefs.ThemeMode
import com.rugplayer.app.ui.RugNavHost
import com.rugplayer.app.ui.theme.RugPlayerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
                RugNavHost(graph = graph)
            }
        }
    }
}
