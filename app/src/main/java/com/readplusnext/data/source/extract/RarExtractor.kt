package com.readplusnext.data.source.extract

import com.github.junrar.Archive
import com.readplusnext.core.util.NaturalOrderComparator
import com.readplusnext.data.source.ComicImportException
import com.readplusnext.data.source.ImageFormats
import java.io.File
import java.util.Locale

/** Extracts RAR4/CBR archives with junrar and rewrites them into the normalised page ZIP. */
internal object RarExtractor {

    fun extract(rar: File, outputDir: File): List<File> {
        val archive = try {
            Archive(rar)
        } catch (error: Exception) {
            throw ComicImportException(
                "无法打开 RAR/CBR 文件，可能不是 RAR4 格式：${error.message.orEmpty()}",
                error,
            )
        }
        try {
            if (archive.isEncrypted) {
                throw ComicImportException("该 RAR/CBR 已加密，无法导入")
            }
            val headers = archive.fileHeaders
                .filter { !it.isDirectory && ImageFormats.isImageName(it.fileName) }
            if (headers.isEmpty()) {
                throw ComicImportException("RAR/CBR 中未找到可显示的图片")
            }
            if (headers.any { it.isSplitBefore || it.isSplitAfter }) {
                throw ComicImportException("不支持分卷 RAR/CBR 压缩包")
            }
            val sorted = headers.sortedWith { a, b ->
                NaturalOrderComparator.compare(a.fileName, b.fileName)
            }
            return sorted.mapIndexed { index, header ->
                val target = File(
                    outputDir,
                    "%05d.%s".format(Locale.US, index, ImageFormats.extensionFor(header.fileName)),
                )
                archive.getInputStream(header).use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
                target
            }
        } catch (error: ComicImportException) {
            throw error
        } catch (error: Exception) {
            throw ComicImportException("RAR/CBR 解压失败：${error.message.orEmpty()}", error)
        } finally {
            runCatching { archive.close() }
        }
    }
}
