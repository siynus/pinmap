package com.sinus.pinmap.ui.screens

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
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
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinEditScreen(
    pinId: Long,
    lat: Double = 0.0,
    lng: Double = 0.0,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { PinmapDatabase.getDatabase(context) }
    val pinRepository = remember { PinRepository(database.pinStore()) }
    val categoryRepository = remember { CategoryRepository(database.categoryStore()) }
    val templateRepo = remember { FieldTemplateRepository(database.fieldTemplateStore()) }
    val valueRepo = remember { FieldValueRepository(database.fieldValueStore()) }

    val isCreate = pinId == 0L

    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }
    var templates by remember { mutableStateOf<List<FieldTemplate>>(emptyList()) }
    var fieldValues by remember { mutableStateOf<Map<Long, FieldValue>>(emptyMap()) }
    var editingValues by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var showAddFieldDialog by remember { mutableStateOf(false) }
    var fieldToDelete by remember { mutableStateOf<Long?>(null) }
    var currentFieldKey by remember { mutableStateOf<Long?>(null) }
    var viewingImageUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        categories = categoryRepository.getAllCategories().first()
        if (!isCreate) {
            pinRepository.getPinById(pinId)?.let { pin ->
                title = pin.title
                selectedCategory = categories.find { it.id == pin.categoryId }
                selectedCategory?.let { cat ->
                    templates = templateRepo.getFieldTemplatesByCategory(cat.id).first()
                }
                val values = valueRepo.getFieldValuesByPin(pinId).first()
                fieldValues = values.associateBy { it.fieldTemplateId ?: it.id }
            }
        }
    }

    LaunchedEffect(selectedCategory) {
        selectedCategory?.let { cat ->
            templates = templateRepo.getFieldTemplatesByCategory(cat.id).first()
            if (isCreate) {
                fieldValues = emptyMap()
                editingValues = emptyMap()
            }
        }
    }

    var hasImagePermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES
                else Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val imagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { hasImagePermission = it }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            currentFieldKey?.let { key ->
                scope.launch {
                    try {
                        val timeStamp = System.currentTimeMillis()
                        val fileName = "IMG_${timeStamp}.jpg"
                        val imagesDir = File(context.filesDir, "images")
                        if (!imagesDir.exists()) imagesDir.mkdirs()
                        val imageFile = File(imagesDir, fileName)
                        context.contentResolver.openInputStream(selectedUri)?.use { input ->
                            FileOutputStream(imageFile).use { output -> input.copyTo(output) }
                        }
                        editingValues = editingValues + (key to Uri.fromFile(imageFile).toString())
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
            currentFieldKey = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isCreate) "新建标记" else "编辑标记") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("标题") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("分类") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Box(modifier = Modifier.size(24.dp).background(Color(category.color), CircleShape))
                                            Text(category.name)
                                        }
                                    },
                                    onClick = { selectedCategory = category; expanded = false },
                                    leadingIcon = if (selectedCategory?.id == category.id) {
                                        { Icon(Icons.Default.Check, contentDescription = null) }
                                    } else null
                                )
                            }
                        }
                    }
                }

                if (isCreate && selectedCategory == null) {
                    item {
                        Text("请选择一个分类", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }

                if (templates.isNotEmpty()) {
                    item {
                        Text("字段", style = MaterialTheme.typography.titleSmall)
                    }
                }

                items(templates) { template ->
                    val value = editingValues[template.id] ?: fieldValues[template.id]?.value ?: ""
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = template.fieldName, style = MaterialTheme.typography.bodyMedium)
                                IconButton(onClick = { fieldToDelete = template.id }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "删除", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            when (template.fieldType) {
                                FieldType.TEXT -> {
                                    OutlinedTextField(value = value, onValueChange = { editingValues = editingValues + (template.id to it) }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                                }
                                FieldType.NUMBER -> {
                                    OutlinedTextField(value = value, onValueChange = { editingValues = editingValues + (template.id to it) }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                                }
                                FieldType.DATE -> {
                                    OutlinedTextField(value = value, onValueChange = { editingValues = editingValues + (template.id to it) }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("YYYY-MM-DD") })
                                }
                                FieldType.IMAGE -> {
                                    if (value.isNotBlank()) {
                                        Box(modifier = Modifier.fillMaxWidth().clickable { viewingImageUrl = value }) {
                                            Image(
                                                painter = coil.compose.rememberAsyncImagePainter(value),
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 400.dp),
                                                contentScale = ContentScale.Fit
                                            )
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = {
                                                if (hasImagePermission) {
                                                    currentFieldKey = template.id
                                                    imagePickerLauncher.launch("image/*")
                                                } else {
                                                    imagePermissionLauncher.launch(
                                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES
                                                        else Manifest.permission.READ_EXTERNAL_STORAGE
                                                    )
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) { Text("选择图片") }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedButton(
                        onClick = { showAddFieldDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("添加字段") }
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        val categoryId = selectedCategory?.id ?: return@launch
                        val id = if (isCreate) {
                            pinRepository.insertPin(Pin(latitude = lat, longitude = lng, title = title, categoryId = categoryId))
                        } else {
                            pinRepository.getPinById(pinId)?.let { pin ->
                                pinRepository.updatePin(pin.copy(title = title, categoryId = categoryId))
                            }
                            pinId
                        }
                        val allValues = templates.map { template ->
                            val v = editingValues[template.id] ?: fieldValues[template.id]?.value ?: ""
                            val existing = fieldValues[template.id]
                            if (existing != null) {
                                existing.copy(value = v)
                            } else {
                                FieldValue(pinId = id, fieldTemplateId = template.id, value = v)
                            }
                        }
                        valueRepo.getFieldValuesByPin(id).first().forEach { fv ->
                            if (fv.fieldTemplateId !in templates.map { it.id }) {
                                valueRepo.deleteFieldValueById(fv.id)
                            }
                        }
                        allValues.forEach { fv ->
                            if (fv.id == 0L) valueRepo.insertFieldValue(fv) else valueRepo.updateFieldValue(fv)
                        }
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                enabled = title.isNotBlank() && selectedCategory != null
            ) { Text("保存") }
        }
    }

    if (showAddFieldDialog) {
        CreateFieldTemplateDialog(
            onConfirm = { name, type ->
                scope.launch {
                    val catId = selectedCategory?.id ?: return@launch
                    val t = FieldTemplate(categoryId = catId, fieldName = name, fieldType = type)
                    templateRepo.insertFieldTemplate(t)
                    templates = templateRepo.getFieldTemplatesByCategory(catId).first()
                }
                showAddFieldDialog = false
            },
            onDismiss = { showAddFieldDialog = false }
        )
    }

    fieldToDelete?.let { key ->
        AlertDialog(
            onDismissRequest = { fieldToDelete = null },
            title = { Text("删除字段") },
            text = { Text("确定要删除这个字段吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val fv = fieldValues[key]
                            if (fv != null) {
                                valueRepo.deleteFieldValueById(fv.id)
                                if (fv.fieldTemplateId != null) {
                                    templateRepo.deleteFieldTemplateById(fv.fieldTemplateId)
                                }
                            }
                        }
                        fieldToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { fieldToDelete = null }) { Text("取消") } }
        )
    }

    viewingImageUrl?.let { url ->
        Dialog(onDismissRequest = { viewingImageUrl = null }) {
            Box(modifier = Modifier.fillMaxSize().clickable { viewingImageUrl = null }) {
                Image(
                    painter = coil.compose.rememberAsyncImagePainter(url),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).size(48.dp).clickable { viewingImageUrl = null }
                ) {
                    Icon(Icons.Default.Close, contentDescription = "关闭", modifier = Modifier.padding(12.dp))
                }
            }
        }
    }
}
