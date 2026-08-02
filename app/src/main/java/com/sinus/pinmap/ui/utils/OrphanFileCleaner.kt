package com.sinus.pinmap.ui.utils

import android.content.Context
import android.util.Log
import com.sinus.pinmap.data.database.PinmapDatabase
import kotlinx.coroutines.flow.first
import java.io.File

/**
 * 孤儿文件清扫：删除 images/、avatars/ 目录中数据库已无引用的文件。
 */
object OrphanFileCleaner {

    private const val TAG = "OrphanCleaner"

    /**
     * 返回删除的文件数。
     */
    suspend fun clean(context: Context, database: PinmapDatabase): Int {
        val pinRepo = com.sinus.pinmap.data.repository.PinRepository(database.pinStore())
        val valueRepo = com.sinus.pinmap.data.repository.FieldValueRepository(database.fieldValueStore())

        val pins = pinRepo.getAllPins().first()
        val values = valueRepo.getAllFieldValues().first()

        // 收集所有被引用路径（已解析为绝对路径）
        val referenced = mutableSetOf<String>()
        pins.forEach { pin -> pin.avatarPath?.let { referenced.add(resolve(it)) } }
        values.forEach { fv -> fv.value?.let { referenced.add(resolve(it)) } }

        val dirs = listOf(
            File(context.filesDir, "images"),
            File(context.filesDir, "avatars")
        )

        var deleted = 0
        for (dir in dirs) {
            if (!dir.exists()) continue
            dir.listFiles()?.forEach { file ->
                if (file.isFile && file.path !in referenced) {
                    if (file.delete()) {
                        deleted++
                    }
                }
            }
        }
        Log.d(TAG, "clean done: deleted=$deleted referenced=${referenced.size}")
        return deleted
    }

    private fun resolve(raw: String): String {
        return if (raw.startsWith("file://")) {
            android.net.Uri.parse(raw).path ?: raw
        } else {
            raw
        }
    }
}
