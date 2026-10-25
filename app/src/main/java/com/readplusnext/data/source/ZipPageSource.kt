package com.readplusnext.data.source

import com.readplusnext.core.util.NaturalOrderComparator
import java.io.File
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton

/** Random access to the normalised page archive that every comic is stored as. */
@Singleton
class ZipPageSource @Inject constructor() {

    /** Image entry names in natural page order. */
    fun imageEntries(zipFile: File): List<String> {
        if (!zipFile.exists()) return emptyList()
        return ZipFile(zipFile).use { zip ->
            zip.entries()
                .asSequence()
                .filter { !it.isDirectory && ImageFormats.isImageName(it.name) }
                .map { it.name }
                .sortedWith(NaturalOrderComparator)
                .toList()
        }
    }

    fun countImages(zipFile: File): Int = imageEntries(zipFile).size

    fun firstImageEntry(zipFile: File): String? = imageEntries(zipFile).firstOrNull()

    fun readEntry(zipFile: File, entryName: String): ByteArray? {
        if (!zipFile.exists()) return null
        return ZipFile(zipFile).use { zip ->
            val entry = zip.getEntry(entryName) ?: return null
            zip.getInputStream(entry).use { it.readBytes() }
        }
    }
}
