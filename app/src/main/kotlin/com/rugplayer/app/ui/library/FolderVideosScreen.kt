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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.rugplayer.app.AppGraph
import com.rugplayer.app.data.model.VideoItem
import com.rugplayer.app.ui.components.SelectionActionBar
import com.rugplayer.app.ui.components.VideoActionSheet
import com.rugplayer.app.ui.components.VideoListRow
import com.rugplayer.app.ui.components.rememberUriDeleter
import com.rugplayer.app.ui.components.rememberVideoDeleter
import com.rugplayer.app.ui.components.rememberVideoRenamer
import com.rugplayer.app.ui.components.shareVideo
import com.rugplayer.app.ui.components.shareVideos
import com.rugplayer.app.ui.components.videoUri

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
    val context = LocalContext.current
    val videos by viewModel.videos.collectAsState()
    val selectedIds by graph.selectionController.selectedIds.collectAsState()
    val selectionMode = selectedIds.isNotEmpty()

    val deleteVideos = rememberVideoDeleter {}
    val deleteSelected = rememberUriDeleter { success -> if (success) graph.selectionController.clear() }
    val renameVideo = rememberVideoRenamer {}

    var actionSheetVideo by remember { mutableStateOf<VideoItem?>(null) }
    actionSheetVideo?.let { video ->
        VideoActionSheet(
            video = video,
            onDismiss = { actionSheetVideo = null },
            onShare = { shareVideo(context, video) },
            onRename = { newName -> renameVideo(video, newName) },
            onSelect = { graph.selectionController.select(video.id) },
            onDelete = { deleteVideos(listOf(video)) },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(folderName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            if (selectionMode) {
                SelectionActionBar(
                    selectedCount = selectedIds.size,
                    onShare = { shareVideos(context, selectedIds) },
                    onDelete = { deleteSelected(selectedIds.map(::videoUri)) },
                    onCancel = { graph.selectionController.clear() },
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
                            graph.selectionController.toggle(item.video.id)
                        } else {
                            onOpenVideo(item.video.id)
                        }
                    },
                    onLongClick = {
                        if (selectionMode) {
                            graph.selectionController.toggle(item.video.id)
                        } else {
                            actionSheetVideo = item.video
                        }
                    },
                )
            }
        }
    }
}
