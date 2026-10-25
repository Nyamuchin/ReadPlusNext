package com.readplusnext.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.readplusnext.data.local.entity.ComicCategoryCrossRef
import com.readplusnext.data.local.entity.ComicEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ComicDao {
    @Query("SELECT * FROM comics ORDER BY createdAt DESC, id DESC")
    fun observeAll(): Flow<List<ComicEntity>>

    @Query(
        """
        SELECT c.* FROM comics c
        INNER JOIN comic_category_cross_ref r ON c.id = r.comicId
        WHERE r.categoryId = :categoryId
        ORDER BY c.createdAt DESC, c.id DESC
        """
    )
    fun observeByCategory(categoryId: Long): Flow<List<ComicEntity>>

    @Query("SELECT * FROM comics WHERE id = :id")
    fun observeById(id: Long): Flow<ComicEntity?>

    @Query("SELECT * FROM comics WHERE id = :id")
    suspend fun getById(id: Long): ComicEntity?

    @Query("SELECT * FROM comics ORDER BY id ASC")
    suspend fun getAll(): List<ComicEntity>

    @Insert
    suspend fun insert(entity: ComicEntity): Long

    @Insert
    suspend fun insertAll(entities: List<ComicEntity>): List<Long>

    @Update
    suspend fun update(entity: ComicEntity)

    @Query("DELETE FROM comics WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM comics")
    suspend fun deleteAll()

    @Query("UPDATE comics SET title = :title WHERE id = :id")
    suspend fun updateTitle(id: Long, title: String)

    @Query("UPDATE comics SET coverPath = :coverPath WHERE id = :id")
    suspend fun updateCoverPath(id: Long, coverPath: String?)

    @Query("UPDATE comics SET lastReadPage = :page, lastReadAt = :readAt WHERE id = :id")
    suspend fun updateProgress(id: Long, page: Int, readAt: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRef(ref: ComicCategoryCrossRef)

    @Query("DELETE FROM comic_category_cross_ref WHERE comicId = :comicId AND categoryId = :categoryId")
    suspend fun deleteCrossRef(comicId: Long, categoryId: Long)

    @Query("SELECT categoryId FROM comic_category_cross_ref WHERE comicId = :comicId")
    suspend fun categoryIds(comicId: Long): List<Long>

    @Query("SELECT * FROM comic_category_cross_ref")
    suspend fun getAllCrossRefs(): List<ComicCategoryCrossRef>
}
