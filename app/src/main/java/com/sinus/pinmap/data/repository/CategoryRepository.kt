package com.sinus.pinmap.data.repository

import com.sinus.pinmap.data.store.CategoryStore
import com.sinus.pinmap.data.entity.Category
import kotlinx.coroutines.flow.Flow
/**
 * 分类数据仓库
 */
class CategoryRepository(private val categoryStore: CategoryStore) {
    fun getAllCategories(): Flow<List<Category>> = categoryStore.getAllCategories()

    suspend fun getCategoryById(categoryId: Long): Category? = categoryStore.getCategoryById(categoryId)

    suspend fun insertCategory(category: Category): Long = categoryStore.insertCategory(category)

    suspend fun updateCategory(category: Category) = categoryStore.updateCategory(category)

    suspend fun deleteCategory(category: Category) = categoryStore.deleteCategory(category)

    suspend fun deleteCategoryById(categoryId: Long) = categoryStore.deleteCategoryById(categoryId)

    suspend fun getCategoryCount(): Int = categoryStore.getCategoryCount()
}