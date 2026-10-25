package com.readplusnext.domain.model

import com.readplusnext.domain.model.CategoryType

data class Comic(
    val id: Long,
    val title: String,
    val originalUri: String,
    val zipPath: String,
    val pageCount: Int,
    val coverPath: String?,
    val lastReadPage: Int,
    val lastReadAt: Long,
    val createdAt: Long,
) {
    /** PDF comics keep the original file in [zipPath] and are rendered with PdfRenderer. */
    val isPdf: Boolean
        get() = zipPath.endsWith(".pdf", ignoreCase = true)

    val progressFraction: Float
        get() = if (pageCount <= 0) 0f else (lastReadPage.toFloat() / pageCount).coerceIn(0f, 1f)
}

data class Video(
    val id: Long,
    val title: String,
    val uri: String,
    val duration: Long,
    val size: Long,
    val coverPath: String?,
    val folderUri: String?,
    val importSource: ImportSource,
    val createdAt: Long,
)

enum class ImportSource { MANUAL, FOLDER }

data class Category(
    val id: Long,
    val name: String,
    val type: CategoryType,
    val sortOrder: Int,
    val createdAt: Long,
)

enum class CategoryType { COMIC, VIDEO }

/** A page inside a comic, addressed by a stable key handled by the image loader. */
data class ComicPage(
    val index: Int,
    val key: String,
)

data class ImportSummary(
    val imported: Int,
    val failed: Int,
) {
    val total: Int get() = imported + failed
}
