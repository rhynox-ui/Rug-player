package com.rugplayer.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rugplayer.app.data.prefs.AppSettings
import com.rugplayer.app.data.prefs.SettingsRepository
import com.rugplayer.app.data.prefs.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AppSettings(),
    )

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { repository.setTheme(mode) }
    fun setRememberPosition(enabled: Boolean) = viewModelScope.launch { repository.setRememberPosition(enabled) }
    fun setGestureSeek(enabled: Boolean) = viewModelScope.launch { repository.setGestureSeek(enabled) }
    fun setGestureVolumeBrightness(enabled: Boolean) =
        viewModelScope.launch { repository.setGestureVolumeBrightness(enabled) }
    fun setBackgroundPlayback(enabled: Boolean) = viewModelScope.launch { repository.setBackgroundPlayback(enabled) }
}
