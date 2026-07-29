package com.sinus.pinmap.ui.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.sinus.pinmap.data.entity.FieldTemplate
import com.sinus.pinmap.data.entity.FieldType
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

        val templateTypeMap = fieldTemplates.associate { it.id to it.fieldType }
        val dedupMap = mutableMapOf<String, String>()

        fun getFieldType(fv: FieldValue): FieldType? {
            return fv.fieldTemplateId?.let { templateTypeMap[it] }
        }

        fun resolveFilePath(raw: String): String? {
            return if (raw.startsWith("file://")) Uri.parse(raw).path else raw
        }

        fun dedup(sourcePath: String): String {
            return dedupMap.getOrPut(sourcePath) {
                val fileName = File(sourcePath).name
                val entryName = "files/$fileName"
                // handle same-named files from different paths
                var unique = entryName
                var suffix = 1
                while (dedupMap.values.contains(unique)) {
                    val base = fileName.substringBeforeLast('.')
                    val ext = fileName.substringAfterLast('.', "")
                    unique = "files/${base}_$suffix.$ext"
                    suffix++
                }
                unique
            }
        }

        fun pinToJson(pin: Pin, fvs: List<FieldValue>): JSONObject {
            val fvArray = JSONArray()
            fvs.forEach { fv ->
                val isMedia = getFieldType(fv)?.let { it == FieldType.IMAGE || it == FieldType.VIDEO } ?: false
                var value = fv.value
                if (isMedia && !value.isNullOrBlank()) {
                    val path = resolveFilePath(value)
                    if (path != null) {
                        value = dedup(path)
                    }
                }
                fvArray.put(JSONObject().apply {
                    put("fieldTemplateId", fv.fieldTemplateId)
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
                    resolveFilePath(path)?.let { put("avatarFile", dedup(it)) }
                }
                put("createdAt", pin.createdAt)
                put("updatedAt", pin.updatedAt)
                put("fieldValues", fvArray)
            }
        }

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
