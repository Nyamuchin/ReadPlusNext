package com.readplusnext.data.source.extract

import java.io.BufferedOutputStream
import java.io.File
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Shared helper that turns extracted page files back into a normalised ZIP. */
internal object ArchiveExtractionUtils {

    fun writeZip(files: List<File>, target: File) {
        target.parentFile?.mkdirs()
        ZipOutputStream(BufferedOutputStream(target.outputStream())).use { zip ->
            files.forEachIndexed { index, file ->
                val extension = file.name.substringAfterLast('.', "jpg")
                zip.putNextEntry(ZipEntry("page_%05d.%s".format(Locale.US, index, extension)))
                file.inputStream().use { input -> input.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }
}
