package com.readplusnext.data.source

import com.readplusnext.core.util.extensionOf

/** Image names and magic numbers understood by the page pipeline. */
object ImageFormats {

    private val extensionToMime = mapOf(
        "jpg" to "image/jpeg",
        "jpeg" to "image/jpeg",
        "png" to "image/png",
        "webp" to "image/webp",
        "gif" to "image/gif",
        "bmp" to "image/bmp",
        "heic" to "image/heic",
        "heif" to "image/heif",
        "avif" to "image/avif",
    )

    fun isImageName(name: String): Boolean = name.extensionOf() in extensionToMime

    fun mimeTypeFor(name: String): String = extensionToMime[name.extensionOf()] ?: "image/jpeg"

    fun extensionFor(name: String): String = name.extensionOf().ifEmpty { "jpg" }

    /** Detects an image by its leading bytes; used for MOBI record scanning. */
    fun extensionForMagic(header: ByteArray): String? {
        if (header.size >= 3 && header[0] == 0xFF.toByte() && header[1] == 0xD8.toByte() && header[2] == 0xFF.toByte()) {
            return "jpg"
        }
        if (header.size >= 8 && header[0] == 0x89.toByte() && header[1] == 0x50.toByte() &&
            header[2] == 0x4E.toByte() && header[3] == 0x47.toByte()
        ) {
            return "png"
        }
        if (header.size >= 4 && header[0] == 'G'.code.toByte() && header[1] == 'I'.code.toByte() &&
            header[2] == 'F'.code.toByte() && header[3] == '8'.code.toByte()
        ) {
            return "gif"
        }
        if (header.size >= 2 && header[0] == 'B'.code.toByte() && header[1] == 'M'.code.toByte()) {
            return "bmp"
        }
        if (header.size >= 12 && header[0] == 'R'.code.toByte() && header[1] == 'I'.code.toByte() &&
            header[2] == 'F'.code.toByte() && header[3] == 'F'.code.toByte() &&
            header[8] == 'W'.code.toByte() && header[9] == 'E'.code.toByte() &&
            header[10] == 'B'.code.toByte() && header[11] == 'P'.code.toByte()
        ) {
            return "webp"
        }
        if (header.size >= 12 && header[4] == 'f'.code.toByte() && header[5] == 't'.code.toByte() &&
            header[6] == 'y'.code.toByte() && header[7] == 'p'.code.toByte()
        ) {
            val brand = String(header, 8, 4, Charsets.US_ASCII)
            return when (brand) {
                "avif" -> "avif"
                "heic", "heix", "hevc", "mif1", "msf1" -> "heic"
                else -> null
            }
        }
        return null
    }
}
