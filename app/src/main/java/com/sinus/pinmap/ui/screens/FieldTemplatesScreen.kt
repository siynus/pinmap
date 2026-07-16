package com.sinus.pinmap.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.sinus.pinmap.data.database.PinmapDatabase
import com.sinus.pinmap.data.entity.FieldTemplate
import com.sinus.pinmap.data.repository.FieldTemplateRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldTemplatesScreen(
    categoryId: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { PinmapDatabase.getDatabase(context) }
    val templateRepo = remember { FieldTemplateRepository(database.fieldTemplateStore()) }
    val scope = rememberCoroutineScope()

    var templates by remember { mutableStateOf<List<FieldTemplate>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var templateToDelete by remember { mutableStateOf<FieldTemplate?>(null) }
    var templateToEdit by remember { mutableStateOf<FieldTemplate?>(null) }
    var draggedId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    fun load() {
        scope.launch { templates = templateRepo.getFieldTemplatesByCategory(categoryId).first() }
    }

    LaunchedEffect(categoryId) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("字段模板") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (templates.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("还没有字段模板", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(templates, key = { _, t -> t.id }) { _, template ->
                        val isDragging = draggedId == template.id
                        Card(
                            modifier = Modifier
                                .zIndex(if (isDragging) 1f else 0f)
                                .graphicsLayer { translationY = if (isDragging) dragOffset else 0f }
                                .pointerInput(template.id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { draggedId = template.id; dragOffset = 0f },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            dragOffset += amount.y
                                            val list = templates
                                            val i = list.indexOfFirst { it.id == template.id }
                                            if (i < 0) return@detectDragGesturesAfterLongPress
                                            val h = size.height.toFloat()
                                            if (dragOffset > h && i < list.size - 1) {
                                                dragOffset -= h
                                                val m = list.toMutableList(); val item = m.removeAt(i); m.add(i + 1, item); templates = m
                                                scope.launch {
                                                    templateRepo.updateFieldTemplate(item.copy(sortOrder = i + 1))
                                                    templateRepo.updateFieldTemplate(list[i + 1].copy(sortOrder = i))
                                                }
                                            } else if (dragOffset < -h && i > 0) {
                                                dragOffset += h
                                                val m = list.toMutableList(); val item = m.removeAt(i); m.add(i - 1, item); templates = m
                                                scope.launch {
                                                    templateRepo.updateFieldTemplate(item.copy(sortOrder = i - 1))
                                                    templateRepo.updateFieldTemplate(list[i - 1].copy(sortOrder = i))
                                                }
                                            }
                                        },
                                        onDragEnd = { draggedId = null; dragOffset = 0f },
                                        onDragCancel = { draggedId = null; dragOffset = 0f }
                                    )
                                }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f).clickable { templateToEdit = template }
                                ) {
                                    Text(text = template.fieldName, style = MaterialTheme.typography.bodyMedium)
                                    Text(text = template.fieldType.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { templateToDelete = template }) {
                                    Icon(Icons.Default.Close, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("添加字段")
            }
        }
    }

    if (showAddDialog) {
        CreateFieldTemplateDialog(
            onConfirm = { name, type ->
                scope.launch {
                    val nextOrder = templateRepo.nextSortOrder(categoryId)
                    templateRepo.insertFieldTemplate(FieldTemplate(categoryId = categoryId, fieldName = name, fieldType = type, sortOrder = nextOrder))
                    load()
                }
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    templateToEdit?.let { template ->
        var editName by remember { mutableStateOf(template.fieldName) }
        Dialog(onDismissRequest = { templateToEdit = null }) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("编辑字段", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("字段名称") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Text("类型: ${template.fieldType.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { templateToEdit = null }) { Text("取消") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            scope.launch { templateRepo.updateFieldTemplate(template.copy(fieldName = editName)); load() }
                            templateToEdit = null
                        }, enabled = editName.isNotBlank()) { Text("保存") }
                    }
                }
            }
        }
    }

    templateToDelete?.let { template ->
        AlertDialog(
            onDismissRequest = { templateToDelete = null },
            title = { Text("删除字段") },
            text = { Text("确定要删除字段「${template.fieldName}」吗？\n\n这将同时删除所有标记中该字段的值。") },
            confirmButton = {
                Button(onClick = {
                    scope.launch { templateRepo.deleteFieldTemplate(template); load() }
                    templateToDelete = null
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { templateToDelete = null }) { Text("取消") } }
        )
    }
}
