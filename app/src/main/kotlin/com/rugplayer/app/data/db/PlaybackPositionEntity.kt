package com.rugplayer.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_positions")
data class PlaybackPositionEntity(
    @PrimaryKey val videoId: Long,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long,
)
