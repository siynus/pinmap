package com.sinus.pinmap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sinus.pinmap.ui.components.NavigationDrawer
import com.sinus.pinmap.ui.navigation.PinmapNavGraph
import com.sinus.pinmap.ui.navigation.Screen
import com.sinus.pinmap.ui.theme.PinmapTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PinmapTheme {
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route ?: ""

                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                val tabs = remember {
                    listOf(
                        TabItem("地图", Icons.Default.LocationOn, Screen.Map.route),
                        TabItem("标记列表", Icons.AutoMirrored.Filled.List, Screen.PinList.route),
                        TabItem("分类管理", Icons.Default.Edit, Screen.CategoryList.route),
                        TabItem("离线地图", Icons.Default.Info, Screen.OfflineMap.route, visible = false)
                    )
                }

                val tabRoutes = tabs.filter { it.visible }.map { it.route }
                val isTabScreen = currentRoute in tabRoutes

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = currentRoute != "map",
                    drawerContent = {
                        NavigationDrawer(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                if (route != currentRoute) {
                                    scope.launch {
                                        drawerState.snapTo(DrawerValue.Closed)
                                        navController.navigate(route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                        }
                                    }
                                } else {
                                    scope.launch {
                                        drawerState.snapTo(DrawerValue.Closed)
                                    }
                                }
                            }
                        )
                    }
                ) {
                    Column(Modifier.fillMaxSize()) {
                        if (isTabScreen) {
                            TopAppBar(
                                title = {
                                    Text(
                                        when (currentRoute) {
                                            Screen.Map.route -> "地图"
                                            Screen.PinList.route -> "标记列表"
                                            Screen.CategoryList.route -> "分类管理"
                                            else -> "Pinmap"
                                        }
                                    )
                                },
                                navigationIcon = {
                                    IconButton(onClick = {
                                        scope.launch { drawerState.open() }
                                    }) {
                                        Icon(Icons.Default.Menu, contentDescription = "菜单")
                                    }
                                }
                            )
                        }

                        Box(Modifier.weight(1f)) {
                            PinmapNavGraph(
                                navController = navController,
                                onOpenDrawer = { scope.launch { drawerState.open() } }
                            )
                        }

                        if (isTabScreen) {
                            NavigationBar {
                                tabs.filter { it.visible }.forEach { tab ->
                                    NavigationBarItem(
                                        selected = currentRoute == tab.route,
                                        onClick = {
                                            if (currentRoute != tab.route) {
                                                navController.navigate(tab.route) {
                                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                                    launchSingleTop = true
                                                }
                                            }
                                        },
                                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                                        label = { Text(tab.label) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class TabItem(val label: String, val icon: ImageVector, val route: String, val visible: Boolean = true)
