package com.sinus.pinmap.ui.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.room.withTransaction
import com.sinus.pinmap.data.database.PinmapDatabase
import com.sinus.pinmap.data.entity.Category
import com.sinus.pinmap.data.entity.FieldTemplate
import com.sinus.pinmap.data.entity.FieldType
import com.sinus.pinmap.data.entity.FieldValue
import com.sinus.pinmap.data.entity.Pin
import com.sinus.pinmap.data.repository.CategoryRepository
import com.sinus.pinmap.data.repository.FieldTemplateRepository
import com.sinus.pinmap.data.repository.FieldValueRepository
import com.sinus.pinmap.data.repository.PinRepository
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

object PinImporter {

    private const val TAG = "PinImporter"
    private const val COPY_BUFFER_SIZE = 64 * 1024

    data class ImportPreview(
        val categoryCount: Int,
        val templateCount: Int,
        val pinCount: Int
    )

    data class ImportResult(
        val categoryCount: Int,
        val templateCount: Int,
        val pinCount: Int,
        val mediaCount: Int,
        val droppedValues: Int
    )

    private data class TemplateData(
        val oldId: Long,
        val fieldName: String,
        val fieldType: FieldType,
        val sortOrder: Int
    )

    private data class FieldValueData(
        val oldTemplateId: Long,
        val value: String
    )

    private data class PinData(
        val latitude: Double,
        val longitude: Double,
        val title: String,
        val description: String?,
        val address: String?,
        val avatarEntry: String?,
        val createdAt: Long,
        val updatedAt: Long,
        val values: List<FieldValueData>
    )

    private data class CategoryData(
        val name: String,
        val color: Int,
        val icon: String?,
        val createdAt: Long,
        val updatedAt: Long,
        val templates: List<TemplateData>,
        val pins: List<PinData>
    )

    private data class ImportFile(
        val categories: List<CategoryData>
    )

    /**
     * 解析 .pinmap 文件，返回统计信息用于导入确认。
     * 格式错误时抛异常。
     */
    suspend fun preview(context: Context, uri: Uri): ImportPreview {
        val file = parseFile(context, uri)
        Log.d(TAG, "preview: categories=${file.categories.size}, templates=${file.categories.sumOf { it.templates.size }}, pins=${file.categories.sumOf { it.pins.size }}")
        return ImportPreview(
            categoryCount = file.categories.size,
            templateCount = file.categories.sumOf { it.templates.size },
            pinCount = file.categories.sumOf { it.pins.size }
        )
    }

    /**
     * 执行导入：
     * 1. 分类按名称合并（模板取并集），标记全部新增
     * 2. 媒体文件解压到私有目录并按规则重命名，回写引用
     * 3. 模板 id 映射到新 id
     * 4. 数据库事务写入
     */
    suspend fun import(
        context: Context,
        database: PinmapDatabase,
        uri: Uri,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }
    ): ImportResult {
        Log.d(TAG, "import start, uri=$uri")
        val file = parseFile(context, uri)
        Log.d(TAG, "parsed: categories=${file.categories.size}, pins=${file.categories.sumOf { it.pins.size }}, templates=${file.categories.sumOf { it.templates.size }}")

        // 第一步：收集所有需要解压的媒体条目
        val mediaEntries = mutableSetOf<String>()
        val avatarEntries = mutableSetOf<String>()
        for (cat in file.categories) {
            for (pin in cat.pins) {
                pin.avatarEntry?.let {
                    mediaEntries.add(it)
                    avatarEntries.add(it)
                }
                for (fv in pin.values) {
                    val template = cat.templates.firstOrNull { it.oldId == fv.oldTemplateId }
                    if (template != null &&
                        (template.fieldType == FieldType.IMAGE || template.fieldType == FieldType.VIDEO) &&
                        fv.value.isNotEmpty()
                    ) {
                        mediaEntries.add(fv.value)
                    }
                }
            }
        }
        Log.d(TAG, "media entries to extract: ${mediaEntries.size} (avatar: ${avatarEntries.size})")

        // 第二步：解压媒体文件，得到 oldEntry -> newPath 映射
        val mediaDir = File(context.filesDir, "images").apply { mkdirs() }
        val avatarDir = File(context.filesDir, "avatars").apply { mkdirs() }

        val totalWork = file.categories.size + file.categories.sumOf { it.pins.size } + mediaEntries.size + 1
        var done = 0

        val mediaPathMap = extractMedia(
            context, uri, mediaEntries, avatarEntries, mediaDir, avatarDir,
            onProgress = {
                done++
                onProgress(done, totalWork)
            }
        )
        Log.d(TAG, "media extracted: ${mediaPathMap.size}/${mediaEntries.size}")

        // 第三步：事务写入
        val result = database.withTransaction {
            Log.d(TAG, "db transaction start")
            val categoryRepo = CategoryRepository(database.categoryStore())
            val templateRepo = FieldTemplateRepository(database.fieldTemplateStore())
            val pinRepo = PinRepository(database.pinStore())
            val valueRepo = FieldValueRepository(database.fieldValueStore())

            var existingCategories = categoryRepo.getAllCategories().first()
            var newCategoryCount = 0
            var newTemplateCount = 0
            var newPinCount = 0
            var mediaCount = 0
            var droppedValues = 0

            for (catData in file.categories) {
                Log.d(TAG, "import category: ${catData.name}")
                // 分类：按名称合并
                var category = existingCategories.firstOrNull { it.name == catData.name }
                if (category == null) {
                    val newId = categoryRepo.insertCategory(
                        Category(
                            name = catData.name,
                            color = catData.color,
                            icon = catData.icon,
                            createdAt = catData.createdAt,
                            updatedAt = catData.updatedAt
                        )
                    )
                    category = Category(
                        id = newId,
                        name = catData.name,
                        color = catData.color,
                        icon = catData.icon,
                        createdAt = catData.createdAt,
                        updatedAt = catData.updatedAt
                    )
                    existingCategories = existingCategories + category
                    newCategoryCount++
                }

                // 模板：按 (分类, 字段名) 合并，取并集
                val oldTemplateIdMap = mutableMapOf<Long, Long>()
                var existingTemplates = templateRepo.getFieldTemplatesByCategory(category.id).first()
                for (templateData in catData.templates) {
                    var template = existingTemplates.firstOrNull { it.fieldName == templateData.fieldName }
                    if (template == null) {
                        val nextOrder = templateRepo.nextSortOrder(category.id)
                        val newId = templateRepo.insertFieldTemplate(
                            FieldTemplate(
                                categoryId = category.id,
                                fieldName = templateData.fieldName,
                                fieldType = templateData.fieldType,
                                sortOrder = nextOrder
                            )
                        )
                        template = FieldTemplate(
                            id = newId,
                            categoryId = category.id,
                            fieldName = templateData.fieldName,
                            fieldType = templateData.fieldType,
                            sortOrder = nextOrder
                        )
                        existingTemplates = existingTemplates + template
                        newTemplateCount++
                    }
                    oldTemplateIdMap[templateData.oldId] = template.id
                }
                val templateTypeMap = existingTemplates.associate { it.id to it.fieldType }

                // 标记：全部新增
                for (pinData in catData.pins) {
                    Log.d(TAG, "import pin: ${pinData.title}")
                    var avatarPath: String? = null
                    if (pinData.avatarEntry != null) {
                        avatarPath = mediaPathMap[pinData.avatarEntry]
                        if (avatarPath != null) mediaCount++
                    }

                    val newPinId = pinRepo.insertPin(
                        Pin(
                            latitude = pinData.latitude,
                            longitude = pinData.longitude,
                            title = pinData.title,
                            description = pinData.description,
                            categoryId = category.id,
                            avatarPath = avatarPath,
                            address = pinData.address,
                            createdAt = pinData.createdAt,
                            updatedAt = pinData.updatedAt
                        )
                    )
                    newPinCount++

                    for (fvData in pinData.values) {
                        val newTid = oldTemplateIdMap[fvData.oldTemplateId]
                        if (newTid == null) {
                            droppedValues++
                            continue
                        }
                        val ftType = templateTypeMap[newTid]
                        var value = fvData.value
                        if (ftType == FieldType.IMAGE || ftType == FieldType.VIDEO) {
                            val newPath = mediaPathMap[fvData.value]
                            if (newPath != null) {
                                value = newPath
                                mediaCount++
                            }
                        }
                        valueRepo.insertFieldValue(
                            FieldValue(
                                pinId = newPinId,
                                fieldTemplateId = newTid,
                                value = value
                            )
                        )
                    }
                    done++
                    onProgress(done, totalWork)
                }
                done++
                onProgress(done, totalWork)
            }

            ImportResult(newCategoryCount, newTemplateCount, newPinCount, mediaCount, droppedValues)
        }
        Log.d(TAG, "import done: result=$result")

        return result
    }

    private fun parseFile(context: Context, uri: Uri): ImportFile {
        val json = readDataJson(context, uri) ?: throw IllegalArgumentException("文件中缺少 data.json")
        val categoriesJson = json.optJSONArray("categories") ?: return ImportFile(emptyList())

        val categories = mutableListOf<CategoryData>()
        for (i in 0 until categoriesJson.length()) {
            val catJson = categoriesJson.optJSONObject(i) ?: continue

            val templates = mutableListOf<TemplateData>()
            val templatesJson = catJson.optJSONArray("fieldTemplates")
            if (templatesJson != null) {
                for (j in 0 until templatesJson.length()) {
                    val ftJson = templatesJson.optJSONObject(j) ?: continue
                    val oldId = ftJson.optLong("id", -1L)
                    templates.add(
                        TemplateData(
                            oldId = oldId,
                            fieldName = ftJson.optString("fieldName", ""),
                            fieldType = runCatching { FieldType.valueOf(ftJson.optString("fieldType", "")) }
                                .getOrDefault(FieldType.TEXT),
                            sortOrder = ftJson.optInt("sortOrder", 0)
                        )
                    )
                }
            }

            val pins = mutableListOf<PinData>()
            val pinsJson = catJson.optJSONArray("pins")
            if (pinsJson != null) {
                for (k in 0 until pinsJson.length()) {
                    val pinJson = pinsJson.optJSONObject(k) ?: continue
                    val values = mutableListOf<FieldValueData>()
                    val valuesJson = pinJson.optJSONArray("fieldValues")
                    if (valuesJson != null) {
                        for (v in 0 until valuesJson.length()) {
                            val fvJson = valuesJson.optJSONObject(v) ?: continue
                            val oldTid = fvJson.optLong("fieldTemplateId", -1L)
                            if (oldTid >= 0) {
                                values.add(FieldValueData(oldTid, fvJson.optNullableString("value") ?: ""))
                            }
                        }
                    }
                    pins.add(
                        PinData(
                            latitude = pinJson.optDouble("latitude", 0.0),
                            longitude = pinJson.optDouble("longitude", 0.0),
                            title = pinJson.optString("title", "未命名"),
                            description = pinJson.optNullableString("description"),
                            address = pinJson.optNullableString("address"),
                            avatarEntry = pinJson.optNullableString("avatarFile"),
                            createdAt = pinJson.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = pinJson.optLong("updatedAt", System.currentTimeMillis()),
                            values = values
                        )
                    )
                }
            }

            categories.add(
                CategoryData(
                    name = catJson.optString("name", "未命名"),
                    color = catJson.optInt("color", 0xFF03A9F4.toInt()),
                    icon = catJson.optNullableString("icon"),
                    createdAt = catJson.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = catJson.optLong("updatedAt", System.currentTimeMillis()),
                    templates = templates,
                    pins = pins
                )
            )
        }
        return ImportFile(categories)
    }

    private fun JSONObject.optNullableString(name: String): String? {
        val value = opt(name) ?: return null
        if (value == JSONObject.NULL) return null
        return value.toString().takeIf { it.isNotEmpty() }
    }

    private fun readDataJson(context: Context, uri: Uri): JSONObject? {
        val resolver = context.contentResolver
        resolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name == "data.json") {
                        val text = zis.readBytes().toString(Charsets.UTF_8)
                        return JSONObject(text)
                    }
                    entry = zis.nextEntry
                }
            }
        }
        return null
    }

    private fun extractMedia(
        context: Context,
        uri: Uri,
        entries: Set<String>,
        avatarEntries: Set<String>,
        mediaDir: File,
        avatarDir: File,
        onProgress: () -> Unit = {}
    ): Map<String, String> {
        if (entries.isEmpty()) return emptyMap()
        Log.d(TAG, "extractMedia: entries=$entries")
        val result = mutableMapOf<String, String>()
        val usedNames = mutableSetOf<String>()
        val resolver = context.contentResolver
        resolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val entryName = entry.name
                    if (entryName in entries) {
                        val targetDir = if (entryName in avatarEntries) avatarDir else mediaDir
                        val originalName = entryName.substringAfterLast('/')
                        val ext = originalName.substringAfterLast('.', "jpg").takeIf { it.length in 1..5 } ?: "jpg"
                        var fileName = "IMG_${System.currentTimeMillis()}_${usedNames.size}.$ext"
                        while (fileName in usedNames) {
                            fileName = "IMG_${System.currentTimeMillis()}_${usedNames.size}.$ext"
                        }
                        usedNames.add(fileName)
                        val dest = File(targetDir, fileName)
                        FileOutputStream(dest).use { out -> zis.copyTo(out, COPY_BUFFER_SIZE) }
                        result[entryName] = Uri.fromFile(dest).toString()
                        Log.d(TAG, "extracted: $entryName -> $dest")
                        onProgress()
                    }
                    entry = zis.nextEntry
                }
            }
        }
        return result
    }
}
