package com.sinus.pinmap.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 分类通用字段模板
 */
@Entity(tableName = "field_templates")
data class FieldTemplate(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long, // 关联到分类
    val fieldName: String,
    val fieldType: FieldType,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)