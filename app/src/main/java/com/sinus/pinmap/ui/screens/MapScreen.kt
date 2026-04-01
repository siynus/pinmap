package com.sinus.pinmap.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amap.api.maps2d.AMap
import com.amap.api.maps2d.CameraUpdateFactory
import com.amap.api.maps2d.MapView
import com.amap.api.maps2d.model.LatLng
import com.amap.api.maps2d.model.Marker
import com.amap.api.maps2d.model.MarkerOptions
import com.sinus.pinmap.data.database.PinmapDatabase
import com.sinus.pinmap.data.repository.PinRepository
import com.sinus.pinmap.ui.utils.LocationManager
import com.sinus.pinmap.ui.viewmodel.MapViewModel
import kotlinx.coroutines.launch

/**
 * 地图页面
 */
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

    // 位置管理器
    val locationManager = remember { LocationManager(context) }

    // 记住 MapView 实例
    val mapView = remember {
        MapView(context)
    }

    // 地图是否已初始化
    var isMapInitialized by remember { mutableStateOf(false) }

    // 初始化地图位置
    LaunchedEffect(mapView) {
        if (!isMapInitialized) {
            val aMap = mapView.map

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

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { mapView }
    ) { mapView ->
        val aMap = mapView.map

        // 设置地图点击事件
        aMap.setOnMapClickListener { latLng ->
            selectedLocation = latLng
            showCreateDialog = true
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
        aMap.setOnCameraChangeListener(object : com.amap.api.maps2d.AMap.OnCameraChangeListener {
            override fun onCameraChange(cameraPosition: com.amap.api.maps2d.model.CameraPosition?) {
                // 实时移动中
            }

            override fun onCameraChangeFinish(cameraPosition: com.amap.api.maps2d.model.CameraPosition?) {
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