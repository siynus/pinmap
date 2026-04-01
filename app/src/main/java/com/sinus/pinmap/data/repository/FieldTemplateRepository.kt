package com.sinus.pinmap.data.repository

import com.sinus.pinmap.data.dao.FieldTemplateDao
import com.sinus.pinmap.data.entity.FieldTemplate
import kotlinx.coroutines.flow.Flow

/**
 * 字段模板数据仓库
 */
class FieldTemplateRepository(private val fieldTemplateDao: FieldTemplateDao) {
    fun getAllFieldTemplates(): Flow<List<FieldTemplate>> = fieldTemplateDao.getAllFieldTemplates()

    suspend fun getFieldTemplateById(id: Long): FieldTemplate? = fieldTemplateDao.getFieldTemplateById(id)

    fun getFieldTemplatesByCategory(categoryId: Long): Flow<List<FieldTemplate>> =
        fieldTemplateDao.getFieldTemplatesByCategory(categoryId)

    fun getTemplateFieldsByCategory(categoryId: Long): Flow<List<FieldTemplate>> =
        fieldTemplateDao.getTemplateFieldsByCategory(categoryId)

    fun getCustomFieldTemplates(): Flow<List<FieldTemplate>> =
        fieldTemplateDao.getCustomFieldTemplates()

    suspend fun insertFieldTemplate(fieldTemplate: FieldTemplate): Long =
        fieldTemplateDao.insertFieldTemplate(fieldTemplate)

    suspend fun updateFieldTemplate(fieldTemplate: FieldTemplate) =
        fieldTemplateDao.updateFieldTemplate(fieldTemplate)

    suspend fun deleteFieldTemplate(fieldTemplate: FieldTemplate) =
        fieldTemplateDao.deleteFieldTemplate(fieldTemplate)

    suspend fun deleteFieldTemplateById(id: Long) =
        fieldTemplateDao.deleteFieldTemplateById(id)

    suspend fun deleteFieldTemplatesByCategory(categoryId: Long) =
        fieldTemplateDao.deleteFieldTemplatesByCategory(categoryId)
}