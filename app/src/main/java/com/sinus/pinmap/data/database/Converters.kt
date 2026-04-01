package com.sinus.pinmap.data.database

import androidx.room.TypeConverter
import com.sinus.pinmap.data.entity.AttachmentType
import com.sinus.pinmap.data.entity.FieldType

/**
 * Room 类型转换器
 */
class Converters {
    @TypeConverter
    fun fromFieldType(value: FieldType): String {
        return value.name
    }

    @TypeConverter
    fun toFieldType(value: String): FieldType {
        return FieldType.valueOf(value)
    }

    @TypeConverter
    fun fromAttachmentType(value: AttachmentType): String {
        return value.name
    }

    @TypeConverter
    fun toAttachmentType(value: String): AttachmentType {
        return AttachmentType.valueOf(value)
    }
}