package com.readplusnext.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "comics")
data class ComicEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val originalUri: String,
    val zipPath: String,
    val pageCount: Int,
    val coverPath: String? = null,
    val lastReadPage: Int = 0,
    val lastReadAt: Long = 0,
    val createdAt: Long,
)
