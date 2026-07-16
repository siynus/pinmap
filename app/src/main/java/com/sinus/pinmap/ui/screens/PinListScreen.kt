package com.sinus.pinmap.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sinus.pinmap.data.database.PinmapDatabase
import com.sinus.pinmap.data.entity.Pin
import com.sinus.pinmap.data.repository.CategoryRepository
import com.sinus.pinmap.data.repository.PinRepository
import com.sinus.pinmap.ui.utils.LocationManager
import com.sinus.pinmap.ui.utils.haversineDistance
import com.sinus.pinmap.ui.viewmodel.PinListViewModel
import com.sinus.pinmap.ui.viewmodel.SortMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinListScreen(
    onPinClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val database = remember { PinmapDatabase.getDatabase(context) }
    val pinRepository = remember { PinRepository(database.pinStore()) }
    val categoryRepository = remember { CategoryRepository(database.categoryStore()) }
    val viewModel: PinListViewModel = viewModel { PinListViewModel(pinRepository, categoryRepository) }

    val pins by viewModel.filteredPins.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()

    var showDeleteDialog by remember { mutableStateOf<Pin?>(null) }

    LaunchedEffect(Unit) {
        val locationManager = LocationManager(context)
        if (locationManager.hasLocationPermission()) {
            val (loc, _) = locationManager.getLastLocation()
            viewModel.setCurrentLocation(loc.latitude, loc.longitude)
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("标记列表") }
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("搜索标记...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = if (searchQuery.isNotBlank()) {
                        {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "清除搜索")
                            }
                        }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true
                )

                // 分类筛选 + 排序
                if (categories.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedCategoryId == null,
                                onClick = { viewModel.setSelectedCategory(null) },
                                label = { Text("全部") },
                                modifier = Modifier.height(36.dp)
                            )

                            categories.forEach { category ->
                                FilterChip(
                                    selected = selectedCategoryId == category.id,
                                    onClick = { viewModel.setSelectedCategory(category.id) },
                                    label = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .background(Color(category.color), CircleShape)
                                            )
                                            Text(category.name)
                                        }
                                    },
                                    modifier = Modifier.height(36.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Box {
                            var sortExpanded by remember { mutableStateOf(false) }
                            val sortLabel = when (sortMode) {
                                SortMode.CREATED_DESC -> "最新"
                                SortMode.NAME_ASC -> "名称↑"
                                SortMode.NAME_DESC -> "名称↓"
                                SortMode.DISTANCE_ASC -> "最近"
                            }
                            TextButton(
                                onClick = { sortExpanded = true },
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text(sortLabel, style = MaterialTheme.typography.labelMedium)
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = sortExpanded,
                                onDismissRequest = { sortExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("最近") },
                                    onClick = { viewModel.setSortMode(SortMode.DISTANCE_ASC); sortExpanded = false },
                                    leadingIcon = if (sortMode == SortMode.DISTANCE_ASC) {{ Icon(Icons.Default.Check, contentDescription = null) }} else null
                                )
                                DropdownMenuItem(
                                    text = { Text("名称↑") },
                                    onClick = { viewModel.setSortMode(SortMode.NAME_ASC); sortExpanded = false },
                                    leadingIcon = if (sortMode == SortMode.NAME_ASC) {{ Icon(Icons.Default.Check, contentDescription = null) }} else null
                                )
                                DropdownMenuItem(
                                    text = { Text("名称↓") },
                                    onClick = { viewModel.setSortMode(SortMode.NAME_DESC); sortExpanded = false },
                                    leadingIcon = if (sortMode == SortMode.NAME_DESC) {{ Icon(Icons.Default.Check, contentDescription = null) }} else null
                                )
                                DropdownMenuItem(
                                    text = { Text("最新") },
                                    onClick = { viewModel.setSortMode(SortMode.CREATED_DESC); sortExpanded = false },
                                    leadingIcon = if (sortMode == SortMode.CREATED_DESC) {{ Icon(Icons.Default.Check, contentDescription = null) }} else null
                                )
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        if (pins.isEmpty()) {
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
                        text = if (searchQuery.isNotBlank() || selectedCategoryId != null) {
                            "没有找到标记"
                        } else {
                            "还没有标记"
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = if (searchQuery.isNotBlank() || selectedCategoryId != null) {
                            "尝试更改搜索或筛选条件"
                        } else {
                            "在地图上长按创建第一个标记"
                        },
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
                items(pins) { pin ->
                    PinItem(
                        pin = pin,
                        category = categories.find { it.id == pin.categoryId },
                        distanceKm = haversineDistance(
                            viewModel.currentLat, viewModel.currentLng,
                            pin.latitude, pin.longitude
                        ) / 1000f,
                        onClick = { onPinClick(pin.id) },
                        onDelete = { showDeleteDialog = pin }
                    )
                }
            }
        }
    }

    showDeleteDialog?.let { pin ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("删除标记") },
            text = {
                Text("确定要删除标记「${pin.title}」吗？\n\n此操作不可恢复。")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePin(pin)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun PinItem(
    pin: Pin,
    category: com.sinus.pinmap.data.entity.Category?,
    distanceKm: Float,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (category != null) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(category.color), CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            )
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = pin.title,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (distanceKm < 1f) "<1km" else "%.1fkm".format(distanceKm),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4CAF50)
                        )
                    }
                    pin.description?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    pin.address?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Delete, contentDescription = "删除标记")
            }
        }
    }
}
