package com.skd.videoplayer.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.skd.videoplayer.data.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("SELECT video_id FROM favorites")
    fun observeIds(): Flow<List<Long>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE video_id = :videoId)")
    suspend fun isFavorite(videoId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE video_id = :videoId")
    suspend fun delete(videoId: Long)
}
