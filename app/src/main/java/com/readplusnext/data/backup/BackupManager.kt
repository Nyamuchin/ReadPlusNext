package com.readplusnext.data.backup

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.google.gson.Gson
import com.readplusnext.data.local.ReadPlusNextDatabase
import com.readplusnext.data.local.entity.CategoryEntity
import com.readplusnext.data.local.entity.ComicCategoryCrossRef
import com.readplusnext.data.local.entity.ComicEntity
import com.readplusnext.data.local.entity.VideoCategoryCrossRef
import com.readplusnext.data.local.entity.VideoEntity
import com.readplusnext.data.source.UriUtils
import com.readplusnext.domain.model.BackupProgress
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: ReadPlusNextDatabase,
) {

    private val gson = Gson()
    private val _progress = MutableStateFlow(BackupProgress())
    val progress: StateFlow<BackupProgress> = _progress.asStateFlow()

    suspend fun export(toUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            _progress.value = BackupProgress(running = true, phase = "正在收集数据")
            val comicDao = database.comicDao()
            val videoDao = database.videoDao()
            val categoryDao = database.categoryDao()

            val comics = comicDao.getAll()
            val videos = videoDao.getAll()
            val categories = categoryDao.getAll()
            val comicRefs = comicDao.getAllCrossRefs()
            val videoRefs = videoDao.getAllCrossRefs()

            val files = mutableListOf<BackupFile>()
            val workDir = File(context.cacheDir, "backup_work").apply {
                deleteRecursively()
                mkdirs()
            }
            val zipFile = File(workDir, "backup.zip")
            try {
                val backupComics = comics.map { comic ->
                    val mediaFile = File(comic.zipPath)
                    val mediaEntry = if (mediaFile.isFile) {
                        val entry = "comics/${comic.id}_${safeName(mediaFile.name)}"
                        files += BackupFile(entry, file = mediaFile)
                        entry
                    } else {
                        ""
                    }
                    val coverEntry = comic.coverPath?.let { path ->
                        val file = File(path)
                        if (file.isFile) {
                            val entry = "covers/${comic.id}_${safeName(file.name)}"
                            files += BackupFile(entry, file = file)
                            entry
                        } else {
                            null
                        }
                    }
                    BackupComic(
                        id = comic.id,
                        title = comic.title,
                        originalUri = comic.originalUri,
                        mediaPath = mediaEntry,
                        pageCount = comic.pageCount,
                        coverPath = coverEntry,
                        lastReadPage = comic.lastReadPage,
                        lastReadAt = comic.lastReadAt,
                        createdAt = comic.createdAt,
                    )
                }

                val backupVideos = videos.map { video ->
                    val mediaEntry = copyableVideoEntry(video)?.let { (entry, file) ->
                        files += BackupFile(entry, file = file)
                        entry
                    }
                    val streamEntry = if (mediaEntry == null) {
                        val uri = runCatching { Uri.parse(video.uri) }.getOrNull()
                        val displayName = uri?.let { UriUtils.displayName(context, it) }
                        val entry = "videos/${video.id}_${safeName(displayName ?: "video.mp4")}"
                        files += BackupFile(entry, uri = uri)
                        entry
                    } else {
                        null
                    }
                    val coverEntry = video.coverPath?.let { path ->
                        val file = File(path)
                        if (file.isFile) {
                            val entry = "video_covers/${video.id}_${safeName(file.name)}"
                            files += BackupFile(entry, file = file)
                            entry
                        } else {
                            null
                        }
                    }
                    BackupVideo(
                        id = video.id,
                        title = video.title,
                        uri = video.uri,
                        mediaPath = mediaEntry ?: streamEntry,
                        duration = video.duration,
                        size = video.size,
                        coverPath = coverEntry,
                        folderUri = video.folderUri,
                        importSource = video.importSource,
                        createdAt = video.createdAt,
                    )
                }

                val payload = BackupPayload(
                    version = BACKUP_VERSION,
                    exportedAt = System.currentTimeMillis(),
                    comics = backupComics,
                    videos = backupVideos,
                    categories = categories.map {
                        BackupCategory(it.id, it.name, it.type, it.sortOrder, it.createdAt)
                    },
                    comicRefs = comicRefs.map { BackupComicRef(it.comicId, it.categoryId) },
                    videoRefs = videoRefs.map { BackupVideoRef(it.videoId, it.categoryId) },
                )

                val total = files.size + 1
                var processed = 0
                _progress.value = BackupProgress(true, "正在写入备份", 0, total)
                ZipOutputStream(zipFile.outputStream().buffered()).use { zip ->
                    zip.putNextEntry(ZipEntry(DATA_ENTRY))
                    zip.write(gson.toJson(payload).toByteArray())
                    zip.closeEntry()
                    processed++
                    for (backupFile in files) {
                        _progress.value = BackupProgress(true, "正在写入备份", processed, total)
                        val input = backupFile.file?.inputStream()
                            ?: backupFile.uri?.let {
                                context.contentResolver.openInputStream(it)
                            }
                            ?: throw IOException("无法读取媒体文件：${backupFile.entry}")
                        zip.putNextEntry(ZipEntry(backupFile.entry))
                        input.use { stream -> stream.copyTo(zip, DEFAULT_BUFFER_SIZE) }
                        zip.closeEntry()
                        processed++
                    }
                    _progress.value = BackupProgress(true, "正在写入备份", processed, total)
                }

                _progress.value = BackupProgress(true, "正在保存备份文件", total, total)
                val output = context.contentResolver.openOutputStream(toUri)
                    ?: throw IOException("无法创建备份文件")
                output.use { stream -> zipFile.inputStream().use { it.copyTo(stream, DEFAULT_BUFFER_SIZE) } }
                _progress.value = BackupProgress(false, "备份完成", total, total)
            } finally {
                workDir.deleteRecursively()
            }
        }.onFailure { error ->
            _progress.value = BackupProgress(false, "备份失败：${error.message}", 0, 0)
        }
    }

    suspend fun import(fromUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            _progress.value = BackupProgress(true, "正在读取备份文件", 0, 0)
            val workDir = File(context.cacheDir, "restore_work").apply {
                deleteRecursively()
                mkdirs()
            }
            val zipFile = File(workDir, "backup.zip")
            try {
                context.contentResolver.openInputStream(fromUri)?.use { input ->
                    zipFile.outputStream().use { output ->
                        input.copyTo(output, DEFAULT_BUFFER_SIZE)
                    }
                } ?: throw IOException("无法读取备份文件")

                ZipFile(zipFile).use { zip ->
                    val dataEntry = zip.getEntry(DATA_ENTRY)
                        ?: throw IOException("备份文件缺少数据清单")
                    val payload = zip.getInputStream(dataEntry).use { stream ->
                        gson.fromJson(stream.reader(Charsets.UTF_8), BackupPayload::class.java)
                    } ?: throw IOException("备份数据损坏")

                    val mediaEntries = zip.entries().toList()
                        .filter { !it.isDirectory && it.name != DATA_ENTRY }
                    val total = mediaEntries.size + 1
                    var processed = 0

                    listOf("comics", "covers", "videos", "video_covers", "thumbs").forEach { dir ->
                        File(context.filesDir, dir).deleteRecursively()
                    }

                    for (entry in mediaEntries) {
                        val target = File(context.filesDir, entry.name)
                        val canonical = target.canonicalFile
                        if (!canonical.path.startsWith(context.filesDir.canonicalFile.path)) {
                            throw IOException("备份文件包含非法路径：${entry.name}")
                        }
                        canonical.parentFile?.mkdirs()
                        zip.getInputStream(entry).use { input ->
                            canonical.outputStream().use { output ->
                                input.copyTo(output, DEFAULT_BUFFER_SIZE)
                            }
                        }
                        processed++
                        _progress.value = BackupProgress(true, "正在恢复文件", processed, total)
                    }

                    val comics = payload.comics.map { comic ->
                        ComicEntity(
                            id = comic.id,
                            title = comic.title,
                            originalUri = comic.originalUri,
                            zipPath = File(context.filesDir, comic.mediaPath).absolutePath,
                            pageCount = comic.pageCount,
                            coverPath = comic.coverPath?.let { File(context.filesDir, it).absolutePath },
                            lastReadPage = comic.lastReadPage,
                            lastReadAt = comic.lastReadAt,
                            createdAt = comic.createdAt,
                        )
                    }
                    val videos = payload.videos.map { video ->
                        val mediaPath = video.mediaPath
                        val uri = if (!mediaPath.isNullOrEmpty()) {
                            Uri.fromFile(File(context.filesDir, mediaPath)).toString()
                        } else {
                            video.uri
                        }
                        VideoEntity(
                            id = video.id,
                            title = video.title,
                            uri = uri,
                            duration = video.duration,
                            size = video.size,
                            coverPath = video.coverPath?.let { File(context.filesDir, it).absolutePath },
                            folderUri = video.folderUri,
                            importSource = video.importSource,
                            createdAt = video.createdAt,
                        )
                    }
                    val categories = payload.categories.map {
                        CategoryEntity(it.id, it.name, it.type, it.sortOrder, it.createdAt)
                    }

                    _progress.value = BackupProgress(true, "正在恢复数据", total, total)
                    database.withTransaction {
                        database.comicDao().deleteAll()
                        database.videoDao().deleteAll()
                        database.categoryDao().deleteAll()
                        database.categoryDao().insertAll(categories)
                        database.comicDao().insertAll(comics)
                        database.videoDao().insertAll(videos)
                        payload.comicRefs.forEach { ref ->
                            database.comicDao().insertCrossRef(
                                ComicCategoryCrossRef(ref.comicId, ref.categoryId),
                            )
                        }
                        payload.videoRefs.forEach { ref ->
                            database.videoDao().insertCrossRef(
                                VideoCategoryCrossRef(ref.videoId, ref.categoryId),
                            )
                        }
                    }
                    _progress.value = BackupProgress(false, "恢复完成", total, total)
                }
            } finally {
                workDir.deleteRecursively()
            }
        }.onFailure { error ->
            _progress.value = BackupProgress(false, "恢复失败：${error.message}", 0, 0)
        }
    }

    private fun copyableVideoEntry(video: VideoEntity): Pair<String, File>? {
        val uri = runCatching { Uri.parse(video.uri) }.getOrNull() ?: return null
        if (uri.scheme != ContentResolver.SCHEME_FILE) return null
        val file = uri.path?.let(::File)?.takeIf { it.isFile } ?: return null
        return "videos/${video.id}_${safeName(file.name)}" to file
    }

    private fun safeName(name: String): String =
        name.replace(Regex("[^A-Za-z0-9._-]"), "_").take(120).ifEmpty { "file" }

    private data class BackupFile(
        val entry: String,
        val file: File? = null,
        val uri: Uri? = null,
    )

    private data class BackupPayload(
        val version: Int,
        val exportedAt: Long,
        val comics: List<BackupComic>,
        val videos: List<BackupVideo>,
        val categories: List<BackupCategory>,
        val comicRefs: List<BackupComicRef>,
        val videoRefs: List<BackupVideoRef>,
    )

    private data class BackupComic(
        val id: Long,
        val title: String,
        val originalUri: String,
        val mediaPath: String,
        val pageCount: Int,
        val coverPath: String?,
        val lastReadPage: Int,
        val lastReadAt: Long,
        val createdAt: Long,
    )

    private data class BackupVideo(
        val id: Long,
        val title: String,
        val uri: String,
        val mediaPath: String?,
        val duration: Long,
        val size: Long,
        val coverPath: String?,
        val folderUri: String?,
        val importSource: String,
        val createdAt: Long,
    )

    private data class BackupCategory(
        val id: Long,
        val name: String,
        val type: String,
        val sortOrder: Int,
        val createdAt: Long,
    )

    private data class BackupComicRef(val comicId: Long, val categoryId: Long)

    private data class BackupVideoRef(val videoId: Long, val categoryId: Long)

    private companion object {
        const val BACKUP_VERSION = 1
        const val DATA_ENTRY = "data.json"
    }
}
