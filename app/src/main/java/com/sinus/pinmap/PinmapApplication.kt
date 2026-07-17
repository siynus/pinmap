package com.sinus.pinmap

import android.app.Application
import android.util.Log
import com.amap.api.maps.MapsInitializer
import com.sinus.pinmap.ui.utils.AuthState

class PinmapApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AuthState.init(this)
        val savedKey = AuthState.getSavedKey()
        val apiKey = savedKey ?: BuildConfig.MAPS_API_KEY
        MapsInitializer.setApiKey(apiKey)
        MapsInitializer.updatePrivacyShow(this, true, true)
        MapsInitializer.updatePrivacyAgree(this, true)
        System.setProperty("amap.sdk.update.enable", "false")
        try {
            MapsInitializer.initialize(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
