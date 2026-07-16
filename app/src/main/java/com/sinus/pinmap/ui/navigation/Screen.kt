package com.sinus.pinmap.ui.navigation

sealed class Screen(val route: String) {
    object Map : Screen("map")
    object PinEdit : Screen("pin_edit/{pinId}?lat={lat}&lng={lng}") {
        fun createRoute(pinId: Long?, lat: Double? = null, lng: Double? = null): String {
            val base = if (pinId != null) "pin_edit/$pinId" else "pin_edit/0"
            return if (lat != null && lng != null) "$base?lat=$lat&lng=$lng" else base
        }
    }
    object PinList : Screen("pin_list")
    object CategoryList : Screen("category_list")
    object OfflineMap : Screen("offline_map")
    object FieldTemplates : Screen("field_templates/{categoryId}") {
        fun createRoute(categoryId: Long) = "field_templates/$categoryId"
    }
}
