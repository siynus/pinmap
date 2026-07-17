package com.sinus.pinmap.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amap.api.maps.CameraUpdateFactory
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.Log
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.services.poisearch.PoiSearchV2
import com.sinus.pinmap.data.database.PinmapDatabase
import com.sinus.pinmap.data.repository.PinRepository
import com.sinus.pinmap.ui.utils.LocationManager
import com.sinus.pinmap.ui.viewmodel.MapHolderViewModel
import com.sinus.pinmap.ui.viewmodel.MapViewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.core.net.toUri
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.graphics.withClip
import com.amap.api.services.core.PoiItemV2
import com.amap.api.services.poisearch.PoiResultV2
import com.sinus.pinmap.data.repository.CategoryRepository
import com.sinus.pinmap.data.repository.FieldTemplateRepository
import com.sinus.pinmap.data.repository.FieldValueRepository
import com.sinus.pinmap.ui.utils.AuthState

/**
 * 地图页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("ClickableViewAccessibility", "LocalContextResourcesRead")
@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    onNavigateToEdit: (Long) -> Unit = {},
    onNavigateToCreate: (Double, Double) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val database = remember { PinmapDatabase.getDatabase(context) }
    val pinRepository = remember { PinRepository(database.pinStore()) }
    val categoryRepository =
        remember { CategoryRepository(database.categoryStore()) }
    val fieldTemplateRepository =
        remember { FieldTemplateRepository(database.fieldTemplateStore()) }
    val fieldValueRepository =
        remember { FieldValueRepository(database.fieldValueStore()) }
    val viewModel: MapViewModel =
        viewModel { MapViewModel(pinRepository, fieldTemplateRepository, fieldValueRepository) }
    val mapHolder: MapHolderViewModel = viewModel()

    val pins by viewModel.pins.collectAsState()
    val categories by categoryRepository.getAllCategories().collectAsState(initial = emptyList())
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    val filteredPins by remember(pins, selectedCategoryId) {
        derivedStateOf {
            if (selectedCategoryId == null) pins
            else pins.filter { it.categoryId == selectedCategoryId }
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var showSearchResults by remember { mutableStateOf(false) }

    // 键盘控制器和焦点请求器
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchFocusRequester = remember { FocusRequester() }

    // 搜索结果
    var poiResults by remember { mutableStateOf<List<PoiItemV2>>(emptyList()) }

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

    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            try {
                val query = PoiSearchV2.Query(searchQuery, "", null)
                query.pageSize = 10
                query.pageNum = 0
                val search = PoiSearchV2(context, query)
                search.setOnPoiSearchListener(object : PoiSearchV2.OnPoiSearchListener {
                    override fun onPoiSearched(result: PoiResultV2?, code: Int) {
                        if (code == 1000) poiResults = result?.pois ?: emptyList()
                    }

                    override fun onPoiItemSearched(item: PoiItemV2?, code: Int) {}
                })
                search.searchPOIAsyn()
            } catch (e: Exception) {
                Log.e("MapScreen", "POI search error", e)
            }
        } else {
            poiResults = emptyList()
        }
    }

    // 位置管理器
    val locationManager = remember { LocationManager(context) }

    // 记住 MapView 实例
    val mapView = mapHolder.init(context)

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
        if (!mapHolder.mIsInitialized) {
            val aMap = mapView.map ?: return@LaunchedEffect
            mapHolder.setAMap(aMap)

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
            mapHolder.markInitialized()
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
                        Log.e("MapScreen", "MapScreen: $e")
                    }
                }

                Lifecycle.Event.ON_PAUSE -> {
                    try {
                        mapView.onPause()
                    } catch (e: Exception) {
                        Log.e("MapScreen", "MapScreen: $e")
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
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    var searchBottom by remember { mutableStateOf(16.dp) }
    LaunchedEffect(imeInsets) {
        snapshotFlow { imeInsets.getBottom(density) }
            .collect { imePx ->
                searchBottom = if (imePx > 0) {
                    (with(density) { imePx.toDp() } - 80.dp).coerceAtLeast(0.dp)
                } else {
                    16.dp
                }
            }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapView }
        )

        // 设置地图事件监听
        LaunchedEffect(mapHolder.aMap) {
            val aMap = mapHolder.aMap ?: return@LaunchedEffect

            // 设置地图点击事件（用于取消选择）
            aMap.setOnMapClickListener { _ ->
                viewModel.clearSelectedPin()
            }

            // 设置地图长按事件（用于创建标记）
            aMap.setOnMapLongClickListener { latLng ->
                if (AuthState.isAmapKeyValid(context)) {
                    onNavigateToCreate(latLng.latitude, latLng.longitude)
                } else {
                    Toast.makeText(context, "API Key 验证失败，无法创建标记", Toast.LENGTH_SHORT).show()
                }
            }

            // 设置标记点击事件
            aMap.setOnMarkerClickListener { marker ->
                marker.hideInfoWindow()
                val pinId = marker.snippet?.toLongOrNull() ?: return@setOnMarkerClickListener false
                onNavigateToEdit(pinId)
                true
            }

            // 设置标记拖拽事件
            aMap.setOnMarkerDragListener(object : com.amap.api.maps.AMap.OnMarkerDragListener {
                override fun onMarkerDragStart(marker: com.amap.api.maps.model.Marker?) {}
                override fun onMarkerDrag(marker: com.amap.api.maps.model.Marker?) {}
                override fun onMarkerDragEnd(marker: com.amap.api.maps.model.Marker?) {
                    val id = marker?.snippet?.toLongOrNull() ?: return
                    val pos = marker.position
                    scope.launch {
                        pinRepository.getPinById(id)?.let { pin ->
                            pinRepository.updatePin(
                                pin.copy(
                                    latitude = pos.latitude,
                                    longitude = pos.longitude
                                )
                            )
                        }
                    }
                }
            })

            // 监听地图移动，保存位置
            aMap.setOnCameraChangeListener(object : com.amap.api.maps.AMap.OnCameraChangeListener {
                override fun onCameraChange(cameraPosition: com.amap.api.maps.model.CameraPosition?) {}
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

        // 底部面板：搜索栏 + 搜索结果
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = searchBottom)
        ) {
            // 搜索结果列表
            if (showSearchResults && (searchResults.isNotEmpty() || poiResults.isNotEmpty())) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                    ) {
                        if (searchResults.isNotEmpty()) {
                            item {
                                Text(
                                    "标记",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 4.dp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            items(searchResults) { pin ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                        .clickable {
                                            scope.launch {
                                                val aMap = mapHolder.aMap ?: return@launch
                                                aMap.animateCamera(
                                                    CameraUpdateFactory.newLatLngZoom(
                                                        LatLng(
                                                            pin.latitude,
                                                            pin.longitude
                                                        ), 16f
                                                    )
                                                )
                                                showSearchResults = false; searchQuery =
                                                ""; keyboardController?.hide()
                                            }
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = pin.title,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        pin.description?.let {
                                            Text(
                                                text = it,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (poiResults.isNotEmpty()) {
                            item {
                                Text(
                                    "地址",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 4.dp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            items(poiResults) { poi ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                        .clickable {
                                            val aMap = mapHolder.aMap ?: return@clickable
                                            aMap.animateCamera(
                                                CameraUpdateFactory.newLatLngZoom(
                                                    LatLng(
                                                        poi.latLonPoint.latitude,
                                                        poi.latLonPoint.longitude
                                                    ), 16f
                                                )
                                            )
                                            showSearchResults = false; searchQuery =
                                            ""; keyboardController?.hide()
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = poi.title,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = poi.snippet,
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
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 搜索栏
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
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
                                    scope.launch {
                                        val pin = searchResults.first()
                                        val aMap = mapHolder.aMap ?: return@launch
                                        val latLng = LatLng(pin.latitude, pin.longitude)
                                        aMap.animateCamera(
                                            CameraUpdateFactory.newLatLngZoom(
                                                latLng,
                                                16f
                                            )
                                        )
                                        showSearchResults = false
                                        searchQuery = ""
                                        keyboardController?.hide()
                                    }
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
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
        }

        // 分类筛选按钮 - 左下角
        var showCategoryFilter by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = searchBottom + 64.dp)
        ) {
            SmallFloatingActionButton(
                onClick = { showCategoryFilter = true },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Icon(Icons.Default.Menu, contentDescription = "筛选分类")
            }
            DropdownMenu(
                expanded = showCategoryFilter,
                onDismissRequest = { showCategoryFilter = false }
            ) {
                DropdownMenuItem(
                    text = { Text("全部") },
                    onClick = { selectedCategoryId = null; showCategoryFilter = false },
                    leadingIcon = if (selectedCategoryId == null) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null
                )
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.name) },
                        onClick = { selectedCategoryId = category.id; showCategoryFilter = false },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(Color(category.color), CircleShape)
                            )
                        },
                        trailingIcon = if (selectedCategoryId == category.id) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null
                    )
                }
            }
        }

        // 定位按钮 - 放在搜索框右侧
        SmallFloatingActionButton(
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
                                val aMap = mapHolder.aMap ?: return@launch

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
                .padding(end = 16.dp, bottom = searchBottom + 64.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = "定位到当前位置")
        }

        // 监听 pins 变化，更新地图标记
        LaunchedEffect(filteredPins, categories) {
            val aMap = mapHolder.aMap ?: return@LaunchedEffect

            aMap.clear()

            filteredPins.forEach { pin ->
                val color = categories.find { it.id == pin.categoryId }?.color ?: 0xFF666666.toInt()
                val label = pin.title.take(1)

                val avatarBitmap = pin.avatarPath?.let { path ->
                    try {
                        val filePath =
                            if (path.startsWith("file://")) path.toUri().path else path
                        filePath?.let { BitmapFactory.decodeFile(it) }
                    } catch (_: Exception) {
                        null
                    }
                }

                val size = context.resources.displayMetrics.density.let { d -> (48 * d).toInt() }
                val arrowH = (12 * context.resources.displayMetrics.density).toInt()
                val totalH = size + arrowH
                val bubble = createBitmap(size, totalH)
                val canvas = Canvas(bubble)

                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                paint.color = color

                // Bubble body (rounded rect)
                val bodyB = totalH - arrowH - 4f
                val rect = RectF(4f, 4f, size - 4f, bodyB)
                canvas.drawRoundRect(rect, 16f, 16f, paint)

                // Arrow
                val arrowPath = android.graphics.Path().apply {
                    val cx = size / 2f
                    val arrowTop = bodyB
                    moveTo(cx - 10f, arrowTop)
                    lineTo(cx + 10f, arrowTop)
                    lineTo(cx, totalH - 4f)
                    close()
                }
                canvas.drawPath(arrowPath, paint)

                val cx = size / 2f
                val cy = size / 2f
                val r = size / 2f - 8f

                if (avatarBitmap != null) {
                    val avatarR = r * 0.8f
                    val scaled = avatarBitmap.scale((avatarR * 2).toInt(), (avatarR * 2).toInt())
                    val clipPath = android.graphics.Path()
                        .apply { addCircle(cx, cy, avatarR, android.graphics.Path.Direction.CW) }
                    canvas.withClip(clipPath) {
                        drawBitmap(scaled, cx - avatarR, cy - avatarR, null)
                    }
                } else {
                    paint.color = color
                    canvas.drawCircle(cx, cy, r, paint)
                    if (label.isNotEmpty()) {
                        paint.color = 0xFFFFFFFF.toInt()
                        paint.textSize = r * 1.2f
                        paint.textAlign = Paint.Align.CENTER
                        paint.typeface = Typeface.DEFAULT_BOLD
                        canvas.drawText(label, cx, cy + paint.textSize / 3, paint)
                    }
                }

                val markerIcon = BitmapDescriptorFactory.fromBitmap(bubble)
                val markerOptions = MarkerOptions()
                    .position(LatLng(pin.latitude, pin.longitude))
                    .icon(markerIcon)
                    .snippet(pin.id.toString())
                    .draggable(true)
                    .anchor(0.5f, 1.0f)

                aMap.addMarker(markerOptions)
            }
        }
    }
}
