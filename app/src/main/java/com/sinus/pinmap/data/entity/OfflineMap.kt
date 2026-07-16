package com.sinus.pinmap.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 离线地图实体类
 */
@Entity(tableName = "offline_maps")
data class OfflineMap(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cityName: String,
    val cityCode: String,
    val filePath: String,
    val fileSize: Long,
    val downloadDate: Long = System.currentTimeMillis(),
    val version: String,
    val isDownloaded: Boolean = false,
    val downloadProgress: Int = 0 // 0-100
)