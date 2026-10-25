package com.readplusnext.data.source

import android.content.Context
import android.graphics.Bitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores generated comic covers. File names carry a timestamp so replacing a cover never
 * serves a stale cached bitmap.
 */
@Singleton
class CoverStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val coversDir: File
        get() = File(context.filesDir, COVERS_DIR).apply { mkdirs() }

    fun saveBytes(bytes: ByteArray, extension: String): String? = runCatching {
        val target = newCoverFile(extension)
        target.outputStream().use { it.write(bytes) }
        target.absolutePath
    }.getOrNull()

    fun saveBitmap(bitmap: Bitmap, extension: String = "jpg", quality: Int = 88): String? = runCatching {
        val target = newCoverFile(extension)
        val format = if (extension.equals("png", ignoreCase = true)) {
            Bitmap.CompressFormat.PNG
        } else {
            Bitmap.CompressFormat.JPEG
        }
        target.outputStream().use { output -> bitmap.compress(format, quality, output) }
        target.absolutePath
    }.getOrNull()

    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
    }

    private fun newCoverFile(extension: String): File {
        val safeExtension = extension.lowercase().ifBlank { "jpg" }
        return File(coversDir, "${UUID.randomUUID()}-${System.currentTimeMillis()}.$safeExtension")
    }

    private companion object {
        const val COVERS_DIR = "covers"
    }
}
