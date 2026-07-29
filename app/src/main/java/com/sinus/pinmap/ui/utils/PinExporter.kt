package com.sinus.pinmap.ui.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.sinus.pinmap.data.entity.FieldTemplate
import com.sinus.pinmap.data.entity.FieldValue
import com.sinus.pinmap.data.entity.Pin
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object PinExporter {

    suspend fun export(
        context: Context,
        categories: List<com.sinus.pinmap.data.entity.Category>,
        fieldTemplates: List<FieldTemplate>,
        pins: List<Pin>,
        fieldValues: Map<Long, List<FieldValue>>,
        fileName: String = "pinmap_export.pinmap"
    ): Uri {
        val exportDir = File(context.cacheDir, "export").apply { mkdirs() }
        val exportFile = File(exportDir, fileName)

        val dedupMap = mutableMapOf<String, String>()
        var fileCounter = 0

        fun dedup(sourcePath: String): String {
            return dedupMap.getOrPut(sourcePath) {
                val ext = sourcePath.substringAfterLast('.', "")
                val name = "files/${fileCounter++}.$ext"
                name
            }
        }

        fun pinToJson(pin: Pin, fvs: List<FieldValue>): JSONObject {
            val fvArray = JSONArray()
            fvs.forEach { fv ->
                val isMedia = fv.fieldType == com.sinus.pinmap.data.entity.FieldType.IMAGE ||
                        fv.fieldType == com.sinus.pinmap.data.entity.FieldType.VIDEO
                var value = fv.value
                if (isMedia && !value.isNullOrBlank()) {
                    val path = if (value.startsWith("file://")) Uri.parse(value).path else value
                    if (path != null) {
                        value = dedup(path)
                    }
                }
                fvArray.put(JSONObject().apply {
                    if (fv.fieldTemplateId != null) {
                        put("fieldTemplateId", fv.fieldTemplateId)
                    }
                    if (fv.fieldName != null) {
                        put("fieldName", fv.fieldName)
                        put("fieldType", fv.fieldType?.name)
                    }
                    put("value", value)
                })
            }

            return JSONObject().apply {
                put("latitude", pin.latitude)
                put("longitude", pin.longitude)
                put("title", pin.title)
                put("description", pin.description ?: JSONObject.NULL)
                put("address", pin.address ?: JSONObject.NULL)
                pin.avatarPath?.let { path ->
                    val realPath = if (path.startsWith("file://")) Uri.parse(path).path else path
                    if (realPath != null) {
                        put("avatarFile", dedup(realPath))
                    }
                }
                put("createdAt", pin.createdAt)
                put("updatedAt", pin.updatedAt)
                put("fieldValues", fvArray)
            }
        }

        // 按分类组织
        val root = JSONObject().apply {
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())
            val catsArray = JSONArray()
            categories.map { cat ->
                JSONObject().apply {
                    put("name", cat.name)
                    put("color", cat.color)
                    cat.icon?.let { put("icon", it) }
                    put("createdAt", cat.createdAt)
                    put("updatedAt", cat.updatedAt)

                    val ftArray = JSONArray()
                    fieldTemplates.filter { it.categoryId == cat.id }.forEach { ft ->
                        ftArray.put(JSONObject().apply {
                            put("fieldName", ft.fieldName)
                            put("fieldType", ft.fieldType.name)
                            put("sortOrder", ft.sortOrder)
                        })
                    }
                    put("fieldTemplates", ftArray)

                    val pArray = JSONArray()
                    pins.filter { it.categoryId == cat.id }.forEach { pin ->
                        pArray.put(pinToJson(pin, fieldValues[pin.id].orEmpty()))
                    }
                    put("pins", pArray)
                }
            }.forEach { catsArray.put(it) }
            put("categories", catsArray)
        }

        ZipOutputStream(exportFile.outputStream()).use { zos ->
            zos.putNextEntry(ZipEntry("data.json"))
            zos.write(root.toString(2).toByteArray())
            zos.closeEntry()

            dedupMap.forEach { (sourcePath, entryName) ->
                val file = File(sourcePath)
                if (file.exists()) {
                    zos.putNextEntry(ZipEntry(entryName))
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }

        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", exportFile)
    }
}
