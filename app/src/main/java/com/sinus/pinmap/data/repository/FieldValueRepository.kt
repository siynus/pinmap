package com.sinus.pinmap.data.repository

import com.sinus.pinmap.data.store.FieldValueStore
import com.sinus.pinmap.data.entity.FieldValue
import kotlinx.coroutines.flow.Flow
/**
 * 字段值数据仓库
 */
class FieldValueRepository(private val mFieldValueStore: FieldValueStore) {
    fun getAllFieldValues(): Flow<List<FieldValue>> = mFieldValueStore.getAllFieldValues()

    suspend fun getFieldValueById(id: Long): FieldValue? = mFieldValueStore.getFieldValueById(id)

    fun getFieldValuesByPin(pinId: Long): Flow<List<FieldValue>> =
        mFieldValueStore.getFieldValuesByPin(pinId)

    suspend fun getFieldValue(pinId: Long, fieldTemplateId: Long): FieldValue? =
        mFieldValueStore.getFieldValue(pinId, fieldTemplateId)

    fun getFieldValuesByTemplate(fieldTemplateId: Long): Flow<List<FieldValue>> =
        mFieldValueStore.getFieldValuesByTemplate(fieldTemplateId)

    suspend fun insertFieldValue(fieldValue: FieldValue): Long =
        mFieldValueStore.insertFieldValue(fieldValue)

    suspend fun updateFieldValue(fieldValue: FieldValue) =
        mFieldValueStore.updateFieldValue(fieldValue)

    suspend fun deleteFieldValue(fieldValue: FieldValue) =
        mFieldValueStore.deleteFieldValue(fieldValue)

    suspend fun deleteFieldValueById(id: Long) =
        mFieldValueStore.deleteFieldValueById(id)

    suspend fun deleteFieldValueByPinAndTemplate(pinId: Long, fieldTemplateId: Long) =
        mFieldValueStore.deleteFieldValueByPinAndTemplate(pinId, fieldTemplateId)

    suspend fun deleteFieldValuesByPin(pinId: Long) =
        mFieldValueStore.deleteFieldValuesByPin(pinId)

    suspend fun deleteFieldValuesByTemplate(fieldTemplateId: Long) =
        mFieldValueStore.deleteFieldValuesByTemplate(fieldTemplateId)
}