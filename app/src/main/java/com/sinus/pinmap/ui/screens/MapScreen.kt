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
import android.graphics.Bitmap
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
import com.amap.api.services.core.PoiItem
import com.amap.api.services.poisearch.PoiResult
import com.amap.api.services.poisearch.PoiSearch
import com.sinus.pinmap.data.database.PinmapDatabase
import com.sinus.pinmap.data.entity.Category
import com.sinus.pinmap.data.repository.PinRepository
import com.sinus.pinmap.ui.utils.LocationManager
import com.sinus.pinmap.ui.viewmodel.MapHolderViewModel
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
import androidx.core.net.toUri
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale

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
    onOpenDrawer: () -> Unit = {}
) {
    val context = LocalContext.current
    val database = remember { PinmapDatabase.getDatabase(context) }
    val pinRepository = remember { PinRepository(database.pinStore()) }
    val categoryRepository =
        remember { com.sinus.pinmap.data.repository.CategoryRepository(database.categoryStore()) }
    val fieldTemplateRepository =
        remember { com.sinus.pinmap.data.repository.FieldTemplateRepository(database.fieldTemplateStore()) }
    val fieldValueRepository =
        remember { com.sinus.pinmap.data.repository.FieldValueRepository(database.fieldValueStore()) }
    val viewModel: MapViewModel =
        viewModel { MapViewModel(pinRepository, fieldTemplateRepository, fieldValueRepository) }
    val mapHolder: MapHolderViewModel = viewModel()

    val pins by viewModel.pins.collectAsState()
    val categories by categoryRepository.getAllCategories().collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var showSearchResults by remember { mutableStateOf(false) }

    // 键盘控制器和焦点请求器
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchFocusRequester = remember { FocusRequester() }

    // 搜索结果
    var poiResults by remember { mutableStateOf<List<PoiItem>>(emptyList()) }

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
                val query = com.amap.api.services.poisearch.PoiSearch.Query(searchQuery, "", null)
                query.pageSize = 10
                query.pageNum = 0
                val search = PoiSearch(context, query)
                search.setOnPoiSearchListener(object : PoiSearch.OnPoiSearchListener {
                    override fun onPoiSearched(result: PoiResult?, code: Int) {
                        if (code == 1000) poiResults = result?.pois ?: emptyList()
                    }

                    override fun onPoiItemSearched(item: PoiItem?, code: Int) {}
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
        if (!mapHolder.isInitialized) {
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
                    }
                }

                Lifecycle.Event.ON_PAUSE -> {
                    try {
                        mapView.onPause()
                    } catch (e: Exception) {
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
    val navigationBarsInsets = WindowInsets.navigationBars
    var searchBottom by remember { mutableStateOf(96.dp) }
    LaunchedEffect(imeInsets) {
        snapshotFlow { imeInsets.getBottom(density) }
            .collect { imePx ->
                searchBottom = if (imePx > 0) with(density) { imePx.toDp() } + 16.dp else 112.dp
                Log.d("MapScreen", "imePx=$imePx searchBottom=$searchBottom navBarDp=${with(density) { navigationBarsInsets.getBottom(density).toDp() }}")
            }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapView }
        ) { mapView ->
        }

        // 设置地图事件监听
        LaunchedEffect(mapHolder.aMap) {
            val aMap = mapHolder.aMap ?: return@LaunchedEffect

            // 设置地图点击事件（用于取消选择）
            aMap.setOnMapClickListener { _ ->
                viewModel.clearSelectedPin()
            }

            // 设置地图长按事件（用于创建标记）
            aMap.setOnMapLongClickListener { latLng ->
                onNavigateToCreate(latLng.latitude, latLng.longitude)
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

        // 菜单按钮 - 放在左下角
        SmallFloatingActionButton(
            onClick = onOpenDrawer,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 80.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Icon(Icons.Default.Menu, contentDescription = "菜单")
        }

        // 定位按钮 - 放在搜索框右上方
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
                .padding(end = 16.dp, bottom = 80.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = "定位到当前位置")
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
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            items(searchResults) { pin ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp).clickable {
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
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            items(poiResults) { poi ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp).clickable {
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
        }

        // 监听 pins 变化，更新地图标记
        LaunchedEffect(pins) {
            val aMap = mapHolder.aMap ?: return@LaunchedEffect

            aMap.clear()

            pins.forEach { pin ->
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

                val size = context.resources.displayMetrics.density.let { d -> (72 * d).toInt() }
                val arrowH = (12 * context.resources.displayMetrics.density).toInt()
                val totalH = (size * 1.25f).toInt() + arrowH
                val bubble = createBitmap(size, totalH)
                val canvas = Canvas(bubble)

                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                paint.color = 0xFFFFFFFF.toInt()

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
                val cy = (size * 1.25f) / 2f - 8f
                val r = size / 2f - 16f

                if (avatarBitmap != null) {
                    val scaled = avatarBitmap.scale((r * 2).toInt(), (r * 2).toInt())
                    val clipPath = android.graphics.Path()
                        .apply { addCircle(cx, cy, r, android.graphics.Path.Direction.CW) }
                    canvas.save()
                    canvas.clipPath(clipPath)
                    canvas.drawBitmap(scaled, cx - r, cy - r, null)
                    canvas.restore()
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
