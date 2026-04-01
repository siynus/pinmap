package com.sinus.pinmap.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sinus.pinmap.data.database.PinmapDatabase
import com.sinus.pinmap.data.repository.CategoryRepository
import com.sinus.pinmap.ui.viewmodel.CategoryViewModel

/**
 * 分类列表页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListScreen(
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val database = remember { PinmapDatabase.getDatabase(context) }
    val categoryRepository = remember { CategoryRepository(database.categoryDao()) }
    val viewModel: CategoryViewModel = viewModel { CategoryViewModel(categoryRepository) }

    val categories by viewModel.categories.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<com.sinus.pinmap.data.entity.Category?>(null) }

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
            // 空状态
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "还没有分类",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "点击右下角按钮创建第一个分类",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(categories) { category ->
                    CategoryItem(
                        category = category,
                        onDelete = { categoryToDelete = category }
                    )
                }
            }
        }
    }

    // 创建分类对话框
    if (showCreateDialog) {
        CreateCategoryDialog(
            onConfirm = { name, color ->
                viewModel.createCategory(name, color)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    // 删除确认对话框
    categoryToDelete?.let { category ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("删除分类") },
            text = {
                Text("确定要删除分类「${category.name}」吗？\n\n删除后将同时删除该分类下的所有标记，此操作不可恢复。")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCategory(category)
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 分类列表项
 */
@Composable
private fun CategoryItem(
    category: com.sinus.pinmap.data.entity.Category,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                // 颜色指示器
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(category.color), CircleShape)
                )

                // 分类名称
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "ID: ${category.id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 删除按钮
            IconButton(
                onClick = onDelete,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Delete, contentDescription = "删除分类")
            }
        }
    }
}