package com.skd.videoplayer.data.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.skd.videoplayer.data.dao.FavoriteDao
import com.skd.videoplayer.data.dao.PlaybackPositionDao
import com.skd.videoplayer.data.entity.FavoriteEntity
import com.skd.videoplayer.data.entity.PlaybackPositionEntity

@Database(
    entities = [FavoriteEntity::class, PlaybackPositionEntity::class],
    version = 2,
    exportSchema = true,
    // v2 only adds the playback_positions table, which Room can migrate on its own.
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao

    abstract fun playbackPositionDao(): PlaybackPositionDao

    companion object {
        const val NAME = "lumora.db"
    }
}
