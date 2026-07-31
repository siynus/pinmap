package com.sinus.pinmap.data.entity

/**
 * 附件类型枚举（已弃用：媒体改用 IMAGE/VIDEO 字段）
 */
@Deprecated("附件类型已弃用，媒体改用 FieldType.IMAGE / FieldType.VIDEO")
enum class AttachmentType {
    IMAGE,
    AUDIO,
    VIDEO
}