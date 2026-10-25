package com.readplusnext.di

import com.readplusnext.data.repository.BackupRepositoryImpl
import com.readplusnext.data.repository.CategoryRepositoryImpl
import com.readplusnext.data.repository.ComicRepositoryImpl
import com.readplusnext.data.repository.SettingsRepositoryImpl
import com.readplusnext.data.repository.VideoRepositoryImpl
import com.readplusnext.domain.repository.BackupRepository
import com.readplusnext.domain.repository.CategoryRepository
import com.readplusnext.domain.repository.ComicRepository
import com.readplusnext.domain.repository.SettingsRepository
import com.readplusnext.domain.repository.VideoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindComicRepository(impl: ComicRepositoryImpl): ComicRepository

    @Binds
    @Singleton
    abstract fun bindVideoRepository(impl: VideoRepositoryImpl): VideoRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository
}

