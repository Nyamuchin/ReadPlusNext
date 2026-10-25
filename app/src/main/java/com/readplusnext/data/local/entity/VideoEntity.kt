package com.readplusnext.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val uri: String,
    val duration: Long,
    val size: Long,
    val coverPath: String? = null,
    val folderUri: String? = null,
    val importSource: String = "MANUAL",
    val createdAt: Long,
)
