package com.rugplayer.app.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rugplayer.app.ui.components.formatDurationMs

@Composable
fun PlayerTopBar(
    title: String,
    onBack: () -> Unit,
    onSubtitles: () -> Unit,
    onSpeed: () -> Unit,
    onSleepTimer: () -> Unit,
    onLock: () -> Unit,
    onToggleZoom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)))
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
        )
        IconButton(onClick = onSleepTimer) {
            Icon(Icons.Filled.Timer, contentDescription = "Sleep timer", tint = Color.White)
        }
        IconButton(onClick = onSpeed) {
            Icon(Icons.Filled.Speed, contentDescription = "Playback speed", tint = Color.White)
        }
        IconButton(onClick = onSubtitles) {
            Icon(Icons.Filled.Subtitles, contentDescription = "Subtitles", tint = Color.White)
        }
        IconButton(onClick = onToggleZoom) {
            Icon(Icons.Filled.AspectRatio, contentDescription = "Zoom / aspect ratio", tint = Color.White)
        }
        IconButton(onClick = onLock) {
            Icon(Icons.Filled.Lock, contentDescription = "Lock controls", tint = Color.White)
        }
    }
}

@Composable
fun PlayerBottomBar(
    isPlaying: Boolean,
    isBuffering: Boolean,
    positionMs: Long,
    durationMs: Long,
    bufferedMs: Long,
    abStartMs: Long?,
    abEndMs: Long?,
    hasNext: Boolean,
    hasPrevious: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onPip: () -> Unit,
    onAbRepeat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragPositionMs by remember { mutableFloatStateOf(-1f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formatDurationMs(if (dragPositionMs >= 0) dragPositionMs.toLong() else positionMs),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
            )
            Slider(
                value = (if (dragPositionMs >= 0) dragPositionMs else positionMs.toFloat())
                    .coerceIn(0f, durationMs.toFloat().coerceAtLeast(1f)),
                onValueChange = { dragPositionMs = it },
                onValueChangeFinished = {
                    onSeek(dragPositionMs.toLong())
                    dragPositionMs = -1f
                },
                valueRange = 0f..durationMs.toFloat().coerceAtLeast(1f),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            )
            Text(
                text = formatDurationMs(durationMs),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onAbRepeat) {
                Icon(
                    Icons.Filled.RepeatOne,
                    contentDescription = "A-B repeat",
                    tint = if (abStartMs != null) MaterialTheme.colorScheme.primary else Color.White,
                )
            }
            IconButton(onClick = onPrevious, enabled = hasPrevious) {
                Icon(
                    Icons.Filled.SkipPrevious,
                    contentDescription = "Previous",
                    tint = if (hasPrevious) Color.White else Color.White.copy(alpha = 0.3f),
                )
            }
            IconButton(onClick = onPlayPause, modifier = Modifier.size(56.dp)) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp),
                )
            }
            IconButton(onClick = onNext, enabled = hasNext) {
                Icon(
                    Icons.Filled.SkipNext,
                    contentDescription = "Next",
                    tint = if (hasNext) Color.White else Color.White.copy(alpha = 0.3f),
                )
            }
            IconButton(onClick = onPip) {
                Icon(Icons.Filled.PictureInPictureAlt, contentDescription = "Picture in picture", tint = Color.White)
            }
        }
    }
}

@Composable
fun GestureHud(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: Float,
    visible: Boolean,
    alignment: Alignment,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(alignment),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(24.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
            ) {
                Icon(icon, contentDescription = null, tint = Color.White)
                Text(
                    text = "${(value * 100).toInt()}%",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
fun SeekHud(deltaSeconds: Int, visible: Boolean, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Text(
                text = if (deltaSeconds >= 0) "+${deltaSeconds}s" else "${deltaSeconds}s",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
fun LockedOverlay(onUnlock: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        IconButton(
            onClick = onUnlock,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(24.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50)),
        ) {
            Icon(Icons.Filled.LockOpen, contentDescription = "Unlock", tint = Color.White)
        }
    }
}
