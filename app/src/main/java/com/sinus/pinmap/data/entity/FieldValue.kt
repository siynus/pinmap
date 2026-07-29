package com.sinus.pinmap.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "field_values")
data class FieldValue(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pinId: Long,
    val fieldTemplateId: Long,
    val value: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
