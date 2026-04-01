package com.sinus.pinmap.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 分类实体类
 */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val color: Int,
    val icon: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)