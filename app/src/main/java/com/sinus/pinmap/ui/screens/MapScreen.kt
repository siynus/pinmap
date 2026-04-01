package com.sinus.pinmap.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
import com.sinus.pinmap.data.repository.PinRepository
import com.sinus.pinmap.ui.utils.LocationManager
import com.sinus.pinmap.ui.viewmodel.MapViewModel
import kotlinx.coroutines.launch

/**
 * 地图页面
 */
@SuppressLint("ClickableViewAccessibility")
@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    onNavigateToEdit: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val database = remember { PinmapDatabase.getDatabase(context) }
    val pinRepository = remember { PinRepository(database.pinDao()) }
    val viewModel: MapViewModel = viewModel { MapViewModel(pinRepository) }

    val pins by viewModel.pins.collectAsState()
    val selectedPin by viewModel.selectedPin.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
    var selectedAddress by remember { mutableStateOf("获取中...") }

    // 位置管理器
    val locationManager = remember { LocationManager(context) }

    // 记住 MapView 实例
    val mapView = remember {
        MapView(context)
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
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        // 初始化
        mapView.onCreate(null)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
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
                    // TODO: 显示详情弹窗
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

        // 定位按钮
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
                .align(androidx.compose.ui.Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = "定位到当前位置")
        }
    }

    // 监听 pins 变化，更新地图标记
    LaunchedEffect(pins) {
        // TODO: 更新地图上的标记
        // 需要清除旧标记并添加新标记
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
            onConfirm = { title, description ->
                viewModel.createPin(
                    latitude = selectedLocation!!.latitude,
                    longitude = selectedLocation!!.longitude,
                    title = title,
                    description = description.ifBlank { null }
                )
                showCreateDialog = false
                selectedLocation = null
            },
            onDismiss = {
                showCreateDialog = false
                selectedLocation = null
            }
        )
    }
}