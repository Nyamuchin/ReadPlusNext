package com.readplusnext.data.source.extract

import com.readplusnext.core.util.NaturalOrderComparator
import com.readplusnext.data.source.ComicImportException
import com.readplusnext.data.source.ImageFormats
import java.io.File
import java.util.Locale
import org.apache.commons.compress.archivers.sevenz.SevenZFile

/** Streams images out of 7z/CB7 archives without holding the archive in memory. */
internal object SevenZExtractor {

    fun extract(archiveFile: File, outputDir: File): List<File> {
        val pages = mutableListOf<Pair<String, File>>()
        try {
            SevenZFile.builder().setFile(archiveFile).get().use { sevenZ ->
                var entry = sevenZ.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && ImageFormats.isImageName(entry.name)) {
                        val target = File(
                            outputDir,
                            "%05d.%s".format(Locale.US, pages.size, ImageFormats.extensionFor(entry.name)),
                        )
                        sevenZ.getInputStream(entry).use { input ->
                            target.outputStream().use { output -> input.copyTo(output) }
                        }
                        pages += entry.name to target
                    }
                    entry = sevenZ.nextEntry
                }
            }
        } catch (error: ComicImportException) {
            throw error
        } catch (error: Exception) {
            throw ComicImportException("7z/CB7 解压失败：${error.message.orEmpty()}", error)
        }
        if (pages.isEmpty()) throw ComicImportException("7z/CB7 中未找到可显示的图片")
        return pages
            .sortedWith { a, b -> NaturalOrderComparator.compare(a.first, b.first) }
            .map { it.second }
    }
}
