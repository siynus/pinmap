package com.sinus.pinmap.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sinus.pinmap.data.dao.AttachmentDao
import com.sinus.pinmap.data.dao.CategoryDao
import com.sinus.pinmap.data.dao.CustomFieldDao
import com.sinus.pinmap.data.dao.FieldTemplateDao
import com.sinus.pinmap.data.dao.FieldValueDao
import com.sinus.pinmap.data.dao.OfflineMapDao
import com.sinus.pinmap.data.dao.PinDao
import com.sinus.pinmap.data.entity.Attachment
import com.sinus.pinmap.data.entity.Category
import com.sinus.pinmap.data.entity.CustomField
import com.sinus.pinmap.data.entity.FieldTemplate
import com.sinus.pinmap.data.entity.FieldValue
import com.sinus.pinmap.data.entity.OfflineMap
import com.sinus.pinmap.data.entity.Pin

/**
 * Pinmap 应用数据库
 */
@Database(
    entities = [
        Pin::class,
        Category::class,
        CustomField::class,
        FieldTemplate::class,
        FieldValue::class,
        Attachment::class,
        OfflineMap::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PinmapDatabase : RoomDatabase() {
    abstract fun pinDao(): PinDao
    abstract fun categoryDao(): CategoryDao
    abstract fun customFieldDao(): CustomFieldDao
    abstract fun fieldTemplateDao(): FieldTemplateDao
    abstract fun fieldValueDao(): FieldValueDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun offlineMapDao(): OfflineMapDao

    companion object {
        @Volatile
        private var INSTANCE: PinmapDatabase? = null

        fun getDatabase(context: Context): PinmapDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PinmapDatabase::class.java,
                    "pinmap_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}