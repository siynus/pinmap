package com.sinus.pinmap.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sinus.pinmap.ui.screens.CategoryListScreen
import com.sinus.pinmap.ui.screens.MapScreen
import com.sinus.pinmap.ui.screens.OfflineMapScreen
import com.sinus.pinmap.ui.screens.PinDetailScreen
import com.sinus.pinmap.ui.screens.PinEditScreen
import com.sinus.pinmap.ui.screens.PinListScreen

/**
 * 应用导航图
 */
@Composable
fun PinmapNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Map.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            // 无进场动画
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, animationSpec = tween(0))
        },
        exitTransition = {
            // 无退场动画
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, animationSpec = tween(0))
        },
        popEnterTransition = {
            // 无进场动画
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, animationSpec = tween(0))
        },
        popExitTransition = {
            // 无退场动画
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, animationSpec = tween(0))
        }
    ) {
        // 地图页面
        composable(Screen.Map.route) {
            MapScreen(
                onNavigateToEdit = { pinId ->
                    navController.navigate(Screen.PinEdit.createRoute(pinId))
                }
            )
        }

        // 标记编辑页面
        composable(
            route = Screen.PinEdit.route,
            arguments = listOf(
                navArgument("pinId") {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) { backStackEntry ->
            val pinId = backStackEntry.arguments?.getLong("pinId") ?: 0L
            // TODO: 从 ViewModel 加载现有标记数据
            PinEditScreen(
                pinId = pinId,
                onSave = { title, description ->
                    // TODO: 保存标记
                    navController.popBackStack()
                },
                onCancel = {
                    navController.popBackStack()
                }
            )
        }

        // 标记详情页面
        composable(
            route = Screen.PinDetail.route,
            arguments = listOf(
                navArgument("pinId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val pinId = backStackEntry.arguments?.getLong("pinId") ?: 0L
            PinDetailScreen(
                pinId = pinId,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // 标记列表页面
        composable(Screen.PinList.route) {
            PinListScreen(
                onPinClick = { pinId ->
                    navController.navigate(Screen.PinDetail.createRoute(pinId))
                }
            )
        }

        // 分类列表页面
        composable(Screen.CategoryList.route) {
            CategoryListScreen()
        }

        // 离线地图页面
        composable(Screen.OfflineMap.route) {
            OfflineMapScreen()
        }
    }
}