package com.sinus.pinmap

import android.app.Application
import com.amap.api.maps.MapsInitializer

/**
 * Pinmap Application
 * 负责全局SDK的初始化
 */
class PinmapApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 禁用自动更新，避免权限问题
        System.setProperty("amap.sdk.update.enable", "false")

        // 初始化高德地图SDK
        try {
            MapsInitializer.initialize(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}