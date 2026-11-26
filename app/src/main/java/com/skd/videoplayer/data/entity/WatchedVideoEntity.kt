package com.skd.videoplayer.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** A library video that has been played at least once, keyed by its MediaStore id. */
@Entity(tableName = "watched_videos")
data class WatchedVideoEntity(
    @PrimaryKey
    @ColumnInfo(name = "video_id")
    val videoId: Long,
    @ColumnInfo(name = "first_played_at")
    val firstPlayedAtEpochMs: Long,
)
