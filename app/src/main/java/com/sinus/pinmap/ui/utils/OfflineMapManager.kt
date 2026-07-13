package com.sinus.pinmap.ui.utils

import android.content.Context
import com.amap.api.maps.MapsInitializer
import com.amap.api.maps.offlinemap.OfflineMapCity
import com.amap.api.maps.offlinemap.OfflineMapManager
import com.amap.api.maps.offlinemap.OfflineMapProvince
import com.amap.api.maps.offlinemap.OfflineMapStatus
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 离线地图管理器
 */
class OfflineMapManager(private val context: Context) {

    companion object {
        @Volatile
        private var instance: com.amap.api.maps.offlinemap.OfflineMapManager? = null

        /**
         * 获取离线地图管理器单例
         */
        fun getInstance(context: Context): com.amap.api.maps.offlinemap.OfflineMapManager {
            return instance ?: synchronized(this) {
                instance ?: try {
                    com.amap.api.maps.offlinemap.OfflineMapManager(context.applicationContext, null).also {
                        instance = it
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    throw e
                }
            }
        }

        /**
         * 销毁离线地图管理器
         */
        fun destroy() {
            try {
                instance?.destroy()
                instance = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private val offlineMapManager by lazy {
        getInstance(context)
    }

    /**
     * 获取所有城市列表
     */
    fun getAllCities(): List<OfflineMapCity> {
        return try {
            offlineMapManager.getOfflineMapCityList() ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 获取所有省份列表
     */
    fun getAllProvinces(): List<OfflineMapProvince> {
        return try {
            offlineMapManager.getOfflineMapProvinceList() ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 下载城市离线地图
     */
    suspend fun downloadCity(city: OfflineMapCity): Result<Unit> = suspendCancellableCoroutine { continuation ->
        try {
            offlineMapManager.downloadByCityName(city.city)
            continuation.resume(Result.success(Unit))
        } catch (e: Exception) {
            e.printStackTrace()
            continuation.resume(Result.failure(e))
        }
    }

    /**
     * 删除城市离线地图
     */
    suspend fun removeCity(city: OfflineMapCity): Result<Unit> = suspendCancellableCoroutine { continuation ->
        try {
            offlineMapManager.remove(city.city)
            continuation.resume(Result.success(Unit))
        } catch (e: Exception) {
            e.printStackTrace()
            continuation.resume(Result.failure(e))
        }
    }

    /**
     * 获取已下载的城市列表
     */
    fun getDownloadedCities(): List<OfflineMapCity> {
        return try {
            offlineMapManager.getDownloadOfflineMapCityList() ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 获取城市下载进度
     */
    fun getCityProgress(city: OfflineMapCity): Int {
        return try {
            city.getcompleteCode()
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    /**
     * 更新离线地图
     */
    suspend fun updateCity(city: OfflineMapCity): Result<Unit> = suspendCancellableCoroutine { continuation ->
        try {
            offlineMapManager.updateOfflineCityByName(city.city)
            continuation.resume(Result.success(Unit))
        } catch (e: Exception) {
            e.printStackTrace()
            continuation.resume(Result.failure(e))
        }
    }
}