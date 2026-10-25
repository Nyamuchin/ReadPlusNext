package com.readplusnext.data.local

import com.readplusnext.domain.model.Category
import com.readplusnext.domain.model.CategoryType
import com.readplusnext.domain.model.Comic
import com.readplusnext.domain.model.ImportSource
import com.readplusnext.domain.model.Video
import com.readplusnext.data.local.entity.CategoryEntity
import com.readplusnext.data.local.entity.ComicEntity
import com.readplusnext.data.local.entity.VideoEntity

fun ComicEntity.toDomain(): Comic = Comic(
    id = id,
    title = title,
    originalUri = originalUri,
    zipPath = zipPath,
    pageCount = pageCount,
    coverPath = coverPath,
    lastReadPage = lastReadPage,
    lastReadAt = lastReadAt,
    createdAt = createdAt,
)

fun VideoEntity.toDomain(): Video = Video(
    id = id,
    title = title,
    uri = uri,
    duration = duration,
    size = size,
    coverPath = coverPath,
    folderUri = folderUri,
    importSource = if (importSource == ImportSource.FOLDER.name) ImportSource.FOLDER else ImportSource.MANUAL,
    createdAt = createdAt,
)

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    type = if (type == CategoryType.VIDEO.name) CategoryType.VIDEO else CategoryType.COMIC,
    sortOrder = sortOrder,
    createdAt = createdAt,
)
