package com.skd.videoplayer.data.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.skd.videoplayer.data.dao.FavoriteDao
import com.skd.videoplayer.data.dao.PlaybackPositionDao
import com.skd.videoplayer.data.dao.WatchedVideoDao
import com.skd.videoplayer.data.entity.FavoriteEntity
import com.skd.videoplayer.data.entity.PlaybackPositionEntity
import com.skd.videoplayer.data.entity.WatchedVideoEntity

@Database(
    entities = [FavoriteEntity::class, PlaybackPositionEntity::class, WatchedVideoEntity::class],
    version = 3,
    exportSchema = true,
    // v2 only adds the playback_positions table, which Room can migrate on its own.
    // v3 is the manual MIGRATION_2_3 (it also seeds data), registered in DatabaseModule.
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao

    abstract fun playbackPositionDao(): PlaybackPositionDao

    abstract fun watchedVideoDao(): WatchedVideoDao

    companion object {
        const val NAME = "lumora.db"
    }
}
