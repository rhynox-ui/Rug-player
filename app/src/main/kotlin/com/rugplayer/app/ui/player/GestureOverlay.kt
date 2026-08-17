package com.rugplayer.app.ui.player

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

private enum class DragAxis { NONE, HORIZONTAL, VERTICAL_LEFT, VERTICAL_RIGHT }

/**
 * Full-surface gesture layer: single tap toggles controls, double tap on the
 * left/right half seeks -/+10s, vertical drag on the left half adjusts
 * brightness, on the right half adjusts volume, and horizontal drag anywhere
 * scrubs playback position.
 */
@Composable
fun GestureOverlay(
    enabled: Boolean,
    onTap: () -> Unit,
    onDoubleTapSeek: (forward: Boolean) -> Unit,
    onBrightnessDelta: (Float) -> Unit,
    onVolumeDelta: (Float) -> Unit,
    onSeekDragStart: () -> Unit,
    onSeekDrag: (deltaMs: Long) -> Unit,
    onSeekDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var axis by remember { mutableStateOf(DragAxis.NONE) }
    var accumulatedX by remember { mutableFloatStateOf(0f) }
    var accumulatedY by remember { mutableFloatStateOf(0f) }

    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnDoubleTap by rememberUpdatedState(onDoubleTapSeek)
    val currentOnBrightness by rememberUpdatedState(onBrightnessDelta)
    val currentOnVolume by rememberUpdatedState(onVolumeDelta)
    val currentOnSeekStart by rememberUpdatedState(onSeekDragStart)
    val currentOnSeekDrag by rememberUpdatedState(onSeekDrag)
    val currentOnSeekEnd by rememberUpdatedState(onSeekDragEnd)

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onTap = { currentOnTap() },
                    onDoubleTap = { offset -> currentOnDoubleTap(offset.x > size.width / 2f) },
                )
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                var startX = 0f
                detectDragGestures(
                    onDragStart = { offset ->
                        axis = DragAxis.NONE
                        accumulatedX = 0f
                        accumulatedY = 0f
                        startX = offset.x
                    },
                    onDragEnd = {
                        if (axis == DragAxis.HORIZONTAL) currentOnSeekEnd()
                        axis = DragAxis.NONE
                    },
                    onDragCancel = {
                        if (axis == DragAxis.HORIZONTAL) currentOnSeekEnd()
                        axis = DragAxis.NONE
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedX += dragAmount.x
                        accumulatedY += dragAmount.y

                        if (axis == DragAxis.NONE && (abs(accumulatedX) > 16f || abs(accumulatedY) > 16f)) {
                            axis = if (abs(accumulatedX) > abs(accumulatedY)) {
                                currentOnSeekStart()
                                DragAxis.HORIZONTAL
                            } else if (startX < size.width / 2f) {
                                DragAxis.VERTICAL_LEFT
                            } else {
                                DragAxis.VERTICAL_RIGHT
                            }
                        }

                        when (axis) {
                            DragAxis.HORIZONTAL -> {
                                // ~250ms of seek per dp of horizontal drag.
                                currentOnSeekDrag((dragAmount.x * 250).toLong())
                            }
                            DragAxis.VERTICAL_LEFT -> {
                                currentOnBrightness(-dragAmount.y / size.height)
                            }
                            DragAxis.VERTICAL_RIGHT -> {
                                currentOnVolume(-dragAmount.y / size.height)
                            }
                            DragAxis.NONE -> Unit
                        }
                    },
                )
            },
    )
}
