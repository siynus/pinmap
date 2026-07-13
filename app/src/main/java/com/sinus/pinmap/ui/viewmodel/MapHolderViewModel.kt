package com.sinus.pinmap.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.amap.api.maps.AMap
import com.amap.api.maps.MapView

class MapHolderViewModel : ViewModel() {

    private var _mapView: MapView? = null
    val mapView: MapView get() = _mapView ?: error("MapView not initialized")

    private var _aMap: AMap? = null
    val aMap: AMap? get() = _aMap

    var isInitialized = false
        private set

    fun init(context: Context): MapView {
        val mv = _mapView
        if (mv != null) return mv
        return MapView(context).apply {
            onCreate(null)
            _mapView = this
        }
    }

    fun setAMap(map: AMap) {
        _aMap = map
    }

    fun markInitialized() {
        isInitialized = true
    }

    override fun onCleared() {
        _mapView?.onDestroy()
        super.onCleared()
    }
}
