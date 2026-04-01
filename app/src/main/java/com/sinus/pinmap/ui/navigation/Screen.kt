package com.sinus.pinmap.ui.navigation

/**
 * 应用屏幕路由
 */
sealed class Screen(val route: String) {
    object Map : Screen("map")
    object PinEdit : Screen("pin_edit/{pinId}") {
        fun createRoute(pinId: Long?) = if (pinId != null) "pin_edit/$pinId" else "pin_edit/0"
    }
    object PinList : Screen("pin_list")
    object CategoryList : Screen("category_list")
    object OfflineMap : Screen("offline_map")
}