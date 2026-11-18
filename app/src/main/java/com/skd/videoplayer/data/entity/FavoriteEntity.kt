package com.skd.videoplayer.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** A video the user marked as favorite, keyed by its MediaStore id. */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    @ColumnInfo(name = "video_id")
    val videoId: Long,
    @ColumnInfo(name = "added_at")
    val addedAtEpochMs: Long,
)
