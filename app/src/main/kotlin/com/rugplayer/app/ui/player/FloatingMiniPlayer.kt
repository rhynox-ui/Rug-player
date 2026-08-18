package com.rugplayer.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.rugplayer.app.player.PlaybackController
import com.rugplayer.app.player.NowPlaying
import kotlin.math.roundToInt

private val MiniPlayerWidth = 160.dp
private val MiniPlayerHeight = 90.dp
private val MiniPlayerMargin = 16.dp

/**
 * A small draggable "keep watching while I do something else" window that
 * shows the live video from the shared [PlaybackController] session. Only
 * one PlayerView is ever attached to the controller at a time — this one,
 * or the full PlayerScreen's — so there's no surface contention between
 * them; whichever screen is composed last simply takes over the output.
 */
@Composable
fun FloatingMiniPlayer(playbackController: PlaybackController, onExpand: (NowPlaying) -> Unit) {
    val nowPlaying by playbackController.nowPlaying.collectAsState()
    val info = nowPlaying ?: return
    val controller = playbackController.controller ?: return
    val density = LocalDensity.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxDragXPx = with(density) { (maxWidth - MiniPlayerWidth - MiniPlayerMargin * 2).toPx() }
            .coerceAtLeast(0f)
        val maxDragYPx = with(density) { (maxHeight - MiniPlayerHeight - MiniPlayerMargin * 2).toPx() }
            .coerceAtLeast(0f)
        var dragX by remember { mutableFloatStateOf(0f) }
        var dragY by remember { mutableFloatStateOf(0f) }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset { IntOffset(dragX.roundToInt(), dragY.roundToInt()) }
                .size(MiniPlayerWidth, MiniPlayerHeight)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        dragX = (dragX + dragAmount.x).coerceIn(-maxDragXPx, 0f)
                        dragY = (dragY + dragAmount.y).coerceIn(-maxDragYPx, 0f)
                    }
                },
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    }
                },
                update = { it.player = controller },
                onRelease = { it.player = null },
                modifier = Modifier.fillMaxSize(),
            )

            IconButton(
                onClick = { playbackController.stopAndClear() },
                modifier = Modifier.align(Alignment.TopEnd).size(28.dp),
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
            }

            IconButton(
                onClick = { if (info.isPlaying) controller.pause() else controller.play() },
                modifier = Modifier.align(Alignment.Center).size(36.dp),
            ) {
                Icon(
                    if (info.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (info.isPlaying) "Pause" else "Play",
                    tint = Color.White,
                )
            }

            IconButton(
                onClick = { onExpand(info) },
                modifier = Modifier.align(Alignment.BottomEnd).size(28.dp),
            ) {
                Icon(Icons.Filled.OpenInFull, contentDescription = "Expand", tint = Color.White)
            }
        }
    }
}
