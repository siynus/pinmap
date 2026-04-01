package com.sinus.pinmap.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 附件实体类
 */
@Entity(tableName = "attachments")
data class Attachment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pinId: Long,
    val type: AttachmentType,
    val filePath: String,
    val fileName: String,
    val fileSize: Long,
    val duration: Long? = null, // 音频/视频时长（毫秒）
    val transcription: String? = null, // 语音转文字结果
    val createdAt: Long = System.currentTimeMillis()
)