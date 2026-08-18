package com.rugplayer.app.ui.library

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.rugplayer.app.AppGraph
import com.rugplayer.app.R
import com.rugplayer.app.data.model.VideoItem
import com.rugplayer.app.ui.components.SelectionActionBar
import com.rugplayer.app.ui.components.VideoActionSheet
import com.rugplayer.app.ui.components.VideoListRow
import com.rugplayer.app.ui.components.VideoThumbnailCard
import com.rugplayer.app.ui.components.rememberUriDeleter
import com.rugplayer.app.ui.components.rememberVideoDeleter
import com.rugplayer.app.ui.components.rememberVideoRenamer
import com.rugplayer.app.ui.components.shareVideo
import com.rugplayer.app.ui.components.shareVideos
import com.rugplayer.app.ui.components.videoUri

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    graph: AppGraph,
    onOpenVideo: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTransfer: () -> Unit,
    onOpenFolder: (String) -> Unit,
    onOpenStatusSaver: () -> Unit,
) {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permissionState = rememberPermissionState(permission)

    if (permissionState.status.isGranted) {
        val viewModel: LibraryViewModel = viewModel(
            factory = viewModelFactory {
                initializer { LibraryViewModel(graph.videoRepository, graph.playbackPositionDao) }
            },
        )
        LibraryContent(
            graph = graph,
            viewModel = viewModel,
            onOpenVideo = onOpenVideo,
            onOpenSettings = onOpenSettings,
            onOpenTransfer = onOpenTransfer,
            onOpenFolder = onOpenFolder,
            onOpenStatusSaver = onOpenStatusSaver,
        )
    } else {
        Scaffold(
            topBar = { TopAppBar(title = { Text(stringResource(R.string.library_title)) }) },
        ) { padding ->
            PermissionRequest(
                showRationale = permissionState.status.shouldShowRationale,
                onRequest = { permissionState.launchPermissionRequest() },
                padding = padding,
            )
        }
    }
}

@Composable
private fun PermissionRequest(
    showRationale: Boolean,
    onRequest: () -> Unit,
    padding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.MovieFilter,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(modifier = Modifier.padding(top = 16.dp)) {
            Text(
                text = stringResource(R.string.permission_rationale_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.permission_rationale_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Button(onClick = onRequest, modifier = Modifier.padding(top = 24.dp)) {
            Text(stringResource(R.string.permission_grant))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryContent(
    graph: AppGraph,
    viewModel: LibraryViewModel,
    onOpenVideo: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTransfer: () -> Unit,
    onOpenFolder: (String) -> Unit,
    onOpenStatusSaver: () -> Unit,
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    var sortMenuOpen by remember { mutableStateOf(false) }
    val selectedIds by graph.selectionController.selectedIds.collectAsState()
    val selectionMode = selectedIds.isNotEmpty()
    val deleteVideos = rememberVideoDeleter {}
    val deleteSelected = rememberUriDeleter { success -> if (success) graph.selectionController.clear() }
    val renameVideo = rememberVideoRenamer {}
    val showingFlatList = state.query.isNotBlank() || state.viewMode == LibraryViewMode.ALL_VIDEOS

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

    var folderPendingDelete by remember { mutableStateOf<FolderSummary?>(null) }
    val deleteFolderVideos = rememberVideoDeleter { folderPendingDelete = null }
    folderPendingDelete?.let { folder ->
        AlertDialog(
            onDismissRequest = { folderPendingDelete = null },
            title = { Text("Delete \"${folder.name}\"?") },
            text = {
                Text(
                    "This deletes all ${folder.videoCount} " +
                        (if (folder.videoCount == 1) "video" else "videos") +
                        " in this folder. This can't be undone.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val toDelete = state.videos
                        .filter { it.video.folder == folder.name }
                        .map { it.video }
                    deleteFolderVideos(toDelete)
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { folderPendingDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.library_title)) },
                actions = {
                    IconButton(onClick = onOpenStatusSaver) {
                        Icon(Icons.Filled.Download, contentDescription = "Status Saver")
                    }
                    IconButton(onClick = onOpenTransfer) {
                        Icon(Icons.Filled.Wifi, contentDescription = stringResource(R.string.action_transfer))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.action_settings))
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                )
                IconButton(
                    onClick = {
                        viewModel.setViewMode(
                            if (state.viewMode == LibraryViewMode.FOLDERS) LibraryViewMode.ALL_VIDEOS
                            else LibraryViewMode.FOLDERS,
                        )
                    },
                ) {
                    Icon(
                        if (state.viewMode == LibraryViewMode.FOLDERS) Icons.Filled.GridView else Icons.Filled.Folder,
                        contentDescription = "Switch view",
                    )
                }
                Box {
                    IconButton(onClick = { sortMenuOpen = true }) {
                        Icon(Icons.Filled.Sort, contentDescription = null)
                    }
                    DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                        SortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    viewModel.setSort(option)
                                    sortMenuOpen = false
                                },
                            )
                        }
                    }
                }
            }

            if (state.videos.isEmpty() && state.folders.isEmpty()) {
                EmptyLibrary()
            } else if (showingFlatList) {
                AllVideosGrid(
                    state = state,
                    selectedIds = selectedIds,
                    selectionMode = selectionMode,
                    onOpenVideo = onOpenVideo,
                    onToggleSelect = { id -> graph.selectionController.toggle(id) },
                    onLongPress = { actionSheetVideo = it },
                )
            } else {
                FoldersList(
                    state = state,
                    onOpenVideo = onOpenVideo,
                    onOpenFolder = onOpenFolder,
                    onDeleteFolder = { folderPendingDelete = it },
                )
            }
        }
    }
}

