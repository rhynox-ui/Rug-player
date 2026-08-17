package com.rugplayer.app.ui.transfer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.rugplayer.app.AppGraph
import com.rugplayer.app.R
import com.rugplayer.app.ui.components.formatBytes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(graph: AppGraph, onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: TransferViewModel = viewModel(
        factory = viewModelFactory {
            initializer { TransferViewModel(context.applicationContext, graph.videoRepository) }
        },
    )
    val state by viewModel.uiState.collectAsState()
    var tabIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.transfer_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = tabIndex) {
                Tab(
                    selected = tabIndex == 0,
                    onClick = { tabIndex = 0 },
                    text = { Text(stringResource(R.string.transfer_send_tab)) },
                )
                Tab(
                    selected = tabIndex == 1,
                    onClick = { tabIndex = 1 },
                    text = { Text(stringResource(R.string.transfer_receive_tab)) },
                )
            }

            if (tabIndex == 0) {
                SendTab(state = state, viewModel = viewModel)
            } else {
                ReceiveTab(state = state, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun SendTab(state: TransferUiState, viewModel: TransferViewModel) {
    val context = LocalContext.current

    if (state.sendingVideo != null) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(state.sendingVideo.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${state.clientsServed} device(s) connected",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            if (state.sendUrl != null) {
                Card(modifier = Modifier.padding(top = 16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(state.sendUrl, style = MaterialTheme.typography.bodyLarge)
                        Row(modifier = Modifier.padding(top = 12.dp)) {
                            OutlinedButton(onClick = { copyToClipboard(context, state.sendUrl) }) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                                Text("Copy")
                            }
                            Button(onClick = { shareLink(context, state.sendUrl) }, modifier = Modifier.padding(start = 8.dp)) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                                Text("Share")
                            }
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.transfer_send_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp),
                )
            } else if (state.statusMessage != null) {
                Text(
                    text = state.statusMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            OutlinedButton(onClick = viewModel::stopSending, modifier = Modifier.padding(top = 24.dp)) {
                Text(stringResource(R.string.transfer_send_stop))
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = stringResource(R.string.transfer_send_pick),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp),
            )
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.videos, key = { it.id }) { video ->
                    ListItem(
                        leadingContent = { Icon(Icons.Filled.Movie, contentDescription = null) },
                        headlineContent = { Text(video.title) },
                        supportingContent = { Text(formatBytes(video.sizeBytes)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.startSending(video) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ReceiveTab(state: TransferUiState, viewModel: TransferViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Icon(
            Icons.Filled.Wifi,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Text(stringResource(R.string.transfer_receive_hint), style = MaterialTheme.typography.bodyMedium)

        OutlinedTextField(
            value = state.receiveUrlInput,
            onValueChange = viewModel::setReceiveUrlInput,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            placeholder = { Text("http://192.168.1.23:5000/movie.mp4") },
            singleLine = true,
            enabled = !state.isReceiving,
        )

        Button(
            onClick = viewModel::startReceiving,
            enabled = !state.isReceiving && state.receiveUrlInput.isNotBlank(),
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Text(stringResource(R.string.transfer_receive_action))
        }

        if (state.isReceiving) {
            Column(modifier = Modifier.padding(top = 24.dp)) {
                Text(stringResource(R.string.transfer_receiving))
                LinearProgressIndicator(
                    progress = { state.receiveProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
        }

        state.statusMessage?.let { message ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 24.dp),
            ) {
                Text(
                    text = message,
                    color = if (state.statusIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Rug Player transfer link", text))
}

private fun shareLink(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, null))
}
