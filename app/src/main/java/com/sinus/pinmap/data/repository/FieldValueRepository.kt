package com.sinus.pinmap.data.repository

import com.sinus.pinmap.data.dao.FieldValueDao
import com.sinus.pinmap.data.entity.FieldValue
import kotlinx.coroutines.flow.Flow

/**
 * 字段值数据仓库
 */
class FieldValueRepository(private val fieldValueDao: FieldValueDao) {
    fun getAllFieldValues(): Flow<List<FieldValue>> = fieldValueDao.getAllFieldValues()

    suspend fun getFieldValueById(id: Long): FieldValue? = fieldValueDao.getFieldValueById(id)

    fun getFieldValuesByPin(pinId: Long): Flow<List<FieldValue>> =
        fieldValueDao.getFieldValuesByPin(pinId)

    suspend fun getFieldValue(pinId: Long, fieldTemplateId: Long): FieldValue? =
        fieldValueDao.getFieldValue(pinId, fieldTemplateId)

    fun getFieldValuesByTemplate(fieldTemplateId: Long): Flow<List<FieldValue>> =
        fieldValueDao.getFieldValuesByTemplate(fieldTemplateId)

    suspend fun insertFieldValue(fieldValue: FieldValue): Long =
        fieldValueDao.insertFieldValue(fieldValue)

    suspend fun updateFieldValue(fieldValue: FieldValue) =
        fieldValueDao.updateFieldValue(fieldValue)

    suspend fun deleteFieldValue(fieldValue: FieldValue) =
        fieldValueDao.deleteFieldValue(fieldValue)

    suspend fun deleteFieldValueById(id: Long) =
        fieldValueDao.deleteFieldValueById(id)

    suspend fun deleteFieldValueByPinAndTemplate(pinId: Long, fieldTemplateId: Long) =
        fieldValueDao.deleteFieldValueByPinAndTemplate(pinId, fieldTemplateId)

    suspend fun deleteFieldValuesByPin(pinId: Long) =
        fieldValueDao.deleteFieldValuesByPin(pinId)

    suspend fun deleteFieldValuesByTemplate(fieldTemplateId: Long) =
        fieldValueDao.deleteFieldValuesByTemplate(fieldTemplateId)
}