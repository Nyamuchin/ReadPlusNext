package com.readplusnext.data.source

import android.graphics.Bitmap
import com.readplusnext.domain.model.Comic
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Domain facing page access: page keys for a comic and decoding/ rendering of a single page.
 * Zip pages are returned as raw encoded bytes so Coil can sample them; PDF pages are rendered
 * through PdfRenderer (serialised with a lock because PdfRenderer is not thread safe).
 */
@Singleton
class ComicPageSource @Inject constructor(
    private val zipPageSource: ZipPageSource,
    private val coverStore: CoverStore,
) {

    private val pdfLock = Any()

    fun pageCount(comic: Comic): Int = pageCount(File(comic.zipPath), comic.isPdf)

    fun pageCount(file: File, isPdf: Boolean): Int =
        if (isPdf) PdfPageRenderer.pageCount(file) else zipPageSource.countImages(file)

    fun pageKeys(comic: Comic): List<String> = pageKeys(File(comic.zipPath), comic.isPdf)

    fun pageKeys(file: File, isPdf: Boolean): List<String> =
        if (isPdf) {
            (0 until PdfPageRenderer.pageCount(file)).map { it.toString() }
        } else {
            zipPageSource.imageEntries(file)
        }

    fun readZipPageBytes(comic: Comic, pageKey: String): ByteArray? =
        readZipPageBytes(File(comic.zipPath), pageKey)

    fun readZipPageBytes(file: File, pageKey: String): ByteArray? = zipPageSource.readEntry(file, pageKey)

    fun mimeTypeFor(comic: Comic, pageKey: String): String =
        if (comic.isPdf) "image/png" else ImageFormats.mimeTypeFor(pageKey)

    fun renderPdfPage(comic: Comic, pageKey: String, targetWidth: Int): Bitmap? =
        renderPdfPage(File(comic.zipPath), pageKey, targetWidth)

    fun renderPdfPage(file: File, pageKey: String, targetWidth: Int): Bitmap? {
        val pageIndex = pageKey.toIntOrNull() ?: return null
        return synchronized(pdfLock) { PdfPageRenderer.render(file, pageIndex, targetWidth) }
    }

    /** Writes a cover file from [pageKey] (or the first page) and returns its path. */
    fun createCover(comic: Comic, pageKey: String? = null): String? {
        val file = File(comic.zipPath)
        return if (comic.isPdf) {
            val bitmap = renderPdfPage(file, pageKey ?: "0", COVER_WIDTH) ?: return null
            try {
                coverStore.saveBitmap(bitmap)
            } finally {
                bitmap.recycle()
            }
        } else {
            val key = pageKey ?: zipPageSource.firstImageEntry(file) ?: return null
            val bytes = zipPageSource.readEntry(file, key) ?: return null
            coverStore.saveBytes(bytes, ImageFormats.extensionFor(key))
        }
    }

    fun createCover(sourcePath: String, isPdf: Boolean): String? = createCover(
        Comic(
            id = 0L,
            title = "",
            originalUri = "",
            zipPath = sourcePath,
            pageCount = 0,
            coverPath = null,
            lastReadPage = 0,
            lastReadAt = 0L,
            createdAt = 0L,
        ),
        pageKey = if (isPdf) "0" else null,
    )

    private companion object {
        const val COVER_WIDTH = 720
    }
}