@Composable
private fun EmptyLibrary() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.library_empty_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.library_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun FoldersList(
    state: LibraryUiState,
    onOpenVideo: (Long) -> Unit,
    onOpenFolder: (String) -> Unit,
    onDeleteFolder: (FolderSummary) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (state.continueWatching.isNotEmpty()) {
            item { ContinueWatchingRow(items = state.continueWatching, onOpenVideo = onOpenVideo) }
        }
        listItems(state.folders, key = { it.name }) { folder ->
            FolderRow(
                folder = folder,
                onClick = { onOpenFolder(folder.name) },
                onLongClick = { onDeleteFolder(folder) },
            )
        }
    }
}

@Composable
private fun AllVideosGrid(
    state: LibraryUiState,
    selectedIds: Set<Long>,
    selectionMode: Boolean,
    onOpenVideo: (Long) -> Unit,
    onToggleSelect: (Long) -> Unit,
    onLongPress: (VideoItem) -> Unit,
) {
    if (state.query.isNotBlank()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            listItems(state.videos, key = { it.video.id }) { item ->
                VideoListRow(
                    video = item.video,
                    progressFraction = item.progressFraction,
                    selected = item.video.id in selectedIds,
                    selectionMode = selectionMode,
                    onClick = {
                        if (selectionMode) onToggleSelect(item.video.id) else onOpenVideo(item.video.id)
                    },
                    onLongClick = {
                        if (selectionMode) onToggleSelect(item.video.id) else onLongPress(item.video)
                    },
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            gridItems(state.videos, key = { it.video.id }) { item ->
                VideoThumbnailCard(
                    video = item.video,
                    progressFraction = item.progressFraction,
                    selected = item.video.id in selectedIds,
                    onClick = {
                        if (selectionMode) onToggleSelect(item.video.id) else onOpenVideo(item.video.id)
                    },
                    onLongClick = {
                        if (selectionMode) onToggleSelect(item.video.id) else onLongPress(item.video)
                    },
                )
            }
        }
    }
}

@Composable
private fun ContinueWatchingRow(items: List<LibraryVideoUi>, onOpenVideo: (Long) -> Unit) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            text = "Continue watching",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            listItems(items, key = { it.video.id }) { item ->
                VideoThumbnailCard(
                    video = item.video,
                    progressFraction = item.progressFraction,
                    onClick = { onOpenVideo(item.video.id) },
                    modifier = Modifier.width(180.dp),
                )
            }
        }
    }
}
