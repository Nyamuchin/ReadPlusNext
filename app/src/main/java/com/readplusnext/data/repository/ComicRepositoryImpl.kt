package com.readplusnext.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.readplusnext.core.util.NaturalOrderComparator
import com.readplusnext.core.util.extensionOf
import com.readplusnext.core.util.md5Hex
import com.readplusnext.data.local.entity.ComicEntity
import com.readplusnext.data.local.dao.ComicDao
import com.readplusnext.data.local.toDomain
import com.readplusnext.data.source.ArchiveFormatDetector
import com.readplusnext.data.source.ComicNormalizer
import com.readplusnext.data.source.ComicPageSource
import com.readplusnext.data.source.CoverStore
import com.readplusnext.data.source.UriUtils
import com.readplusnext.domain.model.Comic
import com.readplusnext.domain.model.ComicPage
import com.readplusnext.domain.model.ImportSummary
import com.readplusnext.domain.repository.ComicRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class ComicRepositoryImpl @Inject constructor(
    private val comicDao: ComicDao,
    private val normalizer: ComicNormalizer,
    private val pageSource: ComicPageSource,
    private val coverStore: CoverStore,
    @ApplicationContext private val context: Context,
) : ComicRepository {

    override fun observeComics(): Flow<List<Comic>> =
        comicDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeComicsByCategory(categoryId: Long): Flow<List<Comic>> =
        comicDao.observeByCategory(categoryId).map { entities -> entities.map { it.toDomain() } }

    override fun observeComic(id: Long): Flow<Comic?> =
        comicDao.observeById(id).map { it?.toDomain() }

    override suspend fun getComic(id: Long): Comic? = comicDao.getById(id)?.toDomain()

    override suspend fun importComics(uris: List<Uri>): ImportSummary = withContext(Dispatchers.IO) {
        var imported = 0
        var failed = 0
        uris.forEach { uri ->
            if (importOne(uri)) imported++ else failed++
        }
        ImportSummary(imported = imported, failed = failed)
    }

    override suspend fun importComicFolder(treeUri: Uri): ImportSummary = withContext(Dispatchers.IO) {
        retainReadPermission(treeUri)
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: return@withContext ImportSummary(imported = 0, failed = 1)
        val files = collectComicFiles(root)
        if (files.isEmpty()) return@withContext ImportSummary(imported = 0, failed = 1)
        var imported = 0
        var failed = 0
        files.forEach { file ->
            if (importOne(file.uri)) imported++ else failed++
        }
        ImportSummary(imported = imported, failed = failed)
    }

    override suspend fun pages(comic: Comic): List<ComicPage> = withContext(Dispatchers.IO) {
        pageSource.pageKeys(comic).mapIndexed { index, key -> ComicPage(index = index, key = key) }
    }

    override suspend fun renameComic(id: Long, title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        comicDao.updateTitle(id, trimmed)
    }

    override suspend fun updateCoverPath(id: Long, coverPath: String?) {
        val entity = comicDao.getById(id) ?: return
        if (entity.coverPath != null && entity.coverPath != coverPath) {
            coverStore.delete(entity.coverPath)
        }
        comicDao.updateCoverPath(id, coverPath)
    }

    override suspend fun generateCoverFromPage(id: Long, pageKey: String): String? =
        withContext(Dispatchers.IO) {
            val entity = comicDao.getById(id) ?: return@withContext null
            val cover = pageSource.createCover(entity.toDomain(), pageKey) ?: return@withContext null
            coverStore.delete(entity.coverPath)
            comicDao.updateCoverPath(id, cover)
            cover
        }

    override suspend fun generateCoverFromUri(id: Long, uri: Uri): String? =
        withContext(Dispatchers.IO) {
            val entity = comicDao.getById(id) ?: return@withContext null
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext null
            val extension = (UriUtils.displayName(context, uri) ?: "cover.jpg").extensionOf()
            val cover = coverStore.saveBytes(bytes, extension) ?: return@withContext null
            coverStore.delete(entity.coverPath)
            comicDao.updateCoverPath(id, cover)
            cover
        }

    override suspend fun deleteComic(id: Long) = withContext(Dispatchers.IO) {
        val entity = comicDao.getById(id) ?: return@withContext
        runCatching { File(entity.zipPath).delete() }
        coverStore.delete(entity.coverPath)
        runCatching {
            File(File(context.filesDir, "thumbs"), entity.zipPath.md5Hex()).deleteRecursively()
        }
        comicDao.deleteById(id)
    }

    override suspend fun saveProgress(id: Long, page: Int) {
        comicDao.updateProgress(id, page.coerceAtLeast(0), System.currentTimeMillis())
    }

    override suspend fun addToCategory(comicId: Long, categoryId: Long) = withContext(Dispatchers.IO) {
        comicDao.insertCrossRef(
            com.readplusnext.data.local.entity.ComicCategoryCrossRef(
                comicId = comicId,
                categoryId = categoryId,
            ),
        )
    }

    override suspend fun removeFromCategory(comicId: Long, categoryId: Long) =
        withContext(Dispatchers.IO) {
            comicDao.deleteCrossRef(comicId, categoryId)
        }

    override suspend fun categoryIds(comicId: Long): List<Long> = withContext(Dispatchers.IO) {
        comicDao.categoryIds(comicId)
    }

    private suspend fun importOne(uri: Uri): Boolean = try {
        val displayName = UriUtils.displayName(context, uri)
        val normalized = normalizer.normalize(uri, displayName)
        val cover = runCatching {
            pageSource.createCover(normalized.sourcePath, normalized.isPdf)
        }.getOrNull()
        comicDao.insert(
            ComicEntity(
                title = normalized.title,
                originalUri = uri.toString(),
                zipPath = normalized.sourcePath,
                pageCount = normalized.pageCount,
                coverPath = cover,
                lastReadPage = 0,
                lastReadAt = 0L,
                createdAt = System.currentTimeMillis(),
            ),
        )
        true
    } catch (error: Exception) {
        Log.w(TAG, "Failed to import comic $uri", error)
        false
    }

    private fun collectComicFiles(root: DocumentFile): List<DocumentFile> {
        val result = mutableListOf<DocumentFile>()
        val pending = ArrayDeque<DocumentFile>()
        pending.addLast(root)
        while (pending.isNotEmpty()) {
            val directory = pending.removeLast()
            directory.listFiles().forEach { child ->
                when {
                    child.isDirectory -> pending.addLast(child)
                    ArchiveFormatDetector.isSupportedName(child.name ?: "") -> result += child
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
        const val TAG = "ComicRepository"
    }
}
