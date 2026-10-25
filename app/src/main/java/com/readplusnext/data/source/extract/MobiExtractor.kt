package com.readplusnext.data.source.extract

import com.readplusnext.data.source.ComicImportException
import com.readplusnext.data.source.ImageFormats
import java.io.File
import java.io.RandomAccessFile
import java.util.Locale

/**
 * PalmDB/MOBI books keep images as raw records, so the PDB record table is walked and every
 * record whose header looks like an image is copied out. DRM protected books are rejected.
 */
internal object MobiExtractor {

    private const val RECORD_COUNT_OFFSET = 76L
    private const val RECORD_TABLE_OFFSET = 78L
    private const val HUFF_COMPRESSION = 17480

    fun extract(mobi: File, outputDir: File): List<File> {
        RandomAccessFile(mobi, "r").use { file ->
            val fileLength = file.length()
            if (fileLength < RECORD_TABLE_OFFSET + 8) {
                throw ComicImportException("文件过小，不是有效的 MOBI/AZW 文件")
            }
            val recordCount = readUInt16(file, RECORD_COUNT_OFFSET)
            if (recordCount <= 0) throw ComicImportException("MOBI/AZW 记录表无效")

            val offsets = LongArray(recordCount) { index ->
                readUInt32(file, RECORD_TABLE_OFFSET + index * 8L)
            }
            val firstRecord = offsets[0]
            if (firstRecord <= 0 || firstRecord >= fileLength) {
                throw ComicImportException("MOBI/AZW 文件头无效")
            }
            val compression = readUInt16(file, firstRecord)
            val encryption = readUInt16(file, firstRecord + 12)
            if (encryption != 0) {
                throw ComicImportException("该 MOBI/AZW 文件受 DRM 保护，无法导入")
            }
            if (compression == HUFF_COMPRESSION) {
                throw ComicImportException("该 MOBI/AZW 使用了 HUFF/CDIC 压缩，暂不支持")
            }

            val pages = mutableListOf<File>()
            for (index in 1 until recordCount) {
                val start = offsets[index]
                if (start <= 0 || start >= fileLength) continue
                val next = if (index + 1 < recordCount) offsets[index + 1] else fileLength
                val end = if (next in (start + 1)..fileLength) next else fileLength
                val size = (end - start).toInt()
                if (size <= 0) continue

                val header = ByteArray(minOf(size, 16))
                file.seek(start)
                file.readFully(header)
                val extension = ImageFormats.extensionForMagic(header) ?: continue

                val target = File(outputDir, "%05d.%s".format(Locale.US, pages.size, extension))
                file.seek(start)
                target.outputStream().use { output ->
                    var remaining = size
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (remaining > 0) {
                        val read = file.read(buffer, 0, minOf(remaining, buffer.size))
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        remaining -= read
                    }
                }
                pages += target
            }
            if (pages.isEmpty()) {
                throw ComicImportException("未能从 MOBI/AZW 文件中提取图片，可能只有纯文本内容")
            }
            return pages
        }
    }

    private fun readUInt16(file: RandomAccessFile, offset: Long): Int {
        file.seek(offset)
        return (file.readUnsignedByte() shl 8) or file.readUnsignedByte()
    }

    private fun readUInt32(file: RandomAccessFile, offset: Long): Long {
        file.seek(offset)
        var value = 0L
        repeat(4) { value = (value shl 8) or file.readUnsignedByte().toLong() }
        return value
    }
}
