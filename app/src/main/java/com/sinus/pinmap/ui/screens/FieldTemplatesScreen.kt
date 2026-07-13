package com.sinus.pinmap.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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

    LaunchedEffect(categoryId) {
        templates = templateRepo.getFieldTemplatesByCategory(categoryId).first()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("字段模板") },
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
                .padding(16.dp)
        ) {
            if (templates.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "还没有字段模板",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(templates) { template ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = template.fieldName, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = template.fieldType.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { templateToDelete = template }) {
                                    Icon(Icons.Default.Close, contentDescription = "删除")
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
                    templateRepo.insertFieldTemplate(
                        FieldTemplate(categoryId = categoryId, fieldName = name, fieldType = type)
                    )
                    templates = templateRepo.getFieldTemplatesByCategory(categoryId).first()
                }
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    templateToDelete?.let { template ->
        AlertDialog(
            onDismissRequest = { templateToDelete = null },
            title = { Text("删除字段") },
            text = { Text("确定要删除字段「${template.fieldName}」吗？\n\n这将同时删除所有标记中该字段的值。") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            templateRepo.deleteFieldTemplate(template)
                            templates = templateRepo.getFieldTemplatesByCategory(categoryId).first()
                        }
                        templateToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { templateToDelete = null }) { Text("取消") }
            }
        )
    }
}
