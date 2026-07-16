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
import com.amap.api.maps.model.LatLng
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 位置管理器
 */
class LocationManager(private val mContext: Context) {

    companion object {
        private const val PREFS_NAME = "location_prefs"
        private const val KEY_LAST_LATITUDE = "last_latitude"
        private const val KEY_LAST_LONGITUDE = "last_longitude"
        private const val KEY_LAST_ZOOM = "last_zoom"

        // 默认位置：中国中心
        val mDefaultLocation = LatLng(35.8617, 104.1954)
        const val DEFAULT_ZOOM = 4f

        // 隐私合规状态
        private var mPrivacyAgreed = false
    }

    private val mPrefs = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        if (!mPrivacyAgreed) {
            AMapLocationClient.updatePrivacyShow(mContext, true, true)
            AMapLocationClient.updatePrivacyAgree(mContext, true)
            mPrivacyAgreed = true
        }
    }

    /**
     * 保存位置
     */
    fun saveLastLocation(lat: Double, lng: Double, zoom: Float) {
        mPrefs.edit().apply {
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
        val lat = mPrefs.getFloat(KEY_LAST_LATITUDE, mDefaultLocation.latitude.toFloat()).toDouble()
        val lng = mPrefs.getFloat(KEY_LAST_LONGITUDE, mDefaultLocation.longitude.toFloat()).toDouble()
        val zoom = mPrefs.getFloat(KEY_LAST_ZOOM, DEFAULT_ZOOM)
        return LatLng(lat, lng) to zoom
    }

    /**
     * 检查位置权限
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            mContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            mContext,
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

        val locationClient = AMapLocationClient(mContext.applicationContext).apply {
            setLocationOption(
                AMapLocationClientOption().apply {
                    locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
                    isOnceLocation = true
                    isNeedAddress = true  // 启用地址获取
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
     * 获取当前位置的地址信息
     */
    suspend fun getCurrentAddress(): Result<String> = suspendCancellableCoroutine { continuation ->
        if (!hasLocationPermission()) {
            continuation.resume(Result.failure(SecurityException("缺少位置权限")))
            return@suspendCancellableCoroutine
        }

        val locationClient = AMapLocationClient(mContext.applicationContext).apply {
            setLocationOption(
                AMapLocationClientOption().apply {
                    locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
                    isOnceLocation = true
                    isNeedAddress = true
                    isLocationCacheEnable = false
                    httpTimeOut = 10000
                }
            )
            setLocationListener(object : AMapLocationListener {
                override fun onLocationChanged(location: AMapLocation?) {
                    location?.let {
                        if (it.errorCode == 0) {
                            val address = it.address ?: "未知地址"
                            continuation.resume(Result.success(address))
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
     * 根据经纬度获取地址（逆地理编码）
     * 注意：高德 2D 地图 SDK 不包含地理编码服务，此功能暂时不可用
     * 如果需要此功能，建议使用定位结果中的地址或升级到 3D 地图 SDK
     */
    suspend fun getAddress(lat: Double, lng: Double): Result<String> {
        // 高德 2D 地图 SDK 不包含地理编码服务
        // 返回经纬度作为临时解决方案
        return Result.success("纬度: ${String.format("%.6f", lat)}, 经度: ${String.format("%.6f", lng)}")
    }

    /**
     * 将 AMapLocation 转换为 LatLng
     */
    fun aMapLocationToLatLng(location: Location): LatLng {
        return LatLng(location.latitude, location.longitude)
    }
}

fun haversineDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
    val R = 6371000f
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val sinLat = sin(dLat / 2)
    val sinLng = sin(dLng / 2)
    val a = sinLat * sinLat + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sinLng * sinLng
    return (R * 2 * atan2(sqrt(a), sqrt(1 - a))).toFloat()
}