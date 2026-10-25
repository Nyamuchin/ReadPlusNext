package com.readplusnext.data.repository

import com.readplusnext.data.local.entity.CategoryEntity
import com.readplusnext.data.local.dao.CategoryDao
import com.readplusnext.data.local.toDomain
import com.readplusnext.domain.model.Category
import com.readplusnext.domain.model.CategoryType
import com.readplusnext.domain.repository.CategoryRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
) : CategoryRepository {

    override fun observeCategories(type: CategoryType): Flow<List<Category>> =
        categoryDao.observeByType(type.name).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getCategories(type: CategoryType): List<Category> =
        categoryDao.getByType(type.name).map { it.toDomain() }

    override suspend fun createCategory(name: String, type: CategoryType): Category {
        val trimmed = name.trim()
        val createdAt = System.currentTimeMillis()
        val sortOrder = categoryDao.countByType(type.name)
        val id = categoryDao.insert(
            CategoryEntity(
                name = trimmed,
                type = type.name,
                sortOrder = sortOrder,
                createdAt = createdAt,
            ),
        )
        return Category(
            id = id,
            name = trimmed,
            type = type,
            sortOrder = sortOrder,
            createdAt = createdAt,
        )
    }

    override suspend fun renameCategory(id: Long, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        categoryDao.rename(id, trimmed)
    }

    override suspend fun deleteCategory(id: Long) {
        categoryDao.deleteById(id)
    }
}
