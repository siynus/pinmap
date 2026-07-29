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
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.sinus.pinmap.data.entity.Pin
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
    onNavigateToCreate: (Double, Double) -> Unit = { _, _ -> },
    focusLat: Double? = null,
    focusLng: Double? = null
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

    val pins by viewModel._pins.collectAsState()
    val categories by categoryRepository.getAllCategories().collectAsState(initial = emptyList())
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    val filteredPins by remember(pins, selectedCategoryId) {
        derivedStateOf {
            if (selectedCategoryId == null) {
                pins
            } else {
                pins.filter { it.categoryId == selectedCategoryId }
            }
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
                        if (code == 1000) {
                            poiResults = result?.pois ?: emptyList()
                        }
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
    var markerMap by remember { mutableStateOf<Map<Long, Marker>>(emptyMap()) }
    var viewerPin by remember { mutableStateOf<Pin?>(null) }
    var highlightedPinId by remember { mutableStateOf<Long?>(null) }
    var poiHighlightLat by remember { mutableDoubleStateOf(0.0) }
    var poiHighlightLng by remember { mutableDoubleStateOf(0.0) }
    var isDragging by remember { mutableStateOf(false) }

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
        if (!mapHolder._isInitialized) {
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
                locationResult.getOrNull()!! to ZOOM_LOCATION
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

    LaunchedEffect(focusLat, focusLng) {
        if (focusLat != null && focusLng != null) {
            mapHolder._aMap?.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    com.amap.api.maps.model.LatLng(focusLat, focusLng), ZOOM_LOCATION
                )
            )
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
                    (with(density) { imePx.toDp() } - NAV_BAR_GAP).coerceAtLeast(0.dp)
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
        LaunchedEffect(mapHolder._aMap) {
            val aMap = mapHolder._aMap ?: return@LaunchedEffect

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
                val pin = pins.find { it.id == pinId }
                if (pin?.avatarPath != null) {
                    viewerPin = pin
                } else {
                    onNavigateToEdit(pinId)
                }
                true
            }

            // 设置标记拖拽事件
            aMap.setOnMarkerDragListener(object : com.amap.api.maps.AMap.OnMarkerDragListener {
                override fun onMarkerDragStart(marker: com.amap.api.maps.model.Marker?) {
                    isDragging = true
                }
                override fun onMarkerDrag(marker: com.amap.api.maps.model.Marker?) {}
                override fun onMarkerDragEnd(marker: com.amap.api.maps.model.Marker?) {
                    val id = marker?.snippet?.toLongOrNull() ?: return
                    val pos = marker.position
                    scope.launch {
                        pinRepository.getPinById(id)?.let { pin ->
                            val address = try {
                                kotlinx.coroutines.suspendCancellableCoroutine<String?> { cont ->
                                    val search = com.amap.api.services.geocoder.GeocodeSearch(context)
                                    search.setOnGeocodeSearchListener(object : com.amap.api.services.geocoder.GeocodeSearch.OnGeocodeSearchListener {
                                        override fun onRegeocodeSearched(res: com.amap.api.services.geocoder.RegeocodeResult?, code: Int) {
                                            cont.resume(if (code == 1000 && res != null) res.regeocodeAddress.formatAddress else null, null)
                                        }
                                        override fun onGeocodeSearched(res: com.amap.api.services.geocoder.GeocodeResult?, code: Int) {}
                                    })
                                    search.getFromLocationAsyn(
                                        com.amap.api.services.geocoder.RegeocodeQuery(
                                            com.amap.api.services.core.LatLonPoint(pos.latitude, pos.longitude),
                                            200f, com.amap.api.services.geocoder.GeocodeSearch.AMAP
                                        )
                                    )
                                }
                            } catch (_: Exception) { null }
                            pinRepository.updatePin(
                                pin.copy(
                                    latitude = pos.latitude,
                                    longitude = pos.longitude,
                                    address = address ?: pin.address
                                )
                            )
                        }
                    }
                    isDragging = false
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
                                .heightIn(max = SEARCH_RESULT_MAX_HEIGHT)
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
                                                val aMap = mapHolder._aMap ?: return@launch
                                                highlightedPinId = pin.id
                                                aMap.animateCamera(
                                                    CameraUpdateFactory.newLatLngZoom(
                                                        LatLng(
                                                            pin.latitude,
                                                            pin.longitude
                                                        ), ZOOM_SEARCH
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
                                            val aMap = mapHolder._aMap ?: return@clickable
                                            poiHighlightLat = poi.latLonPoint.latitude
                                            poiHighlightLng = poi.latLonPoint.longitude
                                            aMap.animateCamera(
                                                CameraUpdateFactory.newLatLngZoom(
                                                    LatLng(
                                                        poi.latLonPoint.latitude,
                                                        poi.latLonPoint.longitude
                                                    ), ZOOM_SEARCH
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
                                        val aMap = mapHolder._aMap ?: return@launch
                                        val latLng = LatLng(pin.latitude, pin.longitude)
                                        aMap.animateCamera(
                                            CameraUpdateFactory.newLatLngZoom(
                                                latLng,
                                                ZOOM_SEARCH
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
                .padding(start = 16.dp, bottom = searchBottom + FAB_OFFSET)
        ) {
            SmallFloatingActionButton(
                onClick = { showCategoryFilter = true },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(FAB_CORNER_RADIUS),
                modifier = Modifier.size(FAB_SIZE)
            ) {
                Icon(Icons.Default.Menu, contentDescription = "筛选分类", modifier = Modifier.size(FAB_ICON_SIZE))
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
                                val aMap = mapHolder._aMap ?: return@launch

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
                                aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, ZOOM_LOCATION))
                            }
                        }
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = searchBottom + FAB_OFFSET)
                .size(FAB_SIZE),
            shape = RoundedCornerShape(FAB_CORNER_RADIUS),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = "定位到当前位置", modifier = Modifier.size(FAB_ICON_SIZE))
        }

        // 监听 pins 变化，更新地图标记
        LaunchedEffect(filteredPins, categories, highlightedPinId) {
            val aMap = mapHolder._aMap ?: return@LaunchedEffect
            if (isDragging) {
                return@LaunchedEffect
            }

            val currentIds = filteredPins.map { it.id }.toSet()
            val oldIds = markerMap.keys

            // 移除已删除的标记
            oldIds.subtract(currentIds).forEach { id ->
                markerMap[id]?.remove()
            }

            // 添加新标记，高亮变化时重建所有（无 aMap.clear 不闪）
            val newMap = markerMap.toMutableMap()
            filteredPins.forEach { pin ->
                val oldMarker = markerMap[pin.id]
                if (oldMarker != null && highlightedPinId != pin.id && oldMarker.position == LatLng(pin.latitude, pin.longitude)) {
                    // 标记未变化且不是高亮目标，跳过
                    newMap[pin.id] = oldMarker
                } else {
                    // 需要创建新标记或更新旧标记
                    oldMarker?.remove()
                    val color = categories.find { it.id == pin.categoryId }?.color ?: DEFAULT_PIN_COLOR
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

                val size = context.resources.displayMetrics.density.let { d -> (MARKER_SIZE_PX * d).toInt() }
                val arrowH = (ARROW_HEIGHT_PX * context.resources.displayMetrics.density).toInt()
                val totalH = size + arrowH
                val bubble = createBitmap(size, totalH)
                val canvas = Canvas(bubble)

                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                paint.color = color

                if (avatarBitmap != null) {
                    val cx = size / 2f
                    val cy = size / 2f
                    val r = size / 2f - PADDING_4F
                    val minDim = minOf(avatarBitmap.width, avatarBitmap.height)
                    val srcX = ((avatarBitmap.width - minDim) / 2f).toInt()
                    val srcY = ((avatarBitmap.height - minDim) / 2f).toInt()
                    val srcRect = android.graphics.Rect(srcX, srcY, srcX + minDim, srcY + minDim)
                    val dstRect = android.graphics.RectF(cx - r, cy - r, cx + r, cy + r)
                    val clipPath = android.graphics.Path()
                        .apply { addCircle(cx, cy, r, android.graphics.Path.Direction.CW) }
                    canvas.save()
                    canvas.clipPath(clipPath)
                    canvas.drawBitmap(avatarBitmap, srcRect, dstRect, null)
                    canvas.restore()
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = STROKE_WIDTH_AVATAR
                    paint.color = COLOR_WHITE_INT
                    canvas.drawCircle(cx, cy, r, paint)
                    paint.style = Paint.Style.FILL
                } else {
                    val bodyB = totalH - arrowH - PADDING_4F
                    val rect = android.graphics.RectF(PADDING_4F, PADDING_4F, size - PADDING_4F, bodyB)
                    canvas.drawRoundRect(rect, CORNER_RADIUS_BODY, CORNER_RADIUS_BODY, paint)
                    val cx = size / 2f
                    val cy = bodyB / 2f
                    val r = bodyB / 2f - 8f
                    if (label.isNotEmpty()) {
                        paint.color = COLOR_WHITE_INT
                        paint.textSize = r * TEXT_SIZE_MULTIPLIER
                        paint.textAlign = Paint.Align.CENTER
                        paint.typeface = Typeface.DEFAULT_BOLD
                        canvas.drawText(label, cx, cy + paint.textSize / 3, paint)
                    }
                }

                // Arrow
                paint.color = color
                val cx = size / 2f
                val arrowPath = android.graphics.Path().apply {
                    val arrowTop = if (avatarBitmap != null) size - PADDING_4F else totalH - arrowH - PADDING_4F
                    moveTo(cx - ARROW_HALF_WIDTH, arrowTop)
                    lineTo(cx + ARROW_HALF_WIDTH, arrowTop)
                    lineTo(cx, totalH - PADDING_4F)
                    close()
                }
                canvas.drawPath(arrowPath, paint)

                // 高亮边框
                if (pin.id == highlightedPinId) {
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = STROKE_WIDTH_HIGHLIGHT
                    paint.color = HIGHLIGHT_RED
                    if (avatarBitmap != null) {
                        val r = size / 2f - PADDING_4F
                        canvas.drawCircle(size / 2f, size / 2f, r + PADDING_2F, paint)
                    } else {
                        val bodyB = totalH - arrowH - PADDING_4F
                        val rect = android.graphics.RectF(PADDING_2F, PADDING_2F, size - PADDING_2F, bodyB + PADDING_2F)
                        canvas.drawRoundRect(rect, CORNER_RADIUS_HIGHLIGHT, CORNER_RADIUS_HIGHLIGHT, paint)
                    }
                    paint.style = Paint.Style.FILL
                }

                val markerIcon = BitmapDescriptorFactory.fromBitmap(bubble)
                val markerOptions = MarkerOptions()
                    .position(LatLng(pin.latitude, pin.longitude))
                    .icon(markerIcon)
                    .snippet(pin.id.toString())
                    .draggable(true)
                    .anchor(0.5f, 1.0f)

                val marker = aMap.addMarker(markerOptions)
                marker?.let { newMap[pin.id] = it }
                }
            }
            markerMap = newMap
        }
    }

    // POI 地址搜索结果高亮
    LaunchedEffect(poiHighlightLat, poiHighlightLng) {
        val aMap = mapHolder._aMap ?: return@LaunchedEffect
        if (poiHighlightLat != 0.0 || poiHighlightLng != 0.0) {
            aMap.addMarker(
                MarkerOptions()
                    .position(LatLng(poiHighlightLat, poiHighlightLng))
                    .title("搜索结果")
                    .zIndex(998f)
            )
        }
    }

    viewerPin?.let { pin ->
        Dialog(
            onDismissRequest = { viewerPin = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { viewerPin = null }
            ) {
                if (pin.avatarPath != null) {
                    Image(
                        painter = coil.compose.rememberAsyncImagePainter(
                            if (pin.avatarPath.startsWith("/")) pin.avatarPath else "file://${pin.avatarPath}"
                        ),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(32.dp)
                ) {
                    Button(onClick = {
                        viewerPin = null
                        onNavigateToEdit(pin.id)
                    }) { Text("编辑") }
                }
            }
        }
    }
}

private val DEFAULT_PIN_COLOR = 0xFF666666.toInt()
private val COLOR_WHITE_INT = 0xFFFFFFFF.toInt()
private val HIGHLIGHT_RED = 0xFFFF4444.toInt()
private val NAV_BAR_GAP = 80.dp
private val FAB_OFFSET = 64.dp
private val SEARCH_RESULT_MAX_HEIGHT = 290.dp
private val FAB_SIZE = 30.dp
private val FAB_ICON_SIZE = 18.dp
private val FAB_CORNER_RADIUS = 10.dp
private const val MARKER_SIZE_PX = 48
private const val ARROW_HEIGHT_PX = 12
private const val STROKE_WIDTH_AVATAR = 3f
private const val STROKE_WIDTH_HIGHLIGHT = 4f
private const val CORNER_RADIUS_BODY = 16f
private const val CORNER_RADIUS_HIGHLIGHT = 14f
private const val TEXT_SIZE_MULTIPLIER = 1.4f
private const val ARROW_HALF_WIDTH = 10f
private const val PADDING_4F = 4f
private const val PADDING_2F = 2f
private const val ZOOM_LOCATION = 15f
private const val ZOOM_SEARCH = 16f
