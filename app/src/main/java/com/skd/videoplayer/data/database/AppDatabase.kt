package com.skd.videoplayer.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.skd.videoplayer.data.dao.FavoriteDao
import com.skd.videoplayer.data.entity.FavoriteEntity

@Database(
    entities = [FavoriteEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        const val NAME = "lumora.db"
    }
}
