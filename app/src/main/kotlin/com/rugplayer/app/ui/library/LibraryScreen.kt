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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.rugplayer.app.ui.components.VideoThumbnailCard

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    graph: AppGraph,
    onOpenVideo: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTransfer: () -> Unit,
) {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permissionState = rememberPermissionState(permission)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.library_title)) },
                actions = {
                    IconButton(onClick = onOpenTransfer) {
                        Icon(Icons.Filled.Wifi, contentDescription = stringResource(R.string.action_transfer))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.action_settings))
                    }
                },
            )
        },
    ) { padding ->
        if (permissionState.status.isGranted) {
            val viewModel: LibraryViewModel = viewModel(
                factory = viewModelFactory {
                    initializer { LibraryViewModel(graph.videoRepository, graph.playbackPositionDao) }
                },
            )
            LibraryContent(
                viewModel = viewModel,
                onOpenVideo = onOpenVideo,
                padding = padding,
            )
        } else {
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
    viewModel: LibraryViewModel,
    onOpenVideo: (Long) -> Unit,
    padding: PaddingValues,
) {
    val state by viewModel.uiState.collectAsState()
    var sortMenuOpen by remember { mutableStateOf(false) }

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
            IconButton(onClick = { viewModel.setGroupByFolder(!state.groupByFolder) }) {
                Icon(
                    if (state.groupByFolder) Icons.Filled.Folder else Icons.Filled.GridView,
                    contentDescription = "Group by folder",
                    tint = if (state.groupByFolder) MaterialTheme.colorScheme.primary else LocalContentColor.current,
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

        if (state.videos.isEmpty()) {
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
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (state.continueWatching.isNotEmpty() && state.query.isBlank()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        ContinueWatchingRow(items = state.continueWatching, onOpenVideo = onOpenVideo)
                    }
                }

                if (state.groupByFolder) {
                    val grouped = state.videos.groupBy { it.video.folder }.toSortedMap()
                    grouped.forEach { (folder, videosInFolder) ->
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = folder,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                        items(videosInFolder, key = { it.video.id }) { item ->
                            VideoThumbnailCard(
                                video = item.video,
                                progressFraction = item.progressFraction,
                                onClick = { onOpenVideo(item.video.id) },
                            )
                        }
                    }
                } else {
                    items(state.videos, key = { it.video.id }) { item ->
                        VideoThumbnailCard(
                            video = item.video,
                            progressFraction = item.progressFraction,
                            onClick = { onOpenVideo(item.video.id) },
                        )
                    }
                }
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
            modifier = Modifier.padding(bottom = 8.dp),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            lazyRowItems(items, key = { it.video.id }) { item ->
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
