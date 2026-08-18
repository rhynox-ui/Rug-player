package com.rugplayer.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rugplayer.app.data.db.PlaybackPositionDao
import com.rugplayer.app.data.repository.VideoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class FolderVideosViewModel(
    private val folderName: String,
    videoRepository: VideoRepository,
    positionDao: PlaybackPositionDao,
) : ViewModel() {

    val videos: StateFlow<List<LibraryVideoUi>> = combine(
        videoRepository.observeVideos(),
        positionDao.observeAll(),
    ) { allVideos, positions ->
        val positionByVideoId = positions.associateBy { it.videoId }
        allVideos
            .filter { it.folder == folderName }
            .sortedByDescending { it.dateAddedSec }
            .map { video ->
                val pos = positionByVideoId[video.id]
                val fraction = if (pos != null && pos.durationMs > 0) {
                    (pos.positionMs.toFloat() / pos.durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f
                LibraryVideoUi(video, fraction, pos?.updatedAt ?: 0)
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
