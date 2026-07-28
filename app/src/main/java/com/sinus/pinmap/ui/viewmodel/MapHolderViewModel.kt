package com.sinus.pinmap.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.amap.api.maps.AMap
import com.amap.api.maps.MapView

class MapHolderViewModel : ViewModel() {

    private var mMapView: MapView? = null
    val _mapView: MapView get() = mMapView ?: error("MapView not initialized")

    private var mAMap: AMap? = null
    val _aMap: AMap? get() = mAMap

    private var mIsInitialized = false
    val _isInitialized: Boolean get() = mIsInitialized

    fun init(context: Context): MapView {
        val mv = mMapView
        if (mv != null) {
            return mv
        }
        return MapView(context).apply {
            onCreate(null)
            mMapView = this
        }
    }

    fun setAMap(map: AMap) {
        mAMap = map
    }

    fun markInitialized() {
        mIsInitialized = true
    }

    override fun onCleared() {
        try {
            mMapView?.onPause()
            mMapView?.onDestroy()
        } catch (_: Exception) { }
        mMapView = null
        mAMap = null
        super.onCleared()
    }
}
