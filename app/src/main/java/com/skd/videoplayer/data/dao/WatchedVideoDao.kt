package com.skd.videoplayer.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.skd.videoplayer.data.entity.WatchedVideoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchedVideoDao {

    @Query("SELECT video_id FROM watched_videos")
    fun observeIds(): Flow<List<Long>>

    /** Keeps the original timestamp when the video is already recorded. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: WatchedVideoEntity)
}
