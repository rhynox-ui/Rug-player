package com.rugplayer.app.ui.library

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rugplayer.app.AppGraph
import com.rugplayer.app.ui.components.SelectionTopBar
import com.rugplayer.app.ui.components.VideoListRow
import com.rugplayer.app.ui.components.rememberVideoDeleter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderVideosScreen(
    graph: AppGraph,
    folderName: String,
    onOpenVideo: (Long) -> Unit,
    onBack: () -> Unit,
) {
    val viewModel: FolderVideosViewModel = viewModel(
        factory = viewModelFactory {
            initializer { FolderVideosViewModel(folderName, graph.videoRepository, graph.playbackPositionDao) }
        },
    )
    val videos by viewModel.videos.collectAsState()
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    val selectionMode = selectedIds.isNotEmpty()

    val deleteVideos = rememberVideoDeleter { selectedIds = emptySet() }

    Scaffold(
        topBar = {
            if (selectionMode) {
                SelectionTopBar(
                    selectedCount = selectedIds.size,
                    onCancel = { selectedIds = emptySet() },
                    onDelete = {
                        val toDelete = videos.filter { it.video.id in selectedIds }.map { it.video }
                        deleteVideos(toDelete)
                    },
                )
            } else {
                TopAppBar(
                    title = { Text(folderName) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                )
            }
        },
    ) { padding ->
        LazyColumn(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            items(videos, key = { it.video.id }) { item ->
                VideoListRow(
                    video = item.video,
                    progressFraction = item.progressFraction,
                    selected = item.video.id in selectedIds,
                    selectionMode = selectionMode,
                    onClick = {
                        if (selectionMode) {
                            selectedIds = toggleSelection(selectedIds, item.video.id)
                        } else {
                            onOpenVideo(item.video.id)
                        }
                    },
                    onLongClick = { selectedIds = toggleSelection(selectedIds, item.video.id) },
                )
            }
        }
    }
}

internal fun toggleSelection(current: Set<Long>, id: Long): Set<Long> =
    if (id in current) current - id else current + id
