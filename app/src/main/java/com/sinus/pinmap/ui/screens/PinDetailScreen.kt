package com.sinus.pinmap.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sinus.pinmap.data.database.PinmapDatabase
import com.sinus.pinmap.data.repository.FieldTemplateRepository
import com.sinus.pinmap.data.repository.FieldValueRepository
import com.sinus.pinmap.data.repository.PinRepository
import com.sinus.pinmap.data.entity.FieldTemplate
import com.sinus.pinmap.ui.viewmodel.PinDetailViewModel
import com.sinus.pinmap.ui.screens.FieldValueEditor
import com.sinus.pinmap.ui.screens.CreateFieldTemplateDialog
import com.sinus.pinmap.ui.screens.FieldValueEditor

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
    val pinRepository = remember { PinRepository(database.pinDao()) }
    val fieldTemplateRepository = remember { FieldTemplateRepository(database.fieldTemplateDao()) }
    val fieldValueRepository = remember { FieldValueRepository(database.fieldValueDao()) }

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

    var showAddFieldDialog by remember { mutableStateOf(false) }
    var fieldToDelete by remember { mutableStateOf<FieldTemplate?>(null) }

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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
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
                                    .padding(16.dp)
                            ) {
                                FieldValueEditor(
                                    fieldTemplate = fieldTemplate,
                                    value = fieldValues[fieldTemplate.id]?.value,
                                    onValueChange = { viewModel.updateFieldValue(fieldTemplate.id, it) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            IconButton(
                                onClick = {
                                    fieldToDelete = fieldTemplate
                                },
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
                viewModel.createFieldTemplate(fieldName, fieldType, isTemplate)
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
                Text("确定要删除字段「${fieldTemplate.fieldName}」吗？\n\n如果该字段是模板字段，删除后可以选择是否保留标记中的字段值。")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFieldTemplate(fieldTemplate, keepFieldValues = false)
                        fieldToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除所有")
                }
            },
            dismissButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            viewModel.deleteFieldTemplate(fieldTemplate, keepFieldValues = true)
                            fieldToDelete = null
                        }
                    ) {
                        Text("保留字段值")
                    }
                    TextButton(onClick = { fieldToDelete = null }) {
                        Text("取消")
                    }
                }
            }
        )
    }
}