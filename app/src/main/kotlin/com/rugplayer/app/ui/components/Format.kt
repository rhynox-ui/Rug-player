package com.rugplayer.app.ui.components

import java.util.Locale
import kotlin.math.roundToInt

fun formatDurationMs(durationMs: Long): String {
    if (durationMs <= 0) return "0:00"
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = listOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unitIndex = -1
    do {
        value /= 1024.0
        unitIndex++
    } while (value >= 1024 && unitIndex < units.size - 1)
    val rounded = (value * 10).roundToInt() / 10.0
    return "$rounded ${units[unitIndex]}"
}
