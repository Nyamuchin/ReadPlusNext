package com.readplusnext.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.readplusnext.data.local.entity.VideoCategoryCrossRef
import com.readplusnext.data.local.entity.VideoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos ORDER BY createdAt DESC, id DESC")
    fun observeAll(): Flow<List<VideoEntity>>

    @Query(
        """
        SELECT v.* FROM videos v
        INNER JOIN video_category_cross_ref r ON v.id = r.videoId
        WHERE r.categoryId = :categoryId
        ORDER BY v.createdAt DESC, v.id DESC
        """
    )
    fun observeByCategory(categoryId: Long): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE id = :id")
    fun observeById(id: Long): Flow<VideoEntity?>

    @Query("SELECT * FROM videos WHERE id = :id")
    suspend fun getById(id: Long): VideoEntity?

    @Query("SELECT * FROM videos ORDER BY id ASC")
    suspend fun getAll(): List<VideoEntity>

    @Insert
    suspend fun insert(entity: VideoEntity): Long

    @Insert
    suspend fun insertAll(entities: List<VideoEntity>): List<Long>

    @Update
    suspend fun update(entity: VideoEntity)

    @Query("DELETE FROM videos WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM videos")
    suspend fun deleteAll()

    @Query("UPDATE videos SET title = :title WHERE id = :id")
    suspend fun updateTitle(id: Long, title: String)

    @Query("UPDATE videos SET coverPath = :coverPath WHERE id = :id")
    suspend fun updateCoverPath(id: Long, coverPath: String?)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRef(ref: VideoCategoryCrossRef)

    @Query("DELETE FROM video_category_cross_ref WHERE videoId = :videoId AND categoryId = :categoryId")
    suspend fun deleteCrossRef(videoId: Long, categoryId: Long)

    @Query("SELECT categoryId FROM video_category_cross_ref WHERE videoId = :videoId")
    suspend fun categoryIds(videoId: Long): List<Long>

    @Query("SELECT * FROM video_category_cross_ref")
    suspend fun getAllCrossRefs(): List<VideoCategoryCrossRef>
}
