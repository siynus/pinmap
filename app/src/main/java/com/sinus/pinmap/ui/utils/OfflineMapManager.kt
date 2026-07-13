package com.sinus.pinmap.ui.utils

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import com.amap.api.maps.offlinemap.OfflineMapCity
import com.amap.api.maps.offlinemap.OfflineMapManager
import com.amap.api.maps.offlinemap.OfflineMapStatus

class OfflineMapManager(context: Context) {

    val downloadStates = mutableStateMapOf<String, Pair<Int, Int>>()

    private val manager = OfflineMapManager(
        context.applicationContext,
        object : OfflineMapManager.OfflineMapDownloadListener {
            override fun onDownload(status: Int, completeCode: Int, cityName: String) {
                downloadStates[cityName] = status to completeCode
            }
            override fun onCheckUpdate(hasNew: Boolean, cityName: String) {}
            override fun onRemove(success: Boolean, cityName: String, describe: String) {
                if (success) downloadStates.remove(cityName)
            }
        }
    )

    fun getAllCities(): List<OfflineMapCity> {
        return try {
            initStates()
            val list = manager.offlineMapCityList
            if (list != null) ArrayList(list) else emptyList()
        } catch (_: Exception) { emptyList() }
    }

    private fun initStates() {
        try {
            manager.downloadOfflineMapCityList?.forEach { city ->
                downloadStates[city.city] = city.getState() to city.getcompleteCode()
            }
        } catch (_: Exception) { }
    }

    fun downloadByCityName(name: String) {
        try {
            manager.downloadByCityName(name)
            downloadStates[name] = 0 to 0
        } catch (e: Exception) {
            Log.e("OfflineMap", "download error: $name", e)
        }
    }

    fun pauseByCityName(name: String) {
        try { manager.pauseByName(name) } catch (_: Exception) { }
    }

    fun removeByCityName(name: String) {
        try {
            manager.remove(name)
            downloadStates.remove(name)
        } catch (_: Exception) { }
    }
}
