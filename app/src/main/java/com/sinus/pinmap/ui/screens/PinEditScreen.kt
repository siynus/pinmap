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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
    var fieldValues by remember { mutableStateOf<Map<Long, List<FieldValue>>>(emptyMap()) }
    var editingValues by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var editingImages by remember { mutableStateOf<Map<Long, List<String>>>(emptyMap()) }
    var showAddFieldDialog by remember { mutableStateOf(false) }
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
                fieldValues = values.groupBy { it.fieldTemplateId ?: it.id }
            }
        }
    }

    LaunchedEffect(selectedCategory) {
        selectedCategory?.let { cat ->
            templates = templateRepo.getFieldTemplatesByCategory(cat.id).first()
            if (isCreate) {
                fieldValues = emptyMap()
                editingValues = emptyMap()
                editingImages = emptyMap()
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

    var pendingImageTemplateId by remember { mutableStateOf<Long?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            pendingImageTemplateId?.let { tid ->
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
                        val uriStr = Uri.fromFile(imageFile).toString()
                        val current = editingImages[tid] ?: emptyList()
                        editingImages = editingImages + (tid to (current + uriStr))
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }
            pendingImageTemplateId = null
        }
    }

    fun getImages(template: FieldTemplate): List<String> {
        val edited = editingImages[template.id]
        if (edited != null) return edited
        return fieldValues[template.id]?.map { it.value ?: "" } ?: emptyList()
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
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("标题") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }

                item {
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(
                            value = selectedCategory?.name ?: "", onValueChange = {}, readOnly = true, label = { Text("分类") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Box(modifier = Modifier.size(24.dp).background(Color(category.color), CircleShape))
                                        Text(category.name)
                                    }},
                                    onClick = { selectedCategory = category; expanded = false },
                                    leadingIcon = if (selectedCategory?.id == category.id) { { Icon(Icons.Default.Check, contentDescription = null) } } else null
                                )
                            }
                        }
                    }
                }

                if (isCreate && selectedCategory == null) {
                    item { Text("请选择一个分类", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                }

                if (templates.isNotEmpty()) {
                    item { Text("字段", style = MaterialTheme.typography.titleSmall) }
                }

                items(templates) { template ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = template.fieldName, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            when (template.fieldType) {
                                FieldType.TEXT -> {
                                    val v = editingValues[template.id] ?: fieldValues[template.id]?.firstOrNull()?.value ?: ""
                                    OutlinedTextField(value = v, onValueChange = { editingValues = editingValues + (template.id to it) }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                                }
                                FieldType.NUMBER -> {
                                    val v = editingValues[template.id] ?: fieldValues[template.id]?.firstOrNull()?.value ?: ""
                                    OutlinedTextField(value = v, onValueChange = { editingValues = editingValues + (template.id to it) }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                                }
                                FieldType.DATE -> {
                                    val v = editingValues[template.id] ?: fieldValues[template.id]?.firstOrNull()?.value ?: ""
                                    OutlinedTextField(value = v, onValueChange = { editingValues = editingValues + (template.id to it) }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("YYYY-MM-DD") })
                                }
                                FieldType.IMAGE -> {
                                    val images = getImages(template)
                                    if (images.isNotEmpty()) {
                                        val rowState = rememberLazyListState()
                                        LaunchedEffect(images.size) {
                                            if (images.isNotEmpty()) rowState.animateScrollToItem(images.size - 1)
                                        }
                                        LazyRow(
                                            state = rowState,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(images) { img ->
                                                Box(modifier = Modifier.size(120.dp).clickable { viewingImageUrl = img }) {
                                                    Image(
                                                        painter = coil.compose.rememberAsyncImagePainter(img),
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.error,
                                                        shape = CircleShape,
                                                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).clickable {
                                                            editingImages = editingImages + (template.id to (images - img))
                                                        }
                                                    ) {
                                                        Icon(Icons.Default.Close, contentDescription = "删除", tint = MaterialTheme.colorScheme.onError, modifier = Modifier.padding(4.dp))
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            if (hasImagePermission) {
                                                pendingImageTemplateId = template.id
                                                imagePickerLauncher.launch("image/*")
                                            } else {
                                                imagePermissionLauncher.launch(
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES
                                                    else Manifest.permission.READ_EXTERNAL_STORAGE
                                                )
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text(if (images.isEmpty()) "选择图片" else "添加图片") }
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedButton(onClick = { showAddFieldDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("添加字段") }
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
                        // Delete removed template values
                        val existingValues = valueRepo.getFieldValuesByPin(id).first()
                        existingValues.forEach { fv ->
                            if (fv.fieldTemplateId !in templates.map { it.id }) {
                                valueRepo.deleteFieldValueById(fv.id)
                            }
                        }
                        // Save values per template
                        templates.forEach { template ->
                            when (template.fieldType) {
                                FieldType.IMAGE -> {
                                    val newImages = editingImages[template.id]
                                    if (newImages != null) {
                                        // Delete existing images for this template
                                        existingValues.filter { it.fieldTemplateId == template.id }.forEach { valueRepo.deleteFieldValueById(it.id) }
                                        // Insert new ones
                                        newImages.forEach { uri ->
                                            valueRepo.insertFieldValue(FieldValue(pinId = id, fieldTemplateId = template.id, value = uri))
                                        }
                                    }
                                }
                                else -> {
                                    val v = editingValues[template.id] ?: fieldValues[template.id]?.firstOrNull()?.value ?: ""
                                    val existing = existingValues.find { it.fieldTemplateId == template.id }
                                    if (existing != null) {
                                        valueRepo.updateFieldValue(existing.copy(value = v))
                                    } else {
                                        valueRepo.insertFieldValue(FieldValue(pinId = id, fieldTemplateId = template.id, value = v))
                                    }
                                }
                            }
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
                    templateRepo.insertFieldTemplate(FieldTemplate(categoryId = catId, fieldName = name, fieldType = type))
                    templates = templateRepo.getFieldTemplatesByCategory(catId).first()
                }
                showAddFieldDialog = false
            },
            onDismiss = { showAddFieldDialog = false }
        )
    }

    viewingImageUrl?.let { url ->
        Dialog(onDismissRequest = { viewingImageUrl = null }) {
            Box(modifier = Modifier.fillMaxSize().clickable { viewingImageUrl = null }) {
                Image(painter = coil.compose.rememberAsyncImagePainter(url), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(50),
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).size(48.dp).clickable { viewingImageUrl = null }
                ) { Icon(Icons.Default.Close, contentDescription = "关闭", modifier = Modifier.padding(12.dp)) }
            }
        }
    }
}
