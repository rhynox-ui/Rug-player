package com.rugplayer.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "rug_player_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val rememberPosition: Boolean = true,
    val gestureSeek: Boolean = true,
    val gestureVolumeBrightness: Boolean = true,
    val defaultSpeed: Float = 1f,
    val subtitleFontScale: Float = 1f,
    val subtitleDelayMs: Int = 0,
    val backgroundPlayback: Boolean = true,
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val REMEMBER_POSITION = booleanPreferencesKey("remember_position")
        val GESTURE_SEEK = booleanPreferencesKey("gesture_seek")
        val GESTURE_VOLUME_BRIGHTNESS = booleanPreferencesKey("gesture_volume_brightness")
        val DEFAULT_SPEED = floatPreferencesKey("default_speed")
        val SUBTITLE_FONT_SCALE = floatPreferencesKey("subtitle_font_scale")
        val SUBTITLE_DELAY_MS = intPreferencesKey("subtitle_delay_ms")
        val BACKGROUND_PLAYBACK = booleanPreferencesKey("background_playback")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            theme = prefs[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            rememberPosition = prefs[Keys.REMEMBER_POSITION] ?: true,
            gestureSeek = prefs[Keys.GESTURE_SEEK] ?: true,
            gestureVolumeBrightness = prefs[Keys.GESTURE_VOLUME_BRIGHTNESS] ?: true,
            defaultSpeed = prefs[Keys.DEFAULT_SPEED] ?: 1f,
            subtitleFontScale = prefs[Keys.SUBTITLE_FONT_SCALE] ?: 1f,
            subtitleDelayMs = prefs[Keys.SUBTITLE_DELAY_MS] ?: 0,
            backgroundPlayback = prefs[Keys.BACKGROUND_PLAYBACK] ?: true,
        )
    }

    suspend fun setTheme(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME] = mode.name }
    }

    suspend fun setRememberPosition(enabled: Boolean) {
        context.dataStore.edit { it[Keys.REMEMBER_POSITION] = enabled }
    }

    suspend fun setGestureSeek(enabled: Boolean) {
        context.dataStore.edit { it[Keys.GESTURE_SEEK] = enabled }
    }

    suspend fun setGestureVolumeBrightness(enabled: Boolean) {
        context.dataStore.edit { it[Keys.GESTURE_VOLUME_BRIGHTNESS] = enabled }
    }

    suspend fun setDefaultSpeed(speed: Float) {
        context.dataStore.edit { it[Keys.DEFAULT_SPEED] = speed }
    }

    suspend fun setSubtitleFontScale(scale: Float) {
        context.dataStore.edit { it[Keys.SUBTITLE_FONT_SCALE] = scale }
    }

    suspend fun setSubtitleDelayMs(delayMs: Int) {
        context.dataStore.edit { it[Keys.SUBTITLE_DELAY_MS] = delayMs }
    }

    suspend fun setBackgroundPlayback(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BACKGROUND_PLAYBACK] = enabled }
    }
}
