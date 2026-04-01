package com.sinus.pinmap.ui.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.amap.api.maps2d.model.LatLng
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 位置管理器
 */
class LocationManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "location_prefs"
        private const val KEY_LAST_LATITUDE = "last_latitude"
        private const val KEY_LAST_LONGITUDE = "last_longitude"
        private const val KEY_LAST_ZOOM = "last_zoom"

        // 默认位置：中国中心
        val DEFAULT_LOCATION = LatLng(35.8617, 104.1954)
        const val DEFAULT_ZOOM = 4f
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * 保存位置
     */
    fun saveLastLocation(lat: Double, lng: Double, zoom: Float) {
        prefs.edit().apply {
            putFloat(KEY_LAST_LATITUDE, lat.toFloat())
            putFloat(KEY_LAST_LONGITUDE, lng.toFloat())
            putFloat(KEY_LAST_ZOOM, zoom)
            apply()
        }
    }

    /**
     * 获取上次保存的位置
     */
    fun getLastLocation(): Pair<LatLng, Float> {
        val lat = prefs.getFloat(KEY_LAST_LATITUDE, DEFAULT_LOCATION.latitude.toFloat()).toDouble()
        val lng = prefs.getFloat(KEY_LAST_LONGITUDE, DEFAULT_LOCATION.longitude.toFloat()).toDouble()
        val zoom = prefs.getFloat(KEY_LAST_ZOOM, DEFAULT_ZOOM)
        return LatLng(lat, lng) to zoom
    }

    /**
     * 检查位置权限
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 获取当前位置（一次性定位）
     */
    suspend fun getCurrentLocation(): Result<LatLng> = suspendCancellableCoroutine { continuation ->
        if (!hasLocationPermission()) {
            continuation.resume(Result.failure(SecurityException("缺少位置权限")))
            return@suspendCancellableCoroutine
        }

        val locationClient = AMapLocationClient(context.applicationContext).apply {
            setLocationOption(
                AMapLocationClientOption().apply {
                    locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
                    isOnceLocation = true
                    isNeedAddress = false
                    isLocationCacheEnable = false
                    httpTimeOut = 10000
                }
            )
            setLocationListener(object : AMapLocationListener {
                override fun onLocationChanged(location: AMapLocation?) {
                    location?.let {
                        if (it.errorCode == 0) {
                            val latLng = LatLng(it.latitude, it.longitude)
                            continuation.resume(Result.success(latLng))
                        } else {
                            continuation.resume(
                                Result.failure(
                                    Exception("定位失败: ${it.errorCode} - ${it.errorInfo}")
                                )
                            )
                        }
                    } ?: continuation.resume(Result.failure(Exception("定位结果为空")))

                    stopLocation()
                    onDestroy()
                }
            })
        }

        continuation.invokeOnCancellation {
            locationClient.stopLocation()
            locationClient.onDestroy()
        }

        locationClient.startLocation()
    }

    /**
     * 将 AMapLocation 转换为 LatLng
     */
    fun aMapLocationToLatLng(location: Location): LatLng {
        return LatLng(location.latitude, location.longitude)
    }
}