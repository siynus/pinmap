package com.sinus.pinmap.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sinus.pinmap.ui.screens.FieldTemplatesScreen
import com.sinus.pinmap.ui.screens.OfflineMapScreen
import com.sinus.pinmap.ui.screens.PinEditScreen
import com.sinus.pinmap.ui.screens.SettingsScreen

@Composable
fun SubPagesNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "empty",
        modifier = modifier,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, animationSpec = tween(300))
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, animationSpec = tween(300))
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, animationSpec = tween(300))
        }
    ) {
        composable("empty") { Box(Modifier) }

        composable(
            route = Screen.PinEdit.mRoute,
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
                onBack = { savedLat, savedLng ->
                    navController.previousBackStackEntry?.savedStateHandle?.apply {
                        set("focusLat", savedLat)
                        set("focusLng", savedLng)
                    }
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.FieldTemplates.mRoute,
            arguments = listOf(navArgument("categoryId") { type = NavType.LongType })
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getLong("categoryId") ?: 0L
            FieldTemplatesScreen(
                categoryId = categoryId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.OfflineMap.mRoute) {
            OfflineMapScreen()
        }

        composable(Screen.Settings.mRoute) {
            SettingsScreen()
        }
    }
}
