package com.skd.videoplayer.di

import com.skd.videoplayer.data.repository.MediaStoreVideoRowSource
import com.skd.videoplayer.data.repository.VideoRepositoryImpl
import com.skd.videoplayer.data.repository.VideoRowSource
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
}
