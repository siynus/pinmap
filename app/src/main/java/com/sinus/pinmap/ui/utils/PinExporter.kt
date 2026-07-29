package com.sinus.pinmap.ui.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.sinus.pinmap.data.database.PinmapDatabase
import com.sinus.pinmap.data.entity.FieldTemplate
import com.sinus.pinmap.data.entity.FieldType
import com.sinus.pinmap.data.entity.FieldValue
import com.sinus.pinmap.data.entity.Pin
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object PinExporter {

    suspend fun exportAll(
        context: Context,
        database: PinmapDatabase,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
        fileName: String = "pinmap_export.pinmap"
    ): Uri {
        val pinRepo = database.pinStore()
        val categoryRepo = database.categoryStore()
        val templateRepo = database.fieldTemplateStore()
        val valueRepo = database.fieldValueStore()

        val cats = com.sinus.pinmap.data.repository.CategoryRepository(categoryRepo).getAllCategories().first()
        val templates = com.sinus.pinmap.data.repository.FieldTemplateRepository(templateRepo).getAllFieldTemplates().first()
        val pins = com.sinus.pinmap.data.repository.PinRepository(pinRepo).getAllPins().first()
        val valueMap = buildValueMap(valueRepo, pins)

        return export(context, cats, templates, pins, valueMap, onProgress, fileName)
    }

    suspend fun exportByCategory(
        context: Context,
        database: PinmapDatabase,
        categoryId: Long,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
        fileName: String = "export_category.pinmap"
    ): Uri {
        val pinRepo = database.pinStore()
        val categoryRepo = database.categoryStore()
        val templateRepo = database.fieldTemplateStore()
        val valueRepo = database.fieldValueStore()

        val cat = com.sinus.pinmap.data.repository.CategoryRepository(categoryRepo).getCategoryById(categoryId)
            ?: throw IllegalStateException("Category not found")
        val templates = com.sinus.pinmap.data.repository.FieldTemplateRepository(templateRepo)
            .getFieldTemplatesByCategory(categoryId).first()
        val pins = com.sinus.pinmap.data.repository.PinRepository(pinRepo).getPinsByCategory(categoryId).first()
        val valueMap = buildValueMap(valueRepo, pins)

        return export(context, listOf(cat), templates, pins, valueMap, onProgress, fileName)
    }

    suspend fun exportByPin(
        context: Context,
        database: PinmapDatabase,
        pinId: Long,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
        fileName: String = "export_pin.pinmap"
    ): Uri {
        val pinRepo = database.pinStore()
        val categoryRepo = database.categoryStore()
        val templateRepo = database.fieldTemplateStore()
        val valueRepo = database.fieldValueStore()

        val pin = com.sinus.pinmap.data.repository.PinRepository(pinRepo).getPinById(pinId)
            ?: throw IllegalStateException("Pin not found")
        val cat = pin.categoryId?.let {
            com.sinus.pinmap.data.repository.CategoryRepository(categoryRepo).getCategoryById(it)
        }
        val templates = if (cat != null) {
            com.sinus.pinmap.data.repository.FieldTemplateRepository(templateRepo)
                .getFieldTemplatesByCategory(cat.id).first()
        } else emptyList()
        val values = com.sinus.pinmap.data.repository.FieldValueRepository(valueRepo)
            .getFieldValuesByPin(pinId).first()

        return export(
            context,
            if (cat != null) listOf(cat) else emptyList(),
            templates, listOf(pin), mapOf(pinId to values),
            onProgress, fileName
        )
    }

    private suspend fun buildValueMap(
        valueRepo: com.sinus.pinmap.data.store.FieldValueStore,
        pins: List<Pin>
    ): Map<Long, List<FieldValue>> {
        val repo = com.sinus.pinmap.data.repository.FieldValueRepository(valueRepo)
        return pins.associate { it.id to repo.getFieldValuesByPin(it.id).first() }
    }

    private suspend fun export(
        context: Context,
        categories: List<com.sinus.pinmap.data.entity.Category>,
        fieldTemplates: List<FieldTemplate>,
        pins: List<Pin>,
        fieldValues: Map<Long, List<FieldValue>>,
        onProgress: (done: Int, total: Int) -> Unit,
        fileName: String
    ): Uri {
        val exportDir = File(context.cacheDir, "export").apply { mkdirs() }
        val exportFile = File(exportDir, fileName)

        val templateTypeMap = fieldTemplates.associate { it.id to it.fieldType }
        val fileRefs = mutableListOf<Pair<String, String>>() // (sourcePath, entryName)

        // First pass: collect all files
        val seen = mutableSetOf<String>()
        fun addFile(sourcePath: String): String {
            if (sourcePath in seen) {
                return fileRefs.first { it.first == sourcePath }.second
            }
            seen.add(sourcePath)
            val name = File(sourcePath).name
            var entry = "files/$name"
            var suffix = 1
            while (fileRefs.any { it.second == entry }) {
                val base = name.substringBeforeLast('.')
                val ext = name.substringAfterLast('.', "")
                entry = "files/${base}_$suffix.$ext"
                suffix++
            }
            fileRefs.add(sourcePath to entry)
            return entry
        }

        fun getFieldType(fv: FieldValue): FieldType? {
            return fv.fieldTemplateId?.let { templateTypeMap[it] }
        }

        fun resolveFilePath(raw: String): String? {
            return if (raw.startsWith("file://")) Uri.parse(raw).path else raw
        }

        fun pinToJson(pin: Pin, fvs: List<FieldValue>): JSONObject {
            val fvArray = JSONArray()
            fvs.forEach { fv ->
                val isMedia = getFieldType(fv)?.let { it == FieldType.IMAGE || it == FieldType.VIDEO } ?: false
                var value = fv.value
                if (isMedia && !value.isNullOrBlank()) {
                    val path = resolveFilePath(value)
                    if (path != null) value = addFile(path)
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
                    resolveFilePath(path)?.let { put("avatarFile", addFile(it)) }
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

        // Build ZIP
        val total = fileRefs.size + 1  // +1 for data.json
        var done = 0

        ZipOutputStream(exportFile.outputStream()).use { zos ->
            zos.putNextEntry(ZipEntry("data.json"))
            zos.write(root.toString(2).toByteArray())
            zos.closeEntry()
            done++
            onProgress(done, total)

            fileRefs.forEach { (sourcePath, entryName) ->
                val file = File(sourcePath)
                if (file.exists()) {
                    zos.putNextEntry(ZipEntry(entryName))
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
                done++
                onProgress(done, total)
            }
        }

        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", exportFile)
    }
}
