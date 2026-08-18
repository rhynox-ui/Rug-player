package com.rugplayer.app

import android.content.Context
import com.rugplayer.app.data.db.AppDatabase
import com.rugplayer.app.data.prefs.SettingsRepository
import com.rugplayer.app.data.repository.StatusSaverRepository
import com.rugplayer.app.data.repository.VideoRepository
import com.rugplayer.app.player.PlaybackController
import com.rugplayer.app.selection.SelectionController

/**
 * Small hand-rolled service locator. The app is intentionally light on
 * dependencies, so a DI framework would be more ceremony than it's worth.
 */
class AppGraph(context: Context) {
    private val appContext = context.applicationContext

    val videoRepository: VideoRepository by lazy { VideoRepository(appContext) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }
    val database: AppDatabase by lazy { AppDatabase.get(appContext) }
    val playbackPositionDao get() = database.playbackPositionDao()
    val statusSaverRepository: StatusSaverRepository by lazy { StatusSaverRepository(appContext) }
    val playbackController: PlaybackController by lazy { PlaybackController(appContext) }
    val selectionController: SelectionController by lazy { SelectionController() }
}
