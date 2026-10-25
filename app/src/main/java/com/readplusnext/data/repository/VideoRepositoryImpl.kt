package com.readplusnext.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.readplusnext.core.util.NaturalOrderComparator
import com.readplusnext.core.util.extensionOf
import com.readplusnext.data.local.entity.VideoEntity
import com.readplusnext.data.local.dao.VideoDao
import com.readplusnext.data.local.toDomain
import com.readplusnext.data.preferences.PreferencesManager
import com.readplusnext.data.source.UriUtils
import com.readplusnext.data.source.VideoCoverStore
import com.readplusnext.domain.model.ImportSource
import com.readplusnext.domain.model.ImportSummary
import com.readplusnext.domain.model.Video
import com.readplusnext.domain.repository.VideoRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class VideoRepositoryImpl @Inject constructor(
    private val videoDao: VideoDao,
    private val coverStore: VideoCoverStore,
    private val preferences: PreferencesManager,
    @ApplicationContext private val context: Context,
) : VideoRepository {

    override fun observeVideos(): Flow<List<Video>> =
        videoDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeVideosByCategory(categoryId: Long): Flow<List<Video>> =
        videoDao.observeByCategory(categoryId).map { entities -> entities.map { it.toDomain() } }

    override fun observeVideo(id: Long): Flow<Video?> =
        videoDao.observeById(id).map { it?.toDomain() }

    override suspend fun getVideo(id: Long): Video? = videoDao.getById(id)?.toDomain()

    override suspend fun importVideos(uris: List<Uri>): ImportSummary = withContext(Dispatchers.IO) {
        var imported = 0
        var failed = 0
        uris.forEach { uri ->
            if (importOne(uri, folderUri = null)) imported++ else failed++
        }
        ImportSummary(imported = imported, failed = failed)
    }

    override suspend fun importVideoFolder(treeUri: Uri): ImportSummary = withContext(Dispatchers.IO) {
        retainReadPermission(treeUri)
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: return@withContext ImportSummary(imported = 0, failed = 1)
        val files = collectVideoFiles(root)
        if (files.isEmpty()) return@withContext ImportSummary(imported = 0, failed = 1)
        var imported = 0
        var failed = 0
        files.forEach { file ->
            if (importOne(file.uri, folderUri = treeUri.toString())) imported++ else failed++
        }
        ImportSummary(imported = imported, failed = failed)
    }

    override suspend fun renameVideo(id: Long, title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        videoDao.updateTitle(id, trimmed)
    }

    override suspend fun deleteVideo(id: Long) = withContext(Dispatchers.IO) {
        val entity = videoDao.getById(id) ?: return@withContext
        coverStore.delete(entity.coverPath)
        preferences.clearPlaybackPosition(id)
        videoDao.deleteById(id)
    }

    override suspend fun addToCategory(videoId: Long, categoryId: Long) = withContext(Dispatchers.IO) {
        videoDao.insertCrossRef(
            com.readplusnext.data.local.entity.VideoCategoryCrossRef(
                videoId = videoId,
                categoryId = categoryId,
            ),
        )
    }

    override suspend fun removeFromCategory(videoId: Long, categoryId: Long) =
        withContext(Dispatchers.IO) {
            videoDao.deleteCrossRef(videoId, categoryId)
        }

    override suspend fun categoryIds(videoId: Long): List<Long> = withContext(Dispatchers.IO) {
        videoDao.categoryIds(videoId)
    }

    override fun observePlaybackPosition(videoId: Long): Flow<Long> =
        preferences.observePlaybackPosition(videoId)

    override suspend fun savePlaybackPosition(videoId: Long, positionMs: Long) {
        preferences.savePlaybackPosition(videoId, positionMs)
    }

    private suspend fun importOne(uri: Uri, folderUri: String?): Boolean = try {
        if (!isSupportedVideo(uri)) throw IllegalArgumentException("unsupported video")
        val displayName = UriUtils.displayName(context, uri) ?: "未命名视频"
        val metadata = coverStore.readMetadata(uri)
        val size = UriUtils.size(context, uri) ?: 0L
        videoDao.insert(
            VideoEntity(
                title = displayName.substringBeforeLast('.', displayName),
                uri = uri.toString(),
                duration = metadata.durationMs,
                size = size,
                coverPath = metadata.coverPath,
                folderUri = folderUri,
                importSource = if (folderUri != null) ImportSource.FOLDER.name else ImportSource.MANUAL.name,
                createdAt = System.currentTimeMillis(),
            ),
        )
        true
    } catch (error: Exception) {
        Log.w(TAG, "Failed to import video $uri", error)
        false
    }

    private fun isSupportedVideo(uri: Uri): Boolean {
        val name = UriUtils.displayName(context, uri) ?: uri.lastPathSegment ?: return false
        return name.extensionOf() in VIDEO_EXTENSIONS
    }

    private fun collectVideoFiles(root: DocumentFile): List<DocumentFile> {
        val result = mutableListOf<DocumentFile>()
        val pending = ArrayDeque<DocumentFile>()
        pending.addLast(root)
        while (pending.isNotEmpty()) {
            val directory = pending.removeLast()
            directory.listFiles().forEach { child ->
                when {
                    child.isDirectory -> pending.addLast(child)
                    (child.name?.extensionOf() ?: "") in VIDEO_EXTENSIONS -> result += child
                }
            }
        }
        return result.sortedWith { a, b -> NaturalOrderComparator.compare(a.name ?: "", b.name ?: "") }
    }

    private fun retainReadPermission(treeUri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }

    private companion object {
        const val TAG = "VideoRepository"
        val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "webm", "avi")
    }
}
