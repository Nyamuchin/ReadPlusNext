package com.readplusnext.data.source

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.ParcelFileDescriptor
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Reads video metadata and renders the first frame as a cover. */
@Singleton
class VideoCoverStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    data class Metadata(
        val durationMs: Long,
        val coverPath: String?,
    )

    fun readMetadata(uri: Uri): Metadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val duration = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?: 0L
            val frame = runCatching {
                retriever.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            }.getOrNull()
            Metadata(durationMs = duration, coverPath = frame?.let { saveFrame(it) })
        } catch (error: Exception) {
            Metadata(durationMs = 0L, coverPath = null)
        } finally {
            runCatching { retriever.release() }
        }
    }

    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
    }

    private fun saveFrame(frame: Bitmap): String? = runCatching {
        val directory = File(context.filesDir, COVERS_DIR).apply { mkdirs() }
        val scaled = if (frame.width > MAX_FRAME_WIDTH) {
            val height = (frame.height.toFloat() / frame.width * MAX_FRAME_WIDTH).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(frame, MAX_FRAME_WIDTH, height, true)
        } else {
            frame
        }
        val target = File(directory, "${UUID.randomUUID()}-${System.currentTimeMillis()}.jpg")
        target.outputStream().use { output ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, output)
        }
        if (scaled !== frame) scaled.recycle()
        target.absolutePath
    }.getOrNull()

    private companion object {
        const val COVERS_DIR = "video_covers"
        const val MAX_FRAME_WIDTH = 960
    }
}

/** Shared PDF helpers (also used when the reader needs a rendered page bitmap). */
internal object PdfPageRenderer {

    fun pageCount(file: File): Int {
        if (!file.exists()) return 0
        return try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                val renderer = PdfRenderer(descriptor)
                try {
                    renderer.pageCount
                } finally {
                    runCatching { renderer.close() }
                }
            }
        } catch (error: Exception) {
            0
        }
    }

    fun render(file: File, pageIndex: Int, targetWidth: Int): Bitmap? {
        if (!file.exists()) return null
        return try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                val renderer = PdfRenderer(descriptor)
                try {
                    if (pageIndex !in 0 until renderer.pageCount) return null
                    val page = renderer.openPage(pageIndex)
                    try {
                        val width = targetWidth.coerceIn(240, 2400)
                        val height = (width.toFloat() / page.width * page.height)
                            .toInt()
                            .coerceAtLeast(1)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    } finally {
                        runCatching { page.close() }
                    }
                } finally {
                    runCatching { renderer.close() }
                }
            }
        } catch (error: Exception) {
            null
        }
    }
}
