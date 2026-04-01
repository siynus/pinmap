package com.sinus.pinmap.ui.screens

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.saveable.rememberSaveable
import coil.compose.rememberAsyncImagePainter
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.sinus.pinmap.data.database.PinmapDatabase
import com.sinus.pinmap.data.entity.Category
import com.sinus.pinmap.data.repository.PinRepository
import com.sinus.pinmap.ui.utils.LocationManager
import com.sinus.pinmap.ui.viewmodel.MapViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.OutlinedTextFieldDefaults

/**
 * 地图页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("ClickableViewAccessibility")
@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    onNavigateToEdit: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val database = remember { PinmapDatabase.getDatabase(context) }
    val pinRepository = remember { PinRepository(database.pinDao()) }
    val categoryRepository = remember { com.sinus.pinmap.data.repository.CategoryRepository(database.categoryDao()) }
    val fieldTemplateRepository = remember { com.sinus.pinmap.data.repository.FieldTemplateRepository(database.fieldTemplateDao()) }
    val fieldValueRepository = remember { com.sinus.pinmap.data.repository.FieldValueRepository(database.fieldValueDao()) }
    val viewModel: MapViewModel = viewModel { MapViewModel(pinRepository, fieldTemplateRepository, fieldValueRepository) }

    val pins by viewModel.pins.collectAsState()
    val selectedPin by viewModel.selectedPin.collectAsState()
    val categories by categoryRepository.getAllCategories().collectAsState(initial = emptyList())
    
    // 获取每个类别的模板字段
    var categoryTemplates by remember { mutableStateOf<Map<Long, List<com.sinus.pinmap.data.entity.FieldTemplate>>>(emptyMap()) }
    
    LaunchedEffect(categories) {
        val templatesMap = mutableMapOf<Long, List<com.sinus.pinmap.data.entity.FieldTemplate>>()
        categories.forEach { category ->
            fieldTemplateRepository.getTemplateFieldsByCategory(category.id).collect { templates ->
                templatesMap[category.id] = templates
                categoryTemplates = templatesMap.toMap()
            }
        }
    }

    var showCreateDialog by remember { mutableStateOf(false) }
    var showCreateCategoryDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
    var selectedAddress by remember { mutableStateOf("获取中...") }
    var searchQuery by remember { mutableStateOf("") }
    var showSearchResults by remember { mutableStateOf(false) }
    
    // 键盘控制器和焦点请求器
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchFocusRequester = remember { FocusRequester() }

    // 搜索结果
    val searchResults by remember(searchQuery) {
        derivedStateOf {
            if (searchQuery.isBlank()) {
                emptyList()
            } else {
                pins.filter {
                    it.title.contains(searchQuery, ignoreCase = true) ||
                    (it.description?.contains(searchQuery, ignoreCase = true) == true)
                }
            }
        }
    }

    // 位置管理器
    val locationManager = remember { LocationManager(context) }

    // 记住 MapView 实例
    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
        }
    }

    // 地图是否已初始化
    var isMapInitialized by remember { mutableStateOf(false) }

var myLocationMarker by remember { mutableStateOf<Marker?>(null) }

    // 位置权限请求
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    // 图片权限请求（Android 13+）
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

    // 在地图初始化时请求权限
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // 初始化地图位置
    LaunchedEffect(mapView) {
        if (!isMapInitialized) {
            val aMap = mapView.map

            // 基本配置
            aMap.mapType = com.amap.api.maps.AMap.MAP_TYPE_NORMAL

            // 启用手势
            val uiSettings = aMap.uiSettings
            uiSettings.isZoomControlsEnabled = true
            uiSettings.isZoomGesturesEnabled = true
            uiSettings.isScrollGesturesEnabled = true
            uiSettings.isCompassEnabled = true

            // 尝试获取当前位置
            val locationResult = locationManager.getCurrentLocation()

            val (targetLocation, zoom) = if (locationResult.isSuccess) {
                // 定位成功，使用当前位置
                locationResult.getOrNull()!! to 15f
            } else {
                // 定位失败，使用上次保存的位置
                locationManager.getLastLocation()
            }

            // 移动地图到目标位置
            aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(targetLocation, zoom))
            isMapInitialized = true
        }
    }

    // 管理生命周期
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    try {
                        mapView.onResume()
                    } catch (e: Exception) {
                        // 忽略异常
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    try {
                        mapView.onPause()
                    } catch (e: Exception) {
                        // 忽略异常
                    }
                }
                Lifecycle.Event.ON_DESTROY -> {
                    try {
                        mapView.onDestroy()
                    } catch (e: Exception) {
                        // 忽略异常
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapView }
        ) { mapView ->
            val aMap = mapView.map

            // 设置地图点击事件（用于取消选择）
            aMap.setOnMapClickListener { _ ->
                // 点击地图空白处取消选择
                viewModel.clearSelectedPin()
            }

            // 设置地图长按事件（用于创建标记）
            aMap.setOnMapLongClickListener { latLng ->
                selectedLocation = latLng
                selectedAddress = "获取中..."
                showCreateDialog = true

                // 获取地址
                scope.launch {
                    val addressResult = locationManager.getAddress(latLng.latitude, latLng.longitude)
                    selectedAddress = if (addressResult.isSuccess) {
                        addressResult.getOrNull() ?: "未知地址"
                    } else {
                        "地址解析失败"
                    }
                }
            }

            // 设置标记点击事件
            aMap.setOnMarkerClickListener { marker ->
                val pin = pins.find {
                    it.latitude == marker.position.latitude &&
                            it.longitude == marker.position.longitude
                }
                if (pin != null) {
                    viewModel.selectPin(pin)
                    showEditDialog = true
                }
                true
            }

            // 监听地图移动，保存位置
            aMap.setOnCameraChangeListener(object : com.amap.api.maps.AMap.OnCameraChangeListener {
                override fun onCameraChange(cameraPosition: com.amap.api.maps.model.CameraPosition?) {
                    // 实时移动中
                }

                override fun onCameraChangeFinish(cameraPosition: com.amap.api.maps.model.CameraPosition?) {
                    cameraPosition?.let {
                        scope.launch {
                            locationManager.saveLastLocation(
                                it.target.latitude,
                                it.target.longitude,
                                it.zoom
                            )
                        }
                    }
                }
            })
        }

        // 定位按钮 - 放在搜索框右上方
        FloatingActionButton(
            onClick = {
                if (!hasLocationPermission) {
                    // 没有权限，请求权限
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                } else {
                    // 有权限，执行定位
                    scope.launch {
                        val locationResult = locationManager.getCurrentLocation()
                        if (locationResult.isSuccess) {
                            val location = locationResult.getOrNull()
                            if (location != null) {
                                val aMap = mapView.map
                                
                                // 移除旧的当前位置标记
                                myLocationMarker?.remove()
                                
                                // 添加新的当前位置标记
                                val markerOptions = MarkerOptions()
                                    .position(location)
                                    .title("当前位置")
                                    .draggable(false)
                                    .snippet("你在当前位置")
                                
                                val marker = aMap.addMarker(markerOptions)
                                myLocationMarker = marker
                                
                                // 移动地图到当前位置
                                aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
                            }
                        }
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 80.dp)
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = "定位到当前位置")
        }

        // 搜索栏 - 放在底部
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .imePadding()
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { newValue: String ->
                        searchQuery = newValue
                        showSearchResults = newValue.isNotBlank()
                    },
                    placeholder = { Text("搜索标记...") },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(searchFocusRequester),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onSearch = {
                            if (searchResults.isNotEmpty()) {
                                // 点击搜索按钮时，跳转到第一个搜索结果
                                scope.launch {
                                    val pin = searchResults.first()
                                    val aMap = mapView.map
                                    val latLng = LatLng(pin.latitude, pin.longitude)
                                    aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
                                    showSearchResults = false
                                    searchQuery = ""
                                    keyboardController?.hide()
                                }
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                )

                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = {
                        searchQuery = ""
                        showSearchResults = false
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "清除搜索")
                    }
                }
            }
        }

        // 搜索结果列表 - 显示在搜索框上方
        if (showSearchResults && searchResults.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 100.dp)
                    .imePadding()
                    .align(Alignment.BottomCenter),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    items(searchResults) { pin ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .clickable {
                                    // 跳转到标记位置
                                    scope.launch {
                                        val aMap = mapView.map
                                        val latLng = LatLng(pin.latitude, pin.longitude)
                                        aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
                                        showSearchResults = false
                                        searchQuery = ""
                                        keyboardController?.hide()
                                    }
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = pin.title,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                pin.description?.let { description ->
                                    Text(
                                        text = description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 监听 pins 变化，更新地图标记
    LaunchedEffect(pins) {
        val aMap = mapView.map

        // 清除所有标记
        aMap.clear()

        // 添加新标记
        pins.forEach { pin ->
            val markerOptions = MarkerOptions()
                .position(LatLng(pin.latitude, pin.longitude))
                .title(pin.title)
                .snippet(pin.description ?: "")
                .draggable(false)
                .anchor(0.5f, 0.5f)

            // 添加标记到地图
            aMap.addMarker(markerOptions)
        }
    }

    // 监听选中的标记
    LaunchedEffect(selectedPin) {
        if (selectedPin != null) {
            // TODO: 显示 PinDetailDialog
        }
    }

    // 显示创建标记对话框
    if (showCreateDialog && selectedLocation != null) {
        CreatePinDialog(
            location = selectedLocation!!,
            address = selectedAddress,
            categories = categories,
            categoryTemplates = categoryTemplates,
            onConfirm = { title, categoryId, fields ->
                viewModel.createPin(
                    latitude = selectedLocation!!.latitude,
                    longitude = selectedLocation!!.longitude,
                    title = title,
                    categoryId = categoryId,
                    fields = fields
                )
                showCreateDialog = false
                selectedLocation = null
            },
            onCreateCategory = {
                showCreateCategoryDialog = true
            },
            onDismiss = {
                showCreateDialog = false
                selectedLocation = null
            }
        )
    }

    // 显示创建分类对话框
    if (showCreateCategoryDialog) {
        CreateCategoryDialog(
            onConfirm = { name, color ->
                scope.launch {
                    categoryRepository.insertCategory(
                        com.sinus.pinmap.data.entity.Category(
                            name = name,
                            color = color
                        )
                    )
                }
                showCreateCategoryDialog = false
            },
            onDismiss = {
                showCreateCategoryDialog = false
            }
        )
    }

    // 显示编辑标记对话框
    if (showEditDialog && selectedPin != null) {
        var editTitle by remember { mutableStateOf(selectedPin!!.title) }
        var editCategory by remember { mutableStateOf<Category?>(null) }
        var expanded by remember { mutableStateOf(false) }
        var showAddFieldDialog by remember { mutableStateOf(false) }
        var tempFields by remember { mutableStateOf<List<com.sinus.pinmap.ui.model.FieldData>>(emptyList()) }
        var nextFieldId by remember { mutableStateOf(0L) }
        var currentEditingFieldId by remember { mutableStateOf<Long?>(null) }
        var viewingImageUrl by remember { mutableStateOf<String?>(null) }
        
        // 图片选择器
        val imagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let { selectedUri ->
                currentEditingFieldId?.let { fieldId ->
                    scope.launch {
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
                        // 更新当前编辑的字段值
                        tempFields = tempFields.map { if (it.id == fieldId) it.copy(value = imageUri) else it }
                    }
                }
            }
            currentEditingFieldId = null
        }

        // 初始化编辑分类和字段
        LaunchedEffect(selectedPin) {
            selectedPin?.let { pin ->
                editCategory = categories.find { it.id == pin.categoryId }
                
                // 加载字段模板和字段值
                scope.launch {
                    // 并行加载模板字段和字段值
                    val templatesDeferred = async<List<com.sinus.pinmap.data.entity.FieldTemplate>> {
                        fieldTemplateRepository.getTemplateFieldsByCategory(pin.categoryId ?: 0L).first()
                    }
                    val valuesDeferred = async<List<com.sinus.pinmap.data.entity.FieldValue>> {
                        fieldValueRepository.getFieldValuesByPin(pin.id).first()
                    }
                    
                    val templates = templatesDeferred.await()
                    val values = valuesDeferred.await()
                    val valueMap = values.associateBy { it.fieldTemplateId }
                    
                    // 加载自定义字段（通过 fieldTemplateId 查询）
                    val customTemplateIds = values.map { it.fieldTemplateId }.toSet()
                    val customTemplates = customTemplateIds.mapNotNull { id ->
                        scope.async {
                            fieldTemplateRepository.getFieldTemplateById(id)
                        }.await()
                    }.filterNotNull().filter { it.categoryId == null } // 只保留自定义字段
                    
                    // 合并所有字段模板
                    val allTemplates = templates + customTemplates
                    
                    tempFields = allTemplates.map { template ->
                        com.sinus.pinmap.ui.model.FieldData(
                            id = nextFieldId++,
                            name = template.fieldName,
                            type = template.fieldType,
                            value = valueMap[template.id]?.value ?: "",
                            isTemplate = template.isTemplate
                        )
                    }
                }
            }
        }

        // 初始化编辑分类
        LaunchedEffect(selectedPin) {
            selectedPin?.let { pin ->
                editCategory = categories.find { it.id == pin.categoryId }
            }
        }

        Dialog(onDismissRequest = { showEditDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "编辑标记",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 类别选择器
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = editCategory?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("选择分类") },
                            trailingIcon = {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .background(Color(category.color), CircleShape)
                                            )
                                            Text(category.name)
                                        }
                                    },
                                    onClick = {
                                        editCategory = category
                                        expanded = false
                                    },
                                    leadingIcon = if (editCategory?.id == category.id) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null
                                            )
                                        }
                                    } else null
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("标题") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 自定义字段
                    if (tempFields.isEmpty()) {
                        Text(
                            text = "还没有添加字段",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(tempFields) { field ->
                                Card(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = field.name,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                if (field.isTemplate) {
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.primaryContainer,
                                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = "模板",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            // 只有自定义字段才显示删除按钮
                                            if (!field.isTemplate) {
                                                TextButton(
                                                    onClick = {
                                                        tempFields = tempFields.filter { it.id != field.id }
                                                    }
                                                ) {
                                                    Text("删除", style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        if (field.type == com.sinus.pinmap.data.entity.FieldType.IMAGE) {
                                            // 图片类型显示
                                            if (field.value.isNotBlank()) {
                                                // 显示图片预览
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(8.dp)
                                                        .clickable {
                                                            viewingImageUrl = field.value
                                                        }
                                                ) {
                                                    Image(
                                                        painter = rememberAsyncImagePainter(field.value),
                                                        contentDescription = field.name,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .heightIn(min = 200.dp, max = 400.dp),
                                                        contentScale = ContentScale.Fit
                                                    )
                                                    
                                                    // 删除按钮
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.error,
                                                        shape = RoundedCornerShape(50),
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .padding(8.dp)
                                                            .size(32.dp)
                                                            .clickable { 
                                                                if (field.isTemplate) {
                                                                    // 模板字段：只清除图片值
                                                                    tempFields = tempFields.map {
                                                                        if (it.id == field.id) it.copy(value = "") else it
                                                                    }
                                                                } else {
                                                                    // 自定义字段：删除整个字段
                                                                    tempFields = tempFields.filter { it.id != field.id }
                                                                }
                                                            }
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Close,
                                                            contentDescription = "删除图片",
                                                            tint = MaterialTheme.colorScheme.onError,
                                                            modifier = Modifier.padding(6.dp)
                                                        )
                                                    }
                                                }
                                            } else {
                                                // 显示选择图片按钮
                                                OutlinedButton(
                                                    onClick = {
                                                        currentEditingFieldId = field.id
                                                        imagePickerLauncher.launch("image/*")
                                                    },
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text("选择图片")
                                                }
                                            }
                                        } else {
                                            // 文本和数字类型
                                            OutlinedTextField(
                                                value = field.value,
                                                onValueChange = { newValue ->
                                                    tempFields = tempFields.map {
                                                        if (it.id == field.id) it.copy(value = newValue) else it
                                                    }
                                                },
                                                placeholder = {
                                                    Text(
                                                        when (field.type) {
                                                            com.sinus.pinmap.data.entity.FieldType.TEXT -> "输入文本"
                                                            com.sinus.pinmap.data.entity.FieldType.NUMBER -> "输入数字"
                                                            com.sinus.pinmap.data.entity.FieldType.IMAGE -> "图片路径"
                                                            else -> "输入值"
                                                        }
                                                    )
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 添加字段按钮
                    OutlinedButton(
                        onClick = { showAddFieldDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("添加字段")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showEditDialog = false }) {
                            Text("取消")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                val category = editCategory
                                if (category != null) {
                                    viewModel.updatePin(
                                        selectedPin!!.copy(
                                            title = editTitle,
                                            categoryId = category.id
                                        )
                                    )
                                    
                                    // 保存字段值
                                    scope.launch {
                                        // 删除旧字段值
                                        fieldValueRepository.deleteFieldValuesByPin(selectedPin!!.id)
                                        
                                        // 保存新字段值
                                        tempFields.forEach { fieldData ->
                                            // 创建字段模板
                                            val fieldTemplate = com.sinus.pinmap.data.entity.FieldTemplate(
                                                categoryId = if (fieldData.isTemplate) category.id else null,
                                                fieldName = fieldData.name,
                                                fieldType = fieldData.type,
                                                isTemplate = fieldData.isTemplate
                                            )
                                            val fieldTemplateId = fieldTemplateRepository.insertFieldTemplate(fieldTemplate)
                                            
                                            // 创建字段值
                                            val fieldValue = com.sinus.pinmap.data.entity.FieldValue(
                                                pinId = selectedPin!!.id,
                                                fieldTemplateId = fieldTemplateId,
                                                value = fieldData.value.ifBlank { null }
                                            )
                                            fieldValueRepository.insertFieldValue(fieldValue)
                                        }
                                    }
                                    
                                    showEditDialog = false
                                }
                            },
                            enabled = editTitle.isNotBlank() && editCategory != null
                        ) {
                            Text("保存")
                        }
                    }
                }
            }
        }
        
        // 图片查看对话框
        if (viewingImageUrl != null) {
            Dialog(onDismissRequest = { viewingImageUrl = null }) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { viewingImageUrl = null }
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(viewingImageUrl),
                        contentDescription = null,
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

        // 创建字段对话框
        if (showAddFieldDialog) {
            var fieldName by remember { mutableStateOf("") }
            var fieldType by remember { mutableStateOf(com.sinus.pinmap.data.entity.FieldType.TEXT) }
            var isTemplate by remember { mutableStateOf(false) }

            Dialog(onDismissRequest = { showAddFieldDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "添加字段",
                            style = MaterialTheme.typography.headlineSmall
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = fieldName,
                            onValueChange = { fieldName = it },
                            label = { Text("字段名称") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "字段类型",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = fieldType == com.sinus.pinmap.data.entity.FieldType.TEXT,
                                onClick = { fieldType = com.sinus.pinmap.data.entity.FieldType.TEXT },
                                label = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Text("文本")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            FilterChip(
                                selected = fieldType == com.sinus.pinmap.data.entity.FieldType.IMAGE,
                                onClick = { fieldType = com.sinus.pinmap.data.entity.FieldType.IMAGE },
                                label = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Text("图片")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            FilterChip(
                                selected = fieldType == com.sinus.pinmap.data.entity.FieldType.NUMBER,
                                onClick = { fieldType = com.sinus.pinmap.data.entity.FieldType.NUMBER },
                                label = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Text("数字")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 是否添加到通用模板
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isTemplate,
                                onCheckedChange = { isTemplate = it }
                            )
                            Text(
                                text = "添加到当前类别的通用模板",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showAddFieldDialog = false }) {
                                Text("取消")
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    tempFields = tempFields + com.sinus.pinmap.ui.model.FieldData(
                                        id = nextFieldId++,
                                        name = fieldName,
                                        type = fieldType,
                                        value = "",
                                        isTemplate = isTemplate
                                    )
                                    showAddFieldDialog = false
                                },
                                enabled = fieldName.isNotBlank()
                            ) {
                                Text("添加")
                            }
                        }
                    }
                }
            }
        }
    }
}