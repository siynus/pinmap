package com.sinus.pinmap.data.repository

import com.sinus.pinmap.data.dao.CustomFieldDao
import com.sinus.pinmap.data.entity.CustomField
import kotlinx.coroutines.flow.Flow

/**
 * 自定义字段数据仓库
 */
class CustomFieldRepository(private val customFieldDao: CustomFieldDao) {
    fun getFieldsByPinId(pinId: Long): Flow<List<CustomField>> = customFieldDao.getFieldsByPinId(pinId)

    suspend fun getFieldById(fieldId: Long): CustomField? = customFieldDao.getFieldById(fieldId)

    suspend fun insertField(field: CustomField): Long = customFieldDao.insertField(field)

    suspend fun updateField(field: CustomField) = customFieldDao.updateField(field)

    suspend fun deleteField(field: CustomField) = customFieldDao.deleteField(field)

    suspend fun deleteFieldsByPinId(pinId: Long) = customFieldDao.deleteFieldsByPinId(pinId)

    suspend fun deleteFieldById(fieldId: Long) = customFieldDao.deleteFieldById(fieldId)
}