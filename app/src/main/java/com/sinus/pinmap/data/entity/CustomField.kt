package com.sinus.pinmap.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 自定义字段实体类
 */
@Entity(tableName = "custom_fields")
data class CustomField(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pinId: Long,
    val fieldName: String,
    val fieldType: FieldType,
    val value: String? = null,
    val options: String? = null // 用于单选/多选的选项，JSON 格式
)