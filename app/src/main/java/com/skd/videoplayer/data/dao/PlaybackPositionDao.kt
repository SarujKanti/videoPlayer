package com.skd.videoplayer.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.skd.videoplayer.data.entity.PlaybackPositionEntity

@Dao
interface PlaybackPositionDao {

    @Query("SELECT * FROM playback_positions WHERE media_key = :mediaKey")
    suspend fun get(mediaKey: String): PlaybackPositionEntity?

    @Upsert
    suspend fun upsert(entity: PlaybackPositionEntity)

    @Query("DELETE FROM playback_positions WHERE media_key = :mediaKey")
    suspend fun delete(mediaKey: String)
}
