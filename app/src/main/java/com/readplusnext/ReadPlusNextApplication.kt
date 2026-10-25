package com.readplusnext

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.readplusnext.data.image.ComicPageFetcherFactory
import com.readplusnext.data.image.ComicPageKeyer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import java.io.File

@HiltAndroidApp
class ReadPlusNextApplication : Application(), ImageLoaderFactory {

    /**
     * Coil asks for the loader lazily, so the Hilt graph is already available when this runs.
     * Custom page fetchers are registered here; the disk cache only holds decoded sources.
     */
    override fun newImageLoader(): ImageLoader {
        val entryPoint = EntryPointAccessors.fromApplication(this, ImageLoaderEntryPoint::class.java)
        return ImageLoader.Builder(this)
            .components {
                add(ComicPageKeyer())
                add(entryPoint.comicPageFetcherFactory())
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(cacheDir, "image_cache"))
                    .maxSizeBytes(256L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ImageLoaderEntryPoint {
    fun comicPageFetcherFactory(): ComicPageFetcherFactory
}
