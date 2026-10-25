package com.readplusnext.domain.repository

import com.readplusnext.domain.model.BackupProgress
import com.readplusnext.domain.model.Category
import com.readplusnext.domain.model.CategoryType
import com.readplusnext.domain.model.Comic
import com.readplusnext.domain.model.ComicPage
import com.readplusnext.domain.model.ImportSummary
import com.readplusnext.domain.model.Video
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ComicRepository {
    fun observeComics(): Flow<List<Comic>>
    fun observeComicsByCategory(categoryId: Long): Flow<List<Comic>>
    fun observeComic(id: Long): Flow<Comic?>
    suspend fun getComic(id: Long): Comic?

    suspend fun importComics(uris: List<android.net.Uri>): ImportSummary
    suspend fun importComicFolder(treeUri: android.net.Uri): ImportSummary

    suspend fun pages(comic: Comic): List<ComicPage>

    suspend fun renameComic(id: Long, title: String)
    suspend fun updateCoverPath(id: Long, coverPath: String?)
    suspend fun generateCoverFromPage(id: Long, pageKey: String): String?
    suspend fun generateCoverFromUri(id: Long, uri: android.net.Uri): String?
    suspend fun deleteComic(id: Long)
    suspend fun saveProgress(id: Long, page: Int)

    suspend fun addToCategory(comicId: Long, categoryId: Long)
    suspend fun removeFromCategory(comicId: Long, categoryId: Long)
    suspend fun categoryIds(comicId: Long): List<Long>
}

interface VideoRepository {
    fun observeVideos(): Flow<List<Video>>
    fun observeVideosByCategory(categoryId: Long): Flow<List<Video>>
    fun observeVideo(id: Long): Flow<Video?>
    suspend fun getVideo(id: Long): Video?

    suspend fun importVideos(uris: List<android.net.Uri>): ImportSummary
    suspend fun importVideoFolder(treeUri: android.net.Uri): ImportSummary

    suspend fun renameVideo(id: Long, title: String)
    suspend fun deleteVideo(id: Long)

    suspend fun addToCategory(videoId: Long, categoryId: Long)
    suspend fun removeFromCategory(videoId: Long, categoryId: Long)
    suspend fun categoryIds(videoId: Long): List<Long>

    fun observePlaybackPosition(videoId: Long): Flow<Long>
    suspend fun savePlaybackPosition(videoId: Long, positionMs: Long)
}

interface CategoryRepository {
    fun observeCategories(type: CategoryType): Flow<List<Category>>
    suspend fun getCategories(type: CategoryType): List<Category>
    suspend fun createCategory(name: String, type: CategoryType): Category
    suspend fun renameCategory(id: Long, name: String)
    suspend fun deleteCategory(id: Long)
}

interface SettingsRepository {
    val settings: Flow<com.readplusnext.domain.model.AppSettings>
    suspend fun setThemeStyle(style: com.readplusnext.domain.model.ThemeStyle)
    suspend fun setThemeMode(mode: com.readplusnext.domain.model.ThemeMode)
    suspend fun setReadingMode(mode: com.readplusnext.domain.model.ReadingMode)
    suspend fun setComicGridColumns(columns: Int)
    suspend fun setVideoGridColumns(columns: Int)
}

interface BackupRepository {
    val progress: StateFlow<BackupProgress>
    suspend fun export(toUri: android.net.Uri): Result<Unit>
    suspend fun import(fromUri: android.net.Uri): Result<Unit>
}
