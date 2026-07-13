package com.sinus.pinmap.ui.model

import com.sinus.pinmap.data.entity.FieldType

data class FieldData(
    val id: Long,
    val name: String,
    val type: FieldType,
    val value: String
)