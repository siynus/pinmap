package com.sinus.pinmap.data.dao

import androidx.room.*
import com.sinus.pinmap.data.entity.CustomField
import kotlinx.coroutines.flow.Flow

/**
 * 自定义字段数据访问对象
 */
@Dao
interface CustomFieldDao {
    @Query("SELECT * FROM custom_fields WHERE pinId = :pinId")
    fun getFieldsByPinId(pinId: Long): Flow<List<CustomField>>

    @Query("SELECT * FROM custom_fields WHERE id = :fieldId")
    suspend fun getFieldById(fieldId: Long): CustomField?

    @Insert
    suspend fun insertField(field: CustomField): Long

    @Update
    suspend fun updateField(field: CustomField)

    @Delete
    suspend fun deleteField(field: CustomField)

    @Query("DELETE FROM custom_fields WHERE pinId = :pinId")
    suspend fun deleteFieldsByPinId(pinId: Long)

    @Query("DELETE FROM custom_fields WHERE id = :fieldId")
    suspend fun deleteFieldById(fieldId: Long)
}