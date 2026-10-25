package com.readplusnext.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.readplusnext.data.local.dao.CategoryDao
import com.readplusnext.data.local.dao.ComicDao
import com.readplusnext.data.local.dao.VideoDao
import com.readplusnext.data.local.entity.CategoryEntity
import com.readplusnext.data.local.entity.ComicCategoryCrossRef
import com.readplusnext.data.local.entity.ComicEntity
import com.readplusnext.data.local.entity.VideoCategoryCrossRef
import com.readplusnext.data.local.entity.VideoEntity

@Database(
    entities = [
        ComicEntity::class,
        VideoEntity::class,
        CategoryEntity::class,
        ComicCategoryCrossRef::class,
        VideoCategoryCrossRef::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class ReadPlusNextDatabase : RoomDatabase() {
    abstract fun comicDao(): ComicDao
    abstract fun videoDao(): VideoDao
    abstract fun categoryDao(): CategoryDao
}
