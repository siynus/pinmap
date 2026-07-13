package com.sinus.pinmap.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sinus.pinmap.ui.screens.CategoryListScreen
import com.sinus.pinmap.ui.screens.FieldTemplatesScreen
import com.sinus.pinmap.ui.screens.MapScreen
import com.sinus.pinmap.ui.screens.OfflineMapScreen
import com.sinus.pinmap.ui.screens.PinDetailScreen
import com.sinus.pinmap.ui.screens.PinEditScreen
import com.sinus.pinmap.ui.screens.PinFieldsScreen
import com.sinus.pinmap.ui.screens.PinListScreen

@Composable
fun PinmapNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Map.route,
    onOpenDrawer: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, animationSpec = tween(0))
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, animationSpec = tween(0))
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, animationSpec = tween(0))
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, animationSpec = tween(0))
        }
    ) {
        composable(Screen.Map.route) {
            MapScreen(
                onNavigateToEdit = { pinId ->
                    navController.navigate(Screen.PinEdit.createRoute(pinId))
                },
                onNavigateToCreate = { lat, lng ->
                    navController.navigate(Screen.PinEdit.createRoute(null, lat, lng))
                },
                onOpenDrawer = onOpenDrawer
            )
        }

        composable(
            route = Screen.PinEdit.route,
            arguments = listOf(
                navArgument("pinId") { type = NavType.LongType; defaultValue = 0L },
                navArgument("lat") { type = NavType.StringType; defaultValue = "0.0" },
                navArgument("lng") { type = NavType.StringType; defaultValue = "0.0" }
            )
        ) { backStackEntry ->
            val pinId = backStackEntry.arguments?.getLong("pinId") ?: 0L
            val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull() ?: 0.0
            val lng = backStackEntry.arguments?.getString("lng")?.toDoubleOrNull() ?: 0.0
            PinEditScreen(
                pinId = pinId,
                lat = lat,
                lng = lng,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.PinDetail.route,
            arguments = listOf(
                navArgument("pinId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val pinId = backStackEntry.arguments?.getLong("pinId") ?: 0L
            PinDetailScreen(
                pinId = pinId,
                onBack = { navController.popBackStack() },
                onNavigateToFields = {
                    navController.navigate(Screen.PinFields.createRoute(it))
                }
            )
        }

        composable(
            route = Screen.PinFields.route,
            arguments = listOf(navArgument("pinId") { type = NavType.LongType })
        ) { backStackEntry ->
            val pinId = backStackEntry.arguments?.getLong("pinId") ?: 0L
            PinFieldsScreen(
                pinId = pinId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.FieldTemplates.route,
            arguments = listOf(navArgument("categoryId") { type = NavType.LongType })
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getLong("categoryId") ?: 0L
            FieldTemplatesScreen(
                categoryId = categoryId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PinList.route) {
            PinListScreen(
                onPinClick = { pinId ->
                    navController.navigate(Screen.PinEdit.createRoute(pinId))
                }
            )
        }

        composable(Screen.CategoryList.route) {
            CategoryListScreen(
                onNavigateToFieldTemplates = { categoryId ->
                    navController.navigate(Screen.FieldTemplates.createRoute(categoryId))
                }
            )
        }

        composable(Screen.OfflineMap.route) {
            OfflineMapScreen()
        }
    }
}
