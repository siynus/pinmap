package com.sinus.pinmap.ui.model

import com.sinus.pinmap.data.entity.FieldType

/**
 * 字段数据类
 * 用于在 UI 层传递字段信息
 */
data class FieldData(
    val id: Long,
    val name: String,
    val type: FieldType,
    val value: String,
    val isTemplate: Boolean = false
)