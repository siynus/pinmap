package com.sinus.pinmap.data.repository

import com.sinus.pinmap.data.dao.CategoryDao
import com.sinus.pinmap.data.entity.Category
import kotlinx.coroutines.flow.Flow

/**
 * 分类数据仓库
 */
class CategoryRepository(private val categoryDao: CategoryDao) {
    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()

    suspend fun getCategoryById(categoryId: Long): Category? = categoryDao.getCategoryById(categoryId)

    suspend fun insertCategory(category: Category): Long = categoryDao.insertCategory(category)

    suspend fun updateCategory(category: Category) = categoryDao.updateCategory(category)

    suspend fun deleteCategory(category: Category) = categoryDao.deleteCategory(category)

    suspend fun deleteCategoryById(categoryId: Long) = categoryDao.deleteCategoryById(categoryId)

    suspend fun getCategoryCount(): Int = categoryDao.getCategoryCount()
}