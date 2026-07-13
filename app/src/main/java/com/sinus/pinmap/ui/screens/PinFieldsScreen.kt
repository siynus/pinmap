package com.sinus.pinmap.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.sinus.pinmap.data.database.PinmapDatabase
import com.sinus.pinmap.data.entity.FieldTemplate
import com.sinus.pinmap.data.entity.FieldType
import com.sinus.pinmap.data.repository.FieldTemplateRepository
import com.sinus.pinmap.data.repository.FieldValueRepository
import com.sinus.pinmap.data.repository.PinRepository
import com.sinus.pinmap.ui.viewmodel.PinFieldsViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinFieldsScreen(
    pinId: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { PinmapDatabase.getDatabase(context) }
    val pinRepository = remember { PinRepository(database.pinStore()) }
    val templateRepo = remember { FieldTemplateRepository(database.fieldTemplateStore()) }
    val valueRepo = remember { FieldValueRepository(database.fieldValueStore()) }

    val viewModel: PinFieldsViewModel = viewModel(
        key = "pin_fields_$pinId",
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return PinFieldsViewModel(pinId, pinRepository, templateRepo, valueRepo) as T
            }
        }
    )

    val pin by viewModel.pin.collectAsState()
    val templates by viewModel.templates.collectAsState()
    val fieldValues by viewModel.fieldValues.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var fieldToDelete by remember { mutableStateOf<Long?>(null) }
    var currentFieldKey by remember { mutableStateOf<Long?>(null) }
    var viewingImageUrl by remember { mutableStateOf<String?>(null) }

    var editingFieldValues by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }

    LaunchedEffect(editingFieldValues) {
        if (editingFieldValues.isEmpty()) return@LaunchedEffect
        kotlinx.coroutines.delay(500)
        editingFieldValues.forEach { (key, value) ->
            viewModel.updateFieldValue(key, value)
        }
        editingFieldValues = emptyMap()
    }

    var hasImagePermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
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
    ) { isGranted -> hasImagePermission = isGranted }

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
                        val imageUri = Uri.fromFile(imageFile).toString()
                        viewModel.updateFieldValue(key, imageUri)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            currentFieldKey = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pin?.title ?: "字段编辑") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        if (templates.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "还没有字段",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { showAddDialog = true }) {
                        Text("添加字段")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(templates) { template ->
                    val value = editingFieldValues[template.id]
                        ?: fieldValues[template.id]?.value
                    FieldValueCard(
                        template = template,
                        value = value,
                        onValueChange = { newValue ->
                            editingFieldValues = editingFieldValues + (template.id to newValue)
                        },
                        hasImagePermission = hasImagePermission,
                        onRequestPermission = {
                            imagePermissionLauncher.launch(
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    Manifest.permission.READ_MEDIA_IMAGES
                                } else {
                                    Manifest.permission.READ_EXTERNAL_STORAGE
                                }
                            )
                        },
                        onSelectImage = {
                            currentFieldKey = template.id
                            imagePickerLauncher.launch("image/*")
                        },
                        onImageClick = { viewingImageUrl = value },
                        onDelete = { fieldToDelete = template.id }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("添加字段")
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        CreateFieldTemplateDialog(
            onConfirm = { name, type ->
                viewModel.addField(name, type)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    fieldToDelete?.let { key ->
        AlertDialog(
            onDismissRequest = { fieldToDelete = null },
            title = { Text("删除字段") },
            text = { Text("确定要删除这个字段吗？\n\n这将同时从所有标记中移除该字段。") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteField(key)
                        fieldToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { fieldToDelete = null }) { Text("取消") }
            }
        )
    }

    viewingImageUrl?.let { url ->
        Dialog(onDismissRequest = { viewingImageUrl = null }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { viewingImageUrl = null }
            ) {
                Image(
                    painter = rememberAsyncImagePainter(url),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(48.dp)
                        .clickable { viewingImageUrl = null }
                ) {
                    Icon(Icons.Default.Close, contentDescription = "关闭", modifier = Modifier.padding(12.dp))
                }
            }
        }
    }
}

@Composable
private fun FieldValueCard(
    template: FieldTemplate,
    value: String?,
    onValueChange: (String) -> Unit,
    hasImagePermission: Boolean,
    onRequestPermission: () -> Unit,
    onSelectImage: () -> Unit,
    onImageClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = template.fieldName, style = MaterialTheme.typography.bodyMedium)
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (template.fieldType) {
                FieldType.TEXT -> {
                    OutlinedTextField(
                        value = value ?: "",
                        onValueChange = onValueChange,
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
                FieldType.NUMBER -> {
                    OutlinedTextField(
                        value = value ?: "",
                        onValueChange = onValueChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
                FieldType.DATE -> {
                    OutlinedTextField(
                        value = value ?: "",
                        onValueChange = onValueChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("YYYY-MM-DD") }
                    )
                }
                FieldType.IMAGE -> {
                    if (value != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onImageClick)
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(value),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 200.dp, max = 400.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                if (hasImagePermission) onSelectImage() else onRequestPermission()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("选择图片") }
                    }
                }
            }
        }
    }
}
