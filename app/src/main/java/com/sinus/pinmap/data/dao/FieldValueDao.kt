package com.sinus.pinmap.data.dao

import androidx.room.*
import com.sinus.pinmap.data.entity.FieldValue
import kotlinx.coroutines.flow.Flow

/**
 * 字段值数据访问对象
 */
@Dao
interface FieldValueDao {
    @Query("SELECT * FROM field_values ORDER BY createdAt DESC")
    fun getAllFieldValues(): Flow<List<FieldValue>>

    @Query("SELECT * FROM field_values WHERE id = :id")
    suspend fun getFieldValueById(id: Long): FieldValue?

    @Query("SELECT * FROM field_values WHERE pinId = :pinId ORDER BY createdAt DESC")
    fun getFieldValuesByPin(pinId: Long): Flow<List<FieldValue>>

    @Query("SELECT * FROM field_values WHERE pinId = :pinId AND fieldTemplateId = :fieldTemplateId")
    suspend fun getFieldValue(pinId: Long, fieldTemplateId: Long): FieldValue?

    @Query("SELECT * FROM field_values WHERE fieldTemplateId = :fieldTemplateId")
    fun getFieldValuesByTemplate(fieldTemplateId: Long): Flow<List<FieldValue>>

    @Insert
    suspend fun insertFieldValue(fieldValue: FieldValue): Long

    @Update
    suspend fun updateFieldValue(fieldValue: FieldValue)

    @Delete
    suspend fun deleteFieldValue(fieldValue: FieldValue)

    @Query("DELETE FROM field_values WHERE id = :id")
    suspend fun deleteFieldValueById(id: Long)

    @Query("DELETE FROM field_values WHERE pinId = :pinId AND fieldTemplateId = :fieldTemplateId")
    suspend fun deleteFieldValueByPinAndTemplate(pinId: Long, fieldTemplateId: Long)

    @Query("DELETE FROM field_values WHERE pinId = :pinId")
    suspend fun deleteFieldValuesByPin(pinId: Long)

    @Query("DELETE FROM field_values WHERE fieldTemplateId = :fieldTemplateId")
    suspend fun deleteFieldValuesByTemplate(fieldTemplateId: Long)
}