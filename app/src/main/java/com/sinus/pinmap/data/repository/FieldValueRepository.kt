package com.sinus.pinmap.data.repository

import com.sinus.pinmap.data.store.FieldValueStore
import com.sinus.pinmap.data.entity.FieldValue
import kotlinx.coroutines.flow.Flow
/**
 * 字段值数据仓库
 */
class FieldValueRepository(private val fieldValueStore: FieldValueStore) {
    fun getAllFieldValues(): Flow<List<FieldValue>> = fieldValueStore.getAllFieldValues()

    suspend fun getFieldValueById(id: Long): FieldValue? = fieldValueStore.getFieldValueById(id)

    fun getFieldValuesByPin(pinId: Long): Flow<List<FieldValue>> =
        fieldValueStore.getFieldValuesByPin(pinId)

    suspend fun getFieldValue(pinId: Long, fieldTemplateId: Long): FieldValue? =
        fieldValueStore.getFieldValue(pinId, fieldTemplateId)

    fun getFieldValuesByTemplate(fieldTemplateId: Long): Flow<List<FieldValue>> =
        fieldValueStore.getFieldValuesByTemplate(fieldTemplateId)

    suspend fun insertFieldValue(fieldValue: FieldValue): Long =
        fieldValueStore.insertFieldValue(fieldValue)

    suspend fun updateFieldValue(fieldValue: FieldValue) =
        fieldValueStore.updateFieldValue(fieldValue)

    suspend fun deleteFieldValue(fieldValue: FieldValue) =
        fieldValueStore.deleteFieldValue(fieldValue)

    suspend fun deleteFieldValueById(id: Long) =
        fieldValueStore.deleteFieldValueById(id)

    suspend fun deleteFieldValueByPinAndTemplate(pinId: Long, fieldTemplateId: Long) =
        fieldValueStore.deleteFieldValueByPinAndTemplate(pinId, fieldTemplateId)

    suspend fun deleteFieldValuesByPin(pinId: Long) =
        fieldValueStore.deleteFieldValuesByPin(pinId)

    suspend fun deleteFieldValuesByTemplate(fieldTemplateId: Long) =
        fieldValueStore.deleteFieldValuesByTemplate(fieldTemplateId)
}