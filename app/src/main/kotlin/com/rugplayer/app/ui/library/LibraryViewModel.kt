package com.rugplayer.app.ui.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rugplayer.app.data.db.PlaybackPositionDao
import com.rugplayer.app.data.model.VideoItem
import com.rugplayer.app.data.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

enum class SortOption(val label: String) {
    DATE_ADDED("Date added"),
    NAME("Name"),
    DURATION("Duration"),
    SIZE("Size"),
}

enum class LibraryViewMode { FOLDERS, ALL_VIDEOS }

data class LibraryVideoUi(
    val video: VideoItem,
    val progressFraction: Float,
    val lastPlayedAt: Long = 0,
)

data class FolderSummary(
    val name: String,
    val videoCount: Int,
    val totalSizeBytes: Long,
    val previewUri: Uri?,
)

data class LibraryUiState(
    val videos: List<LibraryVideoUi> = emptyList(),
    val continueWatching: List<LibraryVideoUi> = emptyList(),
    val folders: List<FolderSummary> = emptyList(),
    val query: String = "",
    val sort: SortOption = SortOption.DATE_ADDED,
    val viewMode: LibraryViewMode = LibraryViewMode.FOLDERS,
)

class LibraryViewModel(
    private val videoRepository: VideoRepository,
    private val positionDao: PlaybackPositionDao,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val sort = MutableStateFlow(SortOption.DATE_ADDED)
    private val viewMode = MutableStateFlow(LibraryViewMode.FOLDERS)

    val uiState: StateFlow<LibraryUiState> = combine(
        videoRepository.observeVideos(),
        positionDao.observeAll(),
        query,
        sort,
        viewMode,
    ) { videos, positions, q, sortOption, mode ->
        val positionByVideoId = positions.associateBy { it.videoId }

        val filtered = if (q.isBlank()) videos else videos.filter {
            it.title.contains(q, ignoreCase = true) || it.displayName.contains(q, ignoreCase = true)
        }

        val sorted = when (sortOption) {
            SortOption.DATE_ADDED -> filtered.sortedByDescending { it.dateAddedSec }
            SortOption.NAME -> filtered.sortedBy { it.title.lowercase() }
            SortOption.DURATION -> filtered.sortedByDescending { it.durationMs }
            SortOption.SIZE -> filtered.sortedByDescending { it.sizeBytes }
        }

        val uiVideos = sorted.map { video ->
            val pos = positionByVideoId[video.id]
            val fraction = if (pos != null && pos.durationMs > 0) {
                (pos.positionMs.toFloat() / pos.durationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f
            LibraryVideoUi(video, fraction, pos?.updatedAt ?: 0)
        }

        val continueWatching = uiVideos
            .filter { it.progressFraction in 0.02f..0.95f }
            .sortedByDescending { it.lastPlayedAt }

        val folders = videos
            .groupBy { it.folder }
            .map { (name, videosInFolder) ->
                FolderSummary(
                    name = name,
                    videoCount = videosInFolder.size,
                    totalSizeBytes = videosInFolder.sumOf { it.sizeBytes },
                    previewUri = videosInFolder.firstOrNull()?.uri,
                )
            }
            .sortedBy { it.name.lowercase() }

        LibraryUiState(
            videos = uiVideos,
            continueWatching = continueWatching,
            folders = folders,
            query = q,
            sort = sortOption,
            viewMode = mode,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryUiState())

    fun setQuery(value: String) = query.update { value }
    fun setSort(value: SortOption) = sort.update { value }
    fun setViewMode(value: LibraryViewMode) = viewMode.update { value }
}
