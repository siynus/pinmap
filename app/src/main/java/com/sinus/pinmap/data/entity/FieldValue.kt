package com.sinus.pinmap.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 字段值实体类
 * fieldTemplateId != null → 模板字段
 * fieldTemplateId == null → 独立字段（fieldName/fieldType 有值）
 */
@Entity(tableName = "field_values")
data class FieldValue(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pinId: Long,
    val fieldTemplateId: Long? = null,
    val fieldName: String? = null,
    val fieldType: FieldType? = null,
    val value: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)