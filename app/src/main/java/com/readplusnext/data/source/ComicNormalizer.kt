package com.readplusnext.data.source

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.readplusnext.core.util.withoutExtension
import com.readplusnext.data.source.extract.ArchiveExtractionUtils
import com.readplusnext.data.source.extract.EpubExtractor
import com.readplusnext.data.source.extract.MobiExtractor
import com.readplusnext.data.source.extract.RarExtractor
import com.readplusnext.data.source.extract.SevenZExtractor
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Turns any supported comic container into the single page source the app understands:
 * a ZIP whose entries are `page_00000.jpg`, `page_00001.png`, ... (or the original PDF).
 * Everything runs on [Dispatchers.IO], temporary files always get cleaned up and every
 * failure is reported as a [ComicImportException].
 */
@Singleton
class ComicNormalizer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val zipPageSource: ZipPageSource,
) {

    data class NormalizedComic(
        val sourcePath: String,
        val isPdf: Boolean,
        val pageCount: Int,
        val title: String,
    )

    suspend fun normalize(uri: Uri, displayName: String?): NormalizedComic = withContext(Dispatchers.IO) {
        val name = displayName?.takeIf { it.isNotBlank() } ?: DEFAULT_TITLE
        val comicsDir = File(context.filesDir, COMICS_DIR).apply { mkdirs() }
        val workDir = File(context.cacheDir, "import_work/${UUID.randomUUID()}").apply { mkdirs() }
        val input = File(workDir, "source.bin")
        try {
            copyTo(input, uri)
            var format = ArchiveFormatDetector.detect(input, name)
            if (format == ArchiveFormat.ZIP && containsEpubContainer(input)) {
                format = ArchiveFormat.EPUB
            }
            val title = name.withoutExtension().ifBlank { DEFAULT_TITLE }
            val id = UUID.randomUUID().toString()

            when (format) {
                ArchiveFormat.ZIP -> {
                    val pageCount = zipPageSource.countImages(input)
                    if (pageCount <= 0) throw ComicImportException("压缩包中未找到可显示的图片")
                    val target = File(comicsDir, "$id.zip")
                    input.copyTo(target, overwrite = true)
                    NormalizedComic(target.absolutePath, isPdf = false, pageCount = pageCount, title = title)
                }

                ArchiveFormat.PDF -> {
                    val pageCount = pdfPageCount(input)
                    if (pageCount <= 0) throw ComicImportException("PDF 没有可显示的页面")
                    val target = File(comicsDir, "$id.pdf")
                    input.copyTo(target, overwrite = true)
                    NormalizedComic(target.absolutePath, isPdf = true, pageCount = pageCount, title = title)
                }

                ArchiveFormat.RAR,
                ArchiveFormat.SEVEN_Z,
                ArchiveFormat.EPUB,
                ArchiveFormat.MOBI,
                -> {
                    val pagesDir = File(workDir, "pages").apply { mkdirs() }
                    val pages = when (format) {
                        ArchiveFormat.RAR -> RarExtractor.extract(input, pagesDir)
                        ArchiveFormat.SEVEN_Z -> SevenZExtractor.extract(input, pagesDir)
                        ArchiveFormat.EPUB -> EpubExtractor.extract(input, pagesDir)
                        else -> MobiExtractor.extract(input, pagesDir)
                    }
                    if (pages.isEmpty()) throw ComicImportException("未找到可显示的图片页面")
                    val target = File(comicsDir, "$id.zip")
                    ArchiveExtractionUtils.writeZip(pages, target)
                    NormalizedComic(target.absolutePath, isPdf = false, pageCount = pages.size, title = title)
                }

                ArchiveFormat.UNKNOWN -> throw ComicImportException("不支持的文件格式：$name")
            }
        } finally {
            workDir.deleteRecursively()
        }
    }

    private fun copyTo(target: File, uri: Uri) {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw ComicImportException("无法读取所选文件")
        input.use { source ->
            target.outputStream().use { destination -> source.copyTo(destination) }
        }
        if (target.length() <= 0L) throw ComicImportException("所选文件为空")
    }

    private fun containsEpubContainer(zip: File): Boolean = runCatching {
        ZipFile(zip).use { it.getEntry("META-INF/container.xml") != null }
    }.getOrDefault(false)

    private fun pdfPageCount(file: File): Int {
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(descriptor)
        return try {
            renderer.pageCount
        } finally {
            runCatching { renderer.close() }
            runCatching { descriptor.close() }
        }
    }

    private companion object {
        const val COMICS_DIR = "comics"
        const val DEFAULT_TITLE = "未命名漫画"
    }
}
