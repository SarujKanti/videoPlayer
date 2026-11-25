package com.skd.videoplayer.di

import com.skd.videoplayer.data.repository.FavoritesRepositoryImpl
import com.skd.videoplayer.data.repository.MediaStoreVideoRowSource
import com.skd.videoplayer.data.repository.PlaybackPositionRepositoryImpl
import com.skd.videoplayer.data.repository.SettingsRepositoryImpl
import com.skd.videoplayer.data.repository.VideoEditRepositoryImpl
import com.skd.videoplayer.data.repository.VideoRepositoryImpl
import com.skd.videoplayer.data.repository.VideoRowSource
import com.skd.videoplayer.domain.repository.FavoritesRepository
import com.skd.videoplayer.domain.repository.PlaybackPositionRepository
import com.skd.videoplayer.domain.repository.SettingsRepository
import com.skd.videoplayer.domain.repository.VideoEditRepository
import com.skd.videoplayer.domain.repository.VideoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindVideoRowSource(impl: MediaStoreVideoRowSource): VideoRowSource

    @Binds
    abstract fun bindVideoRepository(impl: VideoRepositoryImpl): VideoRepository

    @Binds
    abstract fun bindFavoritesRepository(impl: FavoritesRepositoryImpl): FavoritesRepository

    @Binds
    abstract fun bindVideoEditRepository(impl: VideoEditRepositoryImpl): VideoEditRepository

    @Binds
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    abstract fun bindPlaybackPositionRepository(impl: PlaybackPositionRepositoryImpl): PlaybackPositionRepository
}
