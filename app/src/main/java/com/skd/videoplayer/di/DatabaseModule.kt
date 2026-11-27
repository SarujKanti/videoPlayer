package com.skd.videoplayer.di

import android.content.Context
import androidx.room.Room
import com.skd.videoplayer.data.dao.FavoriteDao
import com.skd.videoplayer.data.dao.PlaybackPositionDao
import com.skd.videoplayer.data.dao.WatchedVideoDao
import com.skd.videoplayer.data.database.AppDatabase
import com.skd.videoplayer.data.database.MIGRATION_2_3
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .addMigrations(MIGRATION_2_3)
            .build()

    @Provides
    fun provideFavoriteDao(database: AppDatabase): FavoriteDao = database.favoriteDao()

    @Provides
    fun providePlaybackPositionDao(database: AppDatabase): PlaybackPositionDao = database.playbackPositionDao()

    @Provides
    fun provideWatchedVideoDao(database: AppDatabase): WatchedVideoDao = database.watchedVideoDao()
}
