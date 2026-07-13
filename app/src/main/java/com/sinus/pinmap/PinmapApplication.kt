package com.sinus.pinmap

import android.app.Application
import com.amap.api.maps.MapsInitializer

class PinmapApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        System.setProperty("amap.sdk.update.enable", "false")

        try {
            MapsInitializer.initialize(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
