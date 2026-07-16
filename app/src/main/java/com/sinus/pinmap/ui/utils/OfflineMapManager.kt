package com.sinus.pinmap.ui.utils

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import com.amap.api.maps.offlinemap.OfflineMapCity
import com.amap.api.maps.offlinemap.OfflineMapManager
import com.amap.api.maps.offlinemap.OfflineMapStatus

class OfflineMapManager(context: Context) {

    val mDownloadStates = mutableStateMapOf<String, Pair<Int, Int>>()

    private val mManager = OfflineMapManager(
        context.applicationContext,
        object : OfflineMapManager.OfflineMapDownloadListener {
            override fun onDownload(status: Int, completeCode: Int, cityName: String) {
                mDownloadStates[cityName] = status to completeCode
            }
            override fun onCheckUpdate(hasNew: Boolean, cityName: String) {}
            override fun onRemove(success: Boolean, cityName: String, describe: String) {
                if (success) mDownloadStates.remove(cityName)
            }
        }
    )

    fun getAllCities(): List<OfflineMapCity> {
        return try {
            initStates()
            val list = mManager.offlineMapCityList
            if (list != null) ArrayList(list) else emptyList()
        } catch (_: Exception) { emptyList() }
    }

    private fun initStates() {
        try {
            mManager.downloadOfflineMapCityList?.forEach { city ->
                mDownloadStates[city.city] = city.getState() to city.getcompleteCode()
            }
        } catch (_: Exception) { }
    }

    fun downloadByCityName(name: String) {
        try {
            mManager.downloadByCityName(name)
            mDownloadStates[name] = 0 to 0
        } catch (e: Exception) {
            Log.e("OfflineMap", "download error: $name", e)
        }
    }

    fun pauseByCityName(name: String) {
        try { mManager.pauseByName(name) } catch (_: Exception) { }
    }

    fun removeByCityName(name: String) {
        try {
            mManager.remove(name)
            mDownloadStates.remove(name)
        } catch (_: Exception) { }
    }
}
