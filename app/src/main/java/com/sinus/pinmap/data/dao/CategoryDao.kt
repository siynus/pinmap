package com.sinus.pinmap.data.dao

import androidx.room.*
import com.sinus.pinmap.data.entity.Category
import kotlinx.coroutines.flow.Flow

/**
 * 分类数据访问对象
 */
@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY updatedAt DESC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :categoryId")
    suspend fun getCategoryById(categoryId: Long): Category?

    @Insert
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteCategoryById(categoryId: Long)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int
}