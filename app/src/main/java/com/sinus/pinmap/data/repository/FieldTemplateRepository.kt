package com.sinus.pinmap.data.repository

import com.sinus.pinmap.data.store.FieldTemplateStore
import com.sinus.pinmap.data.entity.FieldTemplate
import kotlinx.coroutines.flow.Flow
/**
 * 字段模板数据仓库
 */
class FieldTemplateRepository(private val fieldTemplateStore: FieldTemplateStore) {
    fun getAllFieldTemplates(): Flow<List<FieldTemplate>> = fieldTemplateStore.getAllFieldTemplates()

    suspend fun getFieldTemplateById(id: Long): FieldTemplate? = fieldTemplateStore.getFieldTemplateById(id)

    fun getFieldTemplatesByCategory(categoryId: Long): Flow<List<FieldTemplate>> =
        fieldTemplateStore.getFieldTemplatesByCategory(categoryId)

    suspend fun insertFieldTemplate(fieldTemplate: FieldTemplate): Long =
        fieldTemplateStore.insertFieldTemplate(fieldTemplate)

    suspend fun updateFieldTemplate(fieldTemplate: FieldTemplate) =
        fieldTemplateStore.updateFieldTemplate(fieldTemplate)

    suspend fun deleteFieldTemplate(fieldTemplate: FieldTemplate) =
        fieldTemplateStore.deleteFieldTemplate(fieldTemplate)

    suspend fun deleteFieldTemplateById(id: Long) =
        fieldTemplateStore.deleteFieldTemplateById(id)

    suspend fun deleteFieldTemplatesByCategory(categoryId: Long) =
        fieldTemplateStore.deleteFieldTemplatesByCategory(categoryId)
}