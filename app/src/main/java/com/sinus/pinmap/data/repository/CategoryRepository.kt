package com.sinus.pinmap.data.repository

import com.sinus.pinmap.data.store.CategoryStore
import com.sinus.pinmap.data.entity.Category
import kotlinx.coroutines.flow.Flow
/**
 * 分类数据仓库
 */
class CategoryRepository(private val mCategoryStore: CategoryStore) {
    fun getAllCategories(): Flow<List<Category>> = mCategoryStore.getAllCategories()

    suspend fun getCategoryById(categoryId: Long): Category? = mCategoryStore.getCategoryById(categoryId)

    suspend fun insertCategory(category: Category): Long = mCategoryStore.insertCategory(category)

    suspend fun updateCategory(category: Category) = mCategoryStore.updateCategory(category)

    suspend fun deleteCategory(category: Category) = mCategoryStore.deleteCategory(category)

    suspend fun deleteCategoryById(categoryId: Long) = mCategoryStore.deleteCategoryById(categoryId)

    suspend fun getCategoryCount(): Int = mCategoryStore.getCategoryCount()
}