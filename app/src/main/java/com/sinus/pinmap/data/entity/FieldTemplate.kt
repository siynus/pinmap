package com.sinus.pinmap.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 字段模板实体类
 * 用于定义可复用的字段模板，可以关联到类别作为通用模板
 */
@Entity(tableName = "field_templates")
data class FieldTemplate(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long?, // 关联到类别，null表示不关联类别（仅用于单个标记）
    val fieldName: String, // 字段名称
    val fieldType: FieldType, // 字段类型
    val isTemplate: Boolean = false, // 是否为通用模板
    val options: String? = null, // 用于单选/多选的选项，JSON 格式
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)