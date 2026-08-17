package com.rugplayer.app.ui.player

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.MimeTypes
import androidx.media3.session.MediaController
import com.rugplayer.app.data.db.PlaybackPositionDao
import com.rugplayer.app.data.db.PlaybackPositionEntity
import com.rugplayer.app.data.model.VideoItem
import com.rugplayer.app.data.prefs.SettingsRepository
import com.rugplayer.app.data.repository.VideoRepository
import com.rugplayer.app.player.connectMediaController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SubtitleTrackUi(
    val id: String,
    val label: String,
    val groupIndex: Int,
    val trackIndex: Int,
)

data class PlayerUiState(
    val queue: List<VideoItem> = emptyList(),
    val currentIndex: Int = -1,
    val isReady: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val bufferedPositionMs: Long = 0,
    val speed: Float = 1f,
    val subtitleTracks: List<SubtitleTrackUi> = emptyList(),
    val selectedSubtitleId: String? = null,
    val locked: Boolean = false,
    val sleepTimerEndAtMs: Long? = null,
    val abRepeatStartMs: Long? = null,
    val abRepeatEndMs: Long? = null,
) {
    val current: VideoItem? get() = queue.getOrNull(currentIndex)
}

class PlayerViewModel(
    private val appContext: Context,
    private val videoRepository: VideoRepository,
    private val positionDao: PlaybackPositionDao,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    var controller: MediaController? = null
        private set

    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var savePositionJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _uiState.update { it.copy(isBuffering = playbackState == Player.STATE_BUFFERING) }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val newIndex = controller?.currentMediaItemIndex ?: return
            _uiState.update { it.copy(currentIndex = newIndex, positionMs = 0) }
            refreshSubtitleTracks()
        }

        override fun onTracksChanged(tracks: Tracks) {
            refreshSubtitleTracks()
        }

        override fun onPlaybackParametersChanged(playbackParameters: androidx.media3.common.PlaybackParameters) {
            _uiState.update { it.copy(speed = playbackParameters.speed) }
        }
    }

    fun start(initialVideoId: Long) {
        viewModelScope.launch {
            val allVideos = videoRepository.queryVideosOnce()
            val startIndex = allVideos.indexOfFirst { it.id == initialVideoId }.coerceAtLeast(0)
            _uiState.update { it.copy(queue = allVideos, currentIndex = startIndex) }

            val mediaController = connectMediaController(appContext)
            controller = mediaController
            mediaController.addListener(playerListener)

            val settings = settingsRepository.settings.first()
            val defaultSpeed = settings.defaultSpeed

            val mediaItems = allVideos.map { video -> buildMediaItem(video) }
            mediaController.setMediaItems(mediaItems, startIndex, C.TIME_UNSET)
            mediaController.playbackParameters = androidx.media3.common.PlaybackParameters(defaultSpeed)

            val savedPosition = positionDao.get(initialVideoId)
            if (settings.rememberPosition && savedPosition != null &&
                savedPosition.durationMs > 0 &&
                savedPosition.positionMs < savedPosition.durationMs * 95 / 100
            ) {
                mediaController.seekTo(startIndex, savedPosition.positionMs)
            }

            mediaController.prepare()
            mediaController.playWhenReady = true

            _uiState.update {
                it.copy(
                    isReady = true,
                    speed = defaultSpeed,
                    durationMs = mediaController.duration.coerceAtLeast(0),
                )
            }
            startProgressTicker()
        }
    }

    private fun buildMediaItem(video: VideoItem): MediaItem =
        MediaItem.Builder()
            .setUri(video.uri)
            .setMediaId(video.id.toString())
            .setMimeType(video.mimeType)
            .build()

    private fun refreshSubtitleTracks() {
        val mediaController = controller ?: return
        val tracks = mediaController.currentTracks
        val subtitleTracks = mutableListOf<SubtitleTrackUi>()
        tracks.groups.forEachIndexed { groupIndex, group ->
            if (group.type != C.TRACK_TYPE_TEXT) return@forEachIndexed
            for (trackIndex in 0 until group.length) {
                if (!group.isTrackSupported(trackIndex)) continue
                val format = group.getTrackFormat(trackIndex)
                val label = format.label ?: format.language?.uppercase() ?: "Track ${trackIndex + 1}"
                subtitleTracks += SubtitleTrackUi(
                    id = "$groupIndex-$trackIndex",
                    label = label,
                    groupIndex = groupIndex,
                    trackIndex = trackIndex,
                )
            }
        }
        val selectedGroup = tracks.groups.firstOrNull { it.type == C.TRACK_TYPE_TEXT && it.isSelected }
        val selectedIndex = selectedGroup?.let { g ->
            (0 until g.length).firstOrNull { g.isTrackSelected(it) }
        }
        val selectedId = if (selectedGroup != null && selectedIndex != null) {
            val groupIndex = tracks.groups.indexOf(selectedGroup)
            "$groupIndex-$selectedIndex"
        } else null

        _uiState.update { it.copy(subtitleTracks = subtitleTracks, selectedSubtitleId = selectedId) }
    }

    fun selectSubtitle(track: SubtitleTrackUi?) {
        val mediaController = controller ?: return
        val paramsBuilder = mediaController.trackSelectionParameters.buildUpon()
        if (track == null) {
            paramsBuilder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            paramsBuilder.clearOverridesOfType(C.TRACK_TYPE_TEXT)
        } else {
            val group = mediaController.currentTracks.groups.getOrNull(track.groupIndex) ?: return
            paramsBuilder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            paramsBuilder.setOverrideForType(
                TrackSelectionOverride(group.mediaTrackGroup, track.trackIndex),
            )
        }
        mediaController.trackSelectionParameters = paramsBuilder.build()
        _uiState.update { it.copy(selectedSubtitleId = track?.id) }
    }

    fun addExternalSubtitle(uri: Uri, displayName: String) {
        val mediaController = controller ?: return
        val currentItem = mediaController.currentMediaItem ?: return
        val mimeType = when {
            displayName.endsWith(".vtt", ignoreCase = true) -> MimeTypes.TEXT_VTT
            displayName.endsWith(".ssa", ignoreCase = true) || displayName.endsWith(".ass", ignoreCase = true) -> MimeTypes.TEXT_SSA
            else -> MimeTypes.APPLICATION_SUBRIP
        }
        val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(uri)
            .setMimeType(mimeType)
            .setLanguage("ext")
            .setLabel(displayName)
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()

        val existing = currentItem.localConfiguration?.subtitleConfigurations.orEmpty()
        val newItem = currentItem.buildUpon()
            .setSubtitleConfigurations(existing + subtitleConfig)
            .build()

        val resumeAt = mediaController.currentPosition
        val wasPlaying = mediaController.isPlaying
        val index = mediaController.currentMediaItemIndex

        mediaController.replaceMediaItem(index, newItem)
        mediaController.seekTo(index, resumeAt)
        mediaController.prepare()
        mediaController.playWhenReady = wasPlaying
    }

    fun togglePlayPause() {
        val mediaController = controller ?: return
        if (mediaController.isPlaying) mediaController.pause() else mediaController.play()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs.coerceIn(0, _uiState.value.durationMs))
        _uiState.update { it.copy(positionMs = positionMs) }
    }

    fun seekBy(deltaMs: Long) {
        val newPosition = (_uiState.value.positionMs + deltaMs).coerceIn(0, _uiState.value.durationMs)
        seekTo(newPosition)
    }

    fun setSpeed(speed: Float) {
        controller?.setPlaybackSpeed(speed)
        _uiState.update { it.copy(speed = speed) }
    }

    fun playNext() {
        controller?.let { if (it.hasNextMediaItem()) it.seekToNext() }
    }

    fun playPrevious() {
        controller?.let { if (it.hasPreviousMediaItem()) it.seekToPrevious() }
    }

    fun toggleLock() {
        _uiState.update { it.copy(locked = !it.locked) }
    }

    fun setAbRepeatPoint() {
        val position = _uiState.value.positionMs
        val state = _uiState.value
        val start = state.abRepeatStartMs
        when {
            start == null -> _uiState.update { it.copy(abRepeatStartMs = position) }
            state.abRepeatEndMs == null && position > start ->
                _uiState.update { it.copy(abRepeatEndMs = position) }
            else -> _uiState.update { it.copy(abRepeatStartMs = null, abRepeatEndMs = null) }
        }
    }

    fun clearAbRepeat() {
        _uiState.update { it.copy(abRepeatStartMs = null, abRepeatEndMs = null) }
    }

    fun startSleepTimer(durationMs: Long) {
        sleepTimerJob?.cancel()
        val endAt = System.currentTimeMillis() + durationMs
        _uiState.update { it.copy(sleepTimerEndAtMs = endAt) }
        sleepTimerJob = viewModelScope.launch {
            delay(durationMs)
            controller?.pause()
            _uiState.update { it.copy(sleepTimerEndAtMs = null) }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _uiState.update { it.copy(sleepTimerEndAtMs = null) }
    }

    private fun startProgressTicker() {
        if (progressJob?.isActive == true) return
        progressJob = viewModelScope.launch {
            while (true) {
                val mediaController = controller
                if (mediaController != null) {
                    val position = mediaController.currentPosition
                    val duration = mediaController.duration.coerceAtLeast(0)
                    _uiState.update {
                        it.copy(
                            positionMs = position,
                            durationMs = duration,
                            bufferedPositionMs = mediaController.bufferedPosition,
                        )
                    }
                    checkAbRepeat(mediaController, position)
                    persistPositionThrottled(position, duration)
                }
                delay(250)
            }
        }
    }

    private fun checkAbRepeat(mediaController: MediaController, position: Long) {
        val state = _uiState.value
        val end = state.abRepeatEndMs ?: return
        val start = state.abRepeatStartMs ?: return
        if (position >= end) mediaController.seekTo(start)
    }

    private fun persistPositionThrottled(position: Long, duration: Long) {
        if (savePositionJob?.isActive == true) return
        val video = _uiState.value.current ?: return
        savePositionJob = viewModelScope.launch {
            positionDao.upsert(
                PlaybackPositionEntity(
                    videoId = video.id,
                    positionMs = position,
                    durationMs = duration,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            delay(3000)
        }
    }

    override fun onCleared() {
        progressJob?.cancel()
        sleepTimerJob?.cancel()
        controller?.removeListener(playerListener)
        controller?.release()
        super.onCleared()
    }
}
