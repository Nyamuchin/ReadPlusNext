package com.readplusnext.data.source

import com.readplusnext.core.util.extensionOf
import java.io.File

/** Container formats the importer can turn into a paginated comic. */
enum class ArchiveFormat {
    ZIP,
    RAR,
    SEVEN_Z,
    PDF,
    EPUB,
    MOBI,
    UNKNOWN,
}

/**
 * Chooses a format from the file name first and falls back to magic bytes, so files with
 * a wrong or missing extension (common for `.cbz` copies) still import.
 */
object ArchiveFormatDetector {

    private val extensionToFormat = mapOf(
        "zip" to ArchiveFormat.ZIP,
        "cbz" to ArchiveFormat.ZIP,
        "rar" to ArchiveFormat.RAR,
        "cbr" to ArchiveFormat.RAR,
        "7z" to ArchiveFormat.SEVEN_Z,
        "cb7" to ArchiveFormat.SEVEN_Z,
        "pdf" to ArchiveFormat.PDF,
        "epub" to ArchiveFormat.EPUB,
        "mobi" to ArchiveFormat.MOBI,
        "azw" to ArchiveFormat.MOBI,
        "azw3" to ArchiveFormat.MOBI,
    )

    fun detect(file: File, fileName: String): ArchiveFormat {
        extensionToFormat[fileName.extensionOf()]?.let { return it }
        return sniff(file)
    }

    fun isSupportedName(fileName: String): Boolean = extensionToFormat.containsKey(fileName.extensionOf())

    private fun sniff(file: File): ArchiveFormat {
        val header = ByteArray(72)
        val read = file.inputStream().use { it.read(header) }
        if (read <= 0) return ArchiveFormat.UNKNOWN
        return when {
            startsWithAscii(header, read, "%PDF") -> ArchiveFormat.PDF
            read >= 4 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() &&
                (header[2] == 0x03.toByte() || header[2] == 0x05.toByte() || header[2] == 0x07.toByte()) ->
                ArchiveFormat.ZIP

            read >= 7 && header[0] == 'R'.code.toByte() && header[1] == 'a'.code.toByte() &&
                header[2] == 'r'.code.toByte() && header[3] == '!'.code.toByte() &&
                header[4] == 0x1A.toByte() && header[5] == 0x07.toByte() -> ArchiveFormat.RAR

            read >= 6 && header[0] == 0x37.toByte() && header[1] == 0x7A.toByte() &&
                header[2] == 0xBC.toByte() && header[3] == 0xAF.toByte() &&
                header[4] == 0x27.toByte() && header[5] == 0x1C.toByte() -> ArchiveFormat.SEVEN_Z

            read >= 68 && String(header, 60, 8, Charsets.US_ASCII) == "BOOKMOBI" -> ArchiveFormat.MOBI

            else -> ArchiveFormat.UNKNOWN
        }
    }

    private fun startsWithAscii(header: ByteArray, read: Int, prefix: String): Boolean {
        if (read < prefix.length) return false
        for (index in prefix.indices) {
            if (header[index] != prefix[index].code.toByte()) return false
        }
        return true
    }
}
