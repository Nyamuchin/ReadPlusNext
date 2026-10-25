package com.readplusnext.di

import android.content.Context
import androidx.room.Room
import com.readplusnext.data.local.ReadPlusNextDatabase
import com.readplusnext.data.local.dao.CategoryDao
import com.readplusnext.data.local.dao.ComicDao
import com.readplusnext.data.local.dao.VideoDao
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
    fun provideDatabase(@ApplicationContext context: Context): ReadPlusNextDatabase =
        Room.databaseBuilder(context, ReadPlusNextDatabase::class.java, "readplusnext.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideComicDao(database: ReadPlusNextDatabase): ComicDao = database.comicDao()

    @Provides
    fun provideVideoDao(database: ReadPlusNextDatabase): VideoDao = database.videoDao()

    @Provides
    fun provideCategoryDao(database: ReadPlusNextDatabase): CategoryDao = database.categoryDao()
}
