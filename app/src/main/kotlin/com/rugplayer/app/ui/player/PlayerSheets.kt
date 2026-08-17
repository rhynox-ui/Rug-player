package com.rugplayer.app.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClosedCaptionOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val SPEED_OPTIONS = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 3f)
private val SLEEP_TIMER_OPTIONS_MIN = listOf(5, 10, 15, 30, 45, 60, 90)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedSheet(currentSpeed: Float, onSelect: (Float) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = "Playback speed",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyColumn {
            items(SPEED_OPTIONS) { speed ->
                ListItem(
                    headlineContent = { Text(if (speed == 1f) "Normal (1x)" else "${speed}x") },
                    trailingContent = {
                        if (speed == currentSpeed) Icon(Icons.Filled.Check, contentDescription = null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(speed) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleSheet(
    tracks: List<SubtitleTrackUi>,
    selectedId: String?,
    onSelect: (SubtitleTrackUi?) -> Unit,
    onAddExternal: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = "Subtitles",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Column {
            ListItem(
                headlineContent = { Text("Off") },
                leadingContent = { Icon(Icons.Filled.ClosedCaptionOff, contentDescription = null) },
                trailingContent = {
                    RadioButton(selected = selectedId == null, onClick = { onSelect(null) })
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(null) },
            )
            tracks.forEach { track ->
                ListItem(
                    headlineContent = { Text(track.label) },
                    trailingContent = {
                        RadioButton(selected = selectedId == track.id, onClick = { onSelect(track) })
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(track) },
                )
            }
            ListItem(
                headlineContent = { Text("Load subtitle file…") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAddExternal),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerSheet(
    activeEndAtMs: Long?,
    onSelectMinutes: (Int) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = "Sleep timer",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Column {
            if (activeEndAtMs != null) {
                ListItem(
                    headlineContent = { Text("Cancel timer") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onCancel),
                )
            }
            SLEEP_TIMER_OPTIONS_MIN.forEach { minutes ->
                ListItem(
                    headlineContent = { Text("$minutes minutes") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectMinutes(minutes) },
                )
            }
        }
    }
}
