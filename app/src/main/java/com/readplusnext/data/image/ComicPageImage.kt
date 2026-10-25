package com.readplusnext.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.key.Keyer
import coil.request.Options
import coil.size.Size
import coil.size.pxOrElse
import com.readplusnext.data.source.ComicPageSource
import java.nio.ByteBuffer
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Identifies one page of one comic for Coil. */
data class ComicPageImage(
    val sourcePath: String,
    val pageKey: String,
    val isPdf: Boolean,
)

class ComicPageKeyer : Keyer<ComicPageImage> {
    override fun key(data: ComicPageImage, options: Options): String {
        val width = options.size.width.pxOrElse { 0 }
        val height = options.size.height.pxOrElse { 0 }
        return "comic-page:${data.isPdf}:${data.sourcePath}:${data.pageKey}:$width:$height"
    }
}

/**
 * Loads a page straight from the normalised archive. ZIP pages are decoded from the entry bytes,
 * downsized to the requested size (animated GIF/WebP pages keep animating through [ImageDecoder]);
 * PDF pages render only the requested page at the requested width.
 */
class ComicPageFetcher(
    private val context: Context,
    private val pageSource: ComicPageSource,
    private val image: ComicPageImage,
    private val size: Size,
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        val file = java.io.File(image.sourcePath)
        if (!file.exists()) return@withContext null

        if (image.isPdf) {
            val targetWidth = size.width.pxOrElse { DEFAULT_PDF_WIDTH }.coerceIn(320, MAX_PDF_WIDTH)
            val bitmap = pageSource.renderPdfPage(file, image.pageKey, targetWidth)
                ?: return@withContext null
            DrawableResult(
                drawable = BitmapDrawable(context.resources, bitmap),
                isSampled = true,
                dataSource = DataSource.DISK,
            )
        } else {
            val bytes = pageSource.readZipPageBytes(file, image.pageKey) ?: return@withContext null
            decodePage(bytes)
        }
    }

    private fun decodePage(bytes: ByteArray): FetchResult? {
        val targetWidth = size.width.pxOrElse { 0 }
        val targetHeight = size.height.pxOrElse { 0 }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val decoded = runCatching {
                val source = ImageDecoder.createSource(ByteBuffer.wrap(bytes))
                ImageDecoder.decodeDrawable(source) { decoder, info, _ ->
                    val sourceWidth = info.size.width
                    val sourceHeight = info.size.height
                    if (sourceWidth <= 0 || sourceHeight <= 0) return@decodeDrawable
                    val scale = if (targetWidth > 0 && targetHeight > 0) {
                        minOf(
                            targetWidth.toFloat() / sourceWidth,
                            targetHeight.toFloat() / sourceHeight,
                            1f,
                        )
                    } else {
                        minOf(MAX_UNSPECIFIED_WIDTH.toFloat() / sourceWidth, 1f)
                    }
                    if (scale < 1f) {
                        decoder.setTargetSize(
                            (sourceWidth * scale).roundToInt().coerceAtLeast(1),
                            (sourceHeight * scale).roundToInt().coerceAtLeast(1),
                        )
                    }
                }
            }.getOrNull()
            if (decoded != null) {
                return DrawableResult(decoded, isSampled = true, dataSource = DataSource.DISK)
            }
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, targetWidth, targetHeight)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
        return DrawableResult(
            drawable = BitmapDrawable(context.resources, bitmap),
            isSampled = true,
            dataSource = DataSource.DISK,
        )
    }

    private fun sampleSize(width: Int, height: Int, targetWidth: Int, targetHeight: Int): Int {
        if (width <= 0 || height <= 0) return 1
        var sample = 1
        if (targetWidth <= 0 || targetHeight <= 0) {
            while (width / (sample * 2) >= MAX_UNSPECIFIED_WIDTH) sample *= 2
            return sample
        }
        while (width / (sample * 2) >= targetWidth && height / (sample * 2) >= targetHeight) {
            sample *= 2
        }
        return sample
    }

    private companion object {
        const val DEFAULT_PDF_WIDTH = 1080
        const val MAX_PDF_WIDTH = 2048
        const val MAX_UNSPECIFIED_WIDTH = 2048
    }
}

class ComicPageFetcherFactory @javax.inject.Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val pageSource: ComicPageSource,
) : Fetcher.Factory<ComicPageImage> {
    override fun create(data: ComicPageImage, options: Options, imageLoader: ImageLoader): Fetcher =
        ComicPageFetcher(context, pageSource, data, options.size)
}
