package com.sinus.pinmap.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sinus.pinmap.data.database.PinmapDatabase
import com.sinus.pinmap.data.repository.FieldTemplateRepository
import com.sinus.pinmap.data.repository.FieldValueRepository
import com.sinus.pinmap.data.repository.PinRepository
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import coil.compose.rememberAsyncImagePainter
import com.sinus.pinmap.data.entity.FieldTemplate
import com.sinus.pinmap.ui.viewmodel.PinDetailViewModel

/**
 * 标记详情页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinDetailScreen(
    pinId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val database = remember { PinmapDatabase.getDatabase(context) }
    val pinRepository = remember { PinRepository(database.pinStore()) }
    val fieldTemplateRepository = remember { FieldTemplateRepository(database.fieldTemplateStore()) }
    val fieldValueRepository = remember { FieldValueRepository(database.fieldValueStore()) }

    val viewModel: PinDetailViewModel = viewModel(
        key = "pin_detail_$pinId",
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return PinDetailViewModel(
                    savedStateHandle = androidx.lifecycle.SavedStateHandle(mapOf("pinId" to pinId)),
                    pinRepository = pinRepository,
                    fieldTemplateRepository = fieldTemplateRepository,
                    fieldValueRepository = fieldValueRepository
                ) as T
            }
        }
    )

    val pin by viewModel.pin.collectAsState()
    val fieldTemplates by viewModel.fieldTemplates.collectAsState()
    val fieldValues by viewModel.fieldValues.collectAsState()

    val scope = rememberCoroutineScope()

    var showAddFieldDialog by remember { mutableStateOf(false) }
    var fieldToDelete by remember { mutableStateOf<FieldTemplate?>(null) }
    var currentEditingFieldId by remember { mutableStateOf<Long?>(null) }
    var viewingImageUrl by remember { mutableStateOf<String?>(null) }
    
    // 本地缓存字段值，避免每次输入都触发 ViewModel 更新
    var editingFieldValues by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    
    // 防抖保存：延迟 500ms 后自动保存
    LaunchedEffect(editingFieldValues) {
        kotlinx.coroutines.delay(500)
        if (editingFieldValues.isNotEmpty()) {
            editingFieldValues.forEach { (key, value) ->
                viewModel.updateFieldValue(key, value)
            }
            editingFieldValues = emptyMap()
        }
    }

    // 图片权限检查
    var hasImagePermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val imagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasImagePermission = isGranted
    }

    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            currentEditingFieldId?.let { fieldId ->
                scope.launch {
                    try {
                        val inputStream = context.contentResolver.openInputStream(selectedUri)
                        val timeStamp = System.currentTimeMillis()
                        val fileName = "IMG_${timeStamp}.jpg"
                        val imagesDir = File(context.filesDir, "images")
                        if (!imagesDir.exists()) {
                            imagesDir.mkdirs()
                        }
                        val imageFile = File(imagesDir, fileName)
                        
                        inputStream?.use { input ->
                            FileOutputStream(imageFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        
                        val imageUri = Uri.fromFile(imageFile).toString()
                        viewModel.updateFieldValue(fieldId, imageUri)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        currentEditingFieldId = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pin?.title ?: "标记详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddFieldDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加字段")
            }
        },
        modifier = modifier
    ) { paddingValues ->
        val density = LocalDensity.current
        val imePadding = with(density) { WindowInsets.ime.getBottom(density).toDp() }
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 16.dp + imePadding
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 基本信息
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "基本信息",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Divider()

                        Text(
                            text = "标题: ${pin?.title ?: ""}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        pin?.description?.let { description ->
                            Text(
                                text = "描述: $description",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Text(
                            text = "坐标: ${pin?.latitude}, ${pin?.longitude}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // 自定义字段
            if (fieldTemplates.isEmpty()) {
                item {
                    Card {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "还没有自定义字段",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "点击右下角按钮添加字段",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(fieldTemplates) { fieldTemplate ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                            ) {
                                val currentValue = editingFieldValues[fieldTemplate.id] 
                                    ?: fieldValues[fieldTemplate.id]?.value
                                FieldValueEditor(
                                    fieldTemplate = fieldTemplate,
                                    value = currentValue,
                                    onValueChange = { newValue ->
                                        editingFieldValues = editingFieldValues + (fieldTemplate.id to (newValue ?: ""))
                                    },
                                    hasImagePermission = hasImagePermission,
                                    onRequestImagePermission = {
                                        imagePermissionLauncher.launch(
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                Manifest.permission.READ_MEDIA_IMAGES
                                            } else {
                                                Manifest.permission.READ_EXTERNAL_STORAGE
                                            }
                                        )
                                    },
                                    onSelectImage = {
                                        currentEditingFieldId = fieldTemplate.id
                                        imagePickerLauncher.launch("image/*")
                                    },
                                    onImageClick = {
                                        viewingImageUrl = currentValue
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            IconButton(
                                onClick = { fieldToDelete = fieldTemplate },
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "删除字段")
                            }
                        }
                    }
                }
            }
        }
    }

    // 添加字段对话框
    if (showAddFieldDialog) {
        CreateFieldTemplateDialog(
            onConfirm = { fieldName, fieldType, isTemplate ->
                if (isTemplate) {
                    viewModel.createTemplateField(fieldName, fieldType)
                } else {
                    viewModel.createIndependentField(fieldName, fieldType)
                }
                showAddFieldDialog = false
            },
            onDismiss = { showAddFieldDialog = false }
        )
    }

    // 删除字段确认对话框
    fieldToDelete?.let { fieldTemplate ->
        AlertDialog(
            onDismissRequest = { fieldToDelete = null },
            title = { Text("删除字段") },
            text = {
                Text("确定要删除字段「${fieldTemplate.fieldName}」吗？")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFieldValue(fieldTemplate.id)
                        fieldToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { fieldToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }

    // 图片查看对话框
    viewingImageUrl?.let { imageUrl ->
        Dialog(onDismissRequest = { viewingImageUrl = null }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { viewingImageUrl = null }
            ) {
                Image(
                    painter = rememberAsyncImagePainter(imageUrl),
                    contentDescription = "查看图片",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                
                // 关闭按钮
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(48.dp)
                        .clickable { viewingImageUrl = null }
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}