package com.rugplayer.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackPositionDao {
    @Upsert
    suspend fun upsert(position: PlaybackPositionEntity)

    @Query("SELECT * FROM playback_positions WHERE videoId = :videoId")
    suspend fun get(videoId: Long): PlaybackPositionEntity?

    @Query("SELECT * FROM playback_positions")
    fun observeAll(): Flow<List<PlaybackPositionEntity>>

    @Query("DELETE FROM playback_positions WHERE videoId = :videoId")
    suspend fun clear(videoId: Long)

    @Delete
    suspend fun delete(position: PlaybackPositionEntity)
}
