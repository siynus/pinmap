package com.sinus.pinmap.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sinus.pinmap.data.store.AttachmentStore
import com.sinus.pinmap.data.store.CategoryStore
import com.sinus.pinmap.data.store.FieldTemplateStore
import com.sinus.pinmap.data.store.FieldValueStore
import com.sinus.pinmap.data.store.OfflineMapStore
import com.sinus.pinmap.data.store.PinStore
import com.sinus.pinmap.data.entity.Attachment
import com.sinus.pinmap.data.entity.Category
import com.sinus.pinmap.data.entity.FieldTemplate
import com.sinus.pinmap.data.entity.FieldValue
import com.sinus.pinmap.data.entity.OfflineMap
import com.sinus.pinmap.data.entity.Pin

@Database(
    entities = [
        Pin::class,
        Category::class,
        FieldTemplate::class,
        FieldValue::class,
        Attachment::class,
        OfflineMap::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PinmapDatabase : RoomDatabase() {
    abstract fun pinStore(): PinStore
    abstract fun categoryStore(): CategoryStore
    abstract fun fieldTemplateStore(): FieldTemplateStore
    abstract fun fieldValueStore(): FieldValueStore
    abstract fun attachmentStore(): AttachmentStore
    abstract fun offlineMapStore(): OfflineMapStore

    companion object {
        @Volatile
        private var INSTANCE: PinmapDatabase? = null

        val MIGRATION_3_4 = Migration(3, 4) { db ->
            db.execSQL("ALTER TABLE field_templates ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
        }

        fun getDatabase(context: Context): PinmapDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PinmapDatabase::class.java,
                    "pinmap_database"
                )
                    .addMigrations(MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
