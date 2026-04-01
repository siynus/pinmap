package com.sinus.pinmap.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 字段值实体类
 * 用于存储标记的具体字段值
 */
@Entity(tableName = "field_values")
data class FieldValue(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pinId: Long, // 关联到标记
    val fieldTemplateId: Long, // 关联到字段模板
    val value: String?, // 字段值（对于图片类型，存储附件ID）
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)