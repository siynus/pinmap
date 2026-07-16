package com.sinus.pinmap.data.repository

import com.sinus.pinmap.data.store.FieldTemplateStore
import com.sinus.pinmap.data.entity.FieldTemplate
import kotlinx.coroutines.flow.Flow
/**
 * 字段模板数据仓库
 */
class FieldTemplateRepository(private val mFieldTemplateStore: FieldTemplateStore) {
    fun getAllFieldTemplates(): Flow<List<FieldTemplate>> = mFieldTemplateStore.getAllFieldTemplates()

    suspend fun getFieldTemplateById(id: Long): FieldTemplate? = mFieldTemplateStore.getFieldTemplateById(id)

    fun getFieldTemplatesByCategory(categoryId: Long): Flow<List<FieldTemplate>> =
        mFieldTemplateStore.getFieldTemplatesByCategory(categoryId)

    suspend fun nextSortOrder(categoryId: Long): Int = mFieldTemplateStore.nextSortOrder(categoryId)

    suspend fun insertFieldTemplate(fieldTemplate: FieldTemplate): Long =
        mFieldTemplateStore.insertFieldTemplate(fieldTemplate)

    suspend fun updateFieldTemplate(fieldTemplate: FieldTemplate) =
        mFieldTemplateStore.updateFieldTemplate(fieldTemplate)

    suspend fun deleteFieldTemplate(fieldTemplate: FieldTemplate) =
        mFieldTemplateStore.deleteFieldTemplate(fieldTemplate)

    suspend fun deleteFieldTemplateById(id: Long) =
        mFieldTemplateStore.deleteFieldTemplateById(id)

    suspend fun deleteFieldTemplatesByCategory(categoryId: Long) =
        mFieldTemplateStore.deleteFieldTemplatesByCategory(categoryId)
}