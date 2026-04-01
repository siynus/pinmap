package com.sinus.pinmap.ui.screens

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import com.amap.api.maps.model.LatLng
import com.sinus.pinmap.data.entity.Category
import com.sinus.pinmap.data.entity.FieldType
import com.sinus.pinmap.ui.model.FieldData
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * 创建标记对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePinDialog(
    location: LatLng,
    address: String,
    categories: List<Category>,
    categoryTemplates: Map<Long, List<com.sinus.pinmap.data.entity.FieldTemplate>> = emptyMap(),
    onConfirm: (title: String, categoryId: Long, fields: List<FieldData>) -> Unit,
    onCreateCategory: () -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var showCreateFieldDialog by remember { mutableStateOf(false) }
    var tempFields by remember { mutableStateOf<List<FieldData>>(emptyList()) }
    var nextFieldId by remember { mutableStateOf(0L) }

    // 当选择类别时，自动添加该类别的模板字段
    LaunchedEffect(selectedCategory) {
        selectedCategory?.let { category ->
            val templates = categoryTemplates[category.id] ?: emptyList()
            // 只添加还没在 tempFields 中的模板字段
            val newTemplates = templates.filter { template ->
                tempFields.none { it.name == template.fieldName }
            }
            tempFields = tempFields + newTemplates.map { template ->
                FieldData(
                    id = nextFieldId++,
                    name = template.fieldName,
                    type = template.fieldType,
                    value = "",
                    isTemplate = true
                )
            }
        }
    }

    val context = LocalContext.current
    var currentFieldId by remember { mutableStateOf<Long?>(null) }
    var viewingImageUrl by remember { mutableStateOf<String?>(null) }

    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { sourceUri ->
            currentFieldId?.let { fieldId ->
                // 复制图片到应用的私有存储
                try {
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    val fileName = "IMG_$timestamp.jpg"
                    val destFile = File(context.filesDir, "images/$fileName")
                    
                    // 确保目录存在
                    destFile.parentFile?.mkdirs()
                    
                    // 复制文件
                    context.contentResolver.openInputStream(sourceUri)?.use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    
                    // 更新字段值
                    val fileUri = Uri.fromFile(destFile).toString()
                    tempFields = tempFields.map {
                        if (it.id == fieldId) it.copy(value = fileUri) else it
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        currentFieldId = null
    }

    // 创建临时字段目录
    LaunchedEffect(Unit) {
        val imagesDir = File(context.filesDir, "images")
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "创建新标记",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 显示地址
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 类别选择器
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("选择分类 *") },
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = if (selectedCategory == null) {
                            OutlinedTextFieldDefaults.colors(
                                errorBorderColor = MaterialTheme.colorScheme.error
                            )
                        } else {
                            OutlinedTextFieldDefaults.colors()
                        }
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        // 创建新分类选项
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text("创建新分类")
                                }
                            },
                            onClick = {
                                expanded = false
                                onCreateCategory()
                            }
                        )

                        Divider()

                        // 分类列表
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(Color(category.color), CircleShape)
                                        )
                                        Text(category.name)
                                    }
                                },
                                onClick = {
                                    selectedCategory = category
                                    expanded = false
                                },
                                leadingIcon = if (selectedCategory?.id == category.id) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                }

                if (selectedCategory == null) {
                    Text(
                        text = "请选择一个分类",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 自定义字段部分
                Text(
                    text = "自定义字段",
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (tempFields.isEmpty()) {
                    Text(
                        text = "还没有添加字段",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tempFields) { field ->
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = field.name,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            if (field.isTemplate) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "模板",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        TextButton(
                                            onClick = {
                                                tempFields = tempFields.filter { it.id != field.id }
                                            }
                                        ) {
                                            Text("删除", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (field.type == FieldType.IMAGE) {
                                        // 图片类型显示
                                        if (field.value.isNotBlank()) {
                                            // 显示图片预览
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(8.dp)
                                                    .clickable {
                                                        viewingImageUrl = field.value
                                                    }
                                            ) {
                                                Image(
                                                    painter = rememberAsyncImagePainter(field.value),
                                                    contentDescription = field.name,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .heightIn(min = 200.dp, max = 400.dp),
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                                )
                                                
                                                // 删除按钮
                                                Surface(
                                                    color = MaterialTheme.colorScheme.error,
                                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                                                    modifier = Modifier
                                                        .align(androidx.compose.ui.Alignment.TopEnd)
                                                        .padding(8.dp)
                                                        .size(32.dp)
                                                        .clickable { 
                                                            tempFields = tempFields.filter { it.id != field.id }
                                                        }
                                                ) {
                                                    Icon(
                                                        androidx.compose.material.icons.Icons.Default.Close,
                                                        contentDescription = "删除图片",
                                                        tint = MaterialTheme.colorScheme.onError,
                                                        modifier = Modifier.padding(6.dp)
                                                    )
                                                }
                                            }
                                        } else {
                                            // 显示选择图片按钮
                                            OutlinedButton(
                                                onClick = {
                                                    currentFieldId = field.id
                                                    imagePickerLauncher.launch("image/*")
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("选择图片")
                                            }
                                        }
                                    } else {
                                        // 文本和数字类型
                                        OutlinedTextField(
                                            value = field.value,
                                            onValueChange = { newValue ->
                                                tempFields = tempFields.map {
                                                    if (it.id == field.id) it.copy(value = newValue) else it
                                                }
                                            },
                                            placeholder = {
                                                Text(
                                                    when (field.type) {
                                                        FieldType.TEXT -> "输入文本"
                                                        FieldType.NUMBER -> "输入数字"
                                                        FieldType.DATE -> "日期 (YYYY-MM-DD)"
                                                        else -> "输入值"
                                                    }
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 添加字段按钮
                OutlinedButton(
                    onClick = { showCreateFieldDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("添加字段")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            selectedCategory?.let {
                                onConfirm(title, it.id, tempFields)
                            }
                        },
                        enabled = title.isNotBlank() && selectedCategory != null
                    ) {
                        Text("创建")
                    }
                }
            }
        }
    }

    // 创建字段对话框
    if (showCreateFieldDialog) {
        var fieldName by remember { mutableStateOf("") }
        var fieldType by remember { mutableStateOf(FieldType.TEXT) }
        var isTemplate by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showCreateFieldDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "添加字段",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = fieldName,
                        onValueChange = { fieldName = it },
                        label = { Text("字段名称") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "字段类型",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = fieldType == FieldType.TEXT,
                            onClick = { fieldType = FieldType.TEXT },
                            label = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Text("文本")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        FilterChip(
                            selected = fieldType == FieldType.IMAGE,
                            onClick = { fieldType = FieldType.IMAGE },
                            label = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Text("图片")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        FilterChip(
                            selected = fieldType == FieldType.NUMBER,
                            onClick = { fieldType = FieldType.NUMBER },
                            label = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Text("数字")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 是否添加到通用模板
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isTemplate,
                            onCheckedChange = { isTemplate = it }
                        )
                        Text(
                            text = "添加到当前类别的通用模板",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showCreateFieldDialog = false }) {
                            Text("取消")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                tempFields = tempFields + FieldData(
                                    id = nextFieldId++,
                                    name = fieldName,
                                    type = fieldType,
                                    value = "",
                                    isTemplate = isTemplate
                                )
                                showCreateFieldDialog = false
                            },
                            enabled = fieldName.isNotBlank()
                        ) {
                            Text("添加")
                        }
                    }
                }
            }
        }
    }
    
    // 图片查看对话框
    if (viewingImageUrl != null) {
        Dialog(onDismissRequest = { viewingImageUrl = null }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { viewingImageUrl = null }
            ) {
                Image(
                    painter = rememberAsyncImagePainter(viewingImageUrl),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
                
                // 关闭按钮
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.TopEnd)
                        .padding(16.dp)
                        .size(48.dp)
                        .clickable { viewingImageUrl = null }
                ) {
                    Icon(
                        androidx.compose.material.icons.Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}