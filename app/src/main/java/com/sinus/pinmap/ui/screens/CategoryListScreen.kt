package com.sinus.pinmap.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sinus.pinmap.data.database.PinmapDatabase
import com.sinus.pinmap.data.entity.Category
import com.sinus.pinmap.data.repository.CategoryRepository
import com.sinus.pinmap.ui.viewmodel.CategoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListScreen(
    modifier: Modifier = Modifier,
    onNavigateToFieldTemplates: (Long) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val database = remember { PinmapDatabase.getDatabase(context) }
    val categoryRepository = remember { CategoryRepository(database.categoryStore()) }
    val viewModel: CategoryViewModel = viewModel { CategoryViewModel(categoryRepository) }

    val categories by viewModel.categories.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("分类列表") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "创建分类")
            }
        },
        modifier = modifier
    ) { paddingValues ->
        if (categories.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("还没有分类", style = MaterialTheme.typography.titleLarge)
                    Text("点击右下角按钮创建第一个分类", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(categories) { category ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onNavigateToFieldTemplates(category.id) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(modifier = Modifier.size(48.dp).background(Color(category.color), CircleShape))
                                Column {
                                    Text(text = category.name, style = MaterialTheme.typography.titleMedium)
                                    Text(text = "点击管理字段模板", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            IconButton(onClick = { categoryToEdit = category }) {
                                Icon(Icons.Default.Edit, contentDescription = "编辑")
                            }
                            IconButton(onClick = { categoryToDelete = category }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateCategoryDialog(
            onConfirm = { name, color ->
                viewModel.createCategory(name, color)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    categoryToEdit?.let { category ->
        var editName by remember { mutableStateOf(category.name) }
        var editColor by remember { mutableStateOf(presetColors.find { it.hashCode() == category.color } ?: presetColors[0]) }

        Dialog(onDismissRequest = { categoryToEdit = null }) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text("编辑分类", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = editName, onValueChange = { editName = it },
                        label = { Text("分类名称") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("选择颜色", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Column {
                        presetColors.chunked(4).forEach { rowColors ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                rowColors.forEach { color ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f).aspectRatio(1f)
                                            .clickable { editColor = color }
                                            .background(color, CircleShape)
                                            .then(if (color == editColor) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (color == editColor) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { categoryToEdit = null }) { Text("取消") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.updateCategory(category.copy(name = editName, color = editColor.hashCode()))
                                categoryToEdit = null
                            },
                            enabled = editName.isNotBlank()
                        ) { Text("保存") }
                    }
                }
            }
        }
    }

    categoryToDelete?.let { category ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("删除分类") },
            text = { Text("确定要删除分类「${category.name}」吗？\n\n删除后将同时删除该分类下的所有标记，此操作不可恢复。") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCategory(category)
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { categoryToDelete = null }) { Text("取消") } }
        )
    }
}

private val presetColors = listOf(
    Color(0xFFEF5350), Color(0xFFEC407A), Color(0xFFAB47BC), Color(0xFF7E57C2),
    Color(0xFF5C6BC0), Color(0xFF42A5F5), Color(0xFF26C6DA), Color(0xFF26A69A),
    Color(0xFF66BB6A), Color(0xFF9CCC65), Color(0xFFFFA726), Color(0xFFFF7043),
)
