package com.sinus.pinmap.data.store

import androidx.room.*
import com.sinus.pinmap.data.entity.FieldTemplate
import kotlinx.coroutines.flow.Flow

@Dao
interface FieldTemplateStore {
    @Query("SELECT * FROM field_templates ORDER BY sortOrder ASC, id ASC")
    fun getAllFieldTemplates(): Flow<List<FieldTemplate>>

    @Query("SELECT * FROM field_templates WHERE id = :id")
    suspend fun getFieldTemplateById(id: Long): FieldTemplate?

    @Query("SELECT * FROM field_templates WHERE categoryId = :categoryId ORDER BY sortOrder ASC, id ASC")
    fun getFieldTemplatesByCategory(categoryId: Long): Flow<List<FieldTemplate>>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM field_templates WHERE categoryId = :categoryId")
    suspend fun nextSortOrder(categoryId: Long): Int

    @Insert
    suspend fun insertFieldTemplate(fieldTemplate: FieldTemplate): Long

    @Update
    suspend fun updateFieldTemplate(fieldTemplate: FieldTemplate)

    @Delete
    suspend fun deleteFieldTemplate(fieldTemplate: FieldTemplate)

    @Query("DELETE FROM field_templates WHERE id = :id")
    suspend fun deleteFieldTemplateById(id: Long)

    @Query("DELETE FROM field_templates WHERE categoryId = :categoryId")
    suspend fun deleteFieldTemplatesByCategory(categoryId: Long)
}
