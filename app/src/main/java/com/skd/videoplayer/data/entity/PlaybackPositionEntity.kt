package com.skd.videoplayer.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Where the user stopped watching a video. Keyed by the player's media id: the MediaStore id for
 * library videos, the content URI for files opened from other apps.
 */
@Entity(tableName = "playback_positions")
data class PlaybackPositionEntity(
    @PrimaryKey
    @ColumnInfo(name = "media_key")
    val mediaKey: String,
    @ColumnInfo(name = "position_ms")
    val positionMs: Long,
    @ColumnInfo(name = "duration_ms")
    val durationMs: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAtEpochMs: Long,
)
