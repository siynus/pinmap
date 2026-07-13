package com.sinus.pinmap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
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
                        TabItem("标记列表", Icons.Default.List, Screen.PinList.route),
                        TabItem("分类管理", Icons.Default.Edit, Screen.CategoryList.route),
                        TabItem("离线地图", Icons.Default.Info, Screen.OfflineMap.route)
                    )
                }

                val tabRoutes = listOf(Screen.Map.route, Screen.PinList.route, Screen.CategoryList.route, Screen.OfflineMap.route)
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
                    Box(modifier = Modifier.fillMaxSize()) {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            topBar = {
                                if (isTabScreen) {
                                    TopAppBar(
                                        title = { Text("Pinmap") },
                                        navigationIcon = {
                                            IconButton(onClick = {
                                                scope.launch { drawerState.open() }
                                            }) {
                                                Icon(Icons.Default.Menu, contentDescription = "菜单")
                                            }
                                        }
                                    )
                                }
                            },
                            bottomBar = {
                                if (isTabScreen) {
                                    NavigationBar {
                                        tabs.forEach { tab ->
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
                        ) { paddingValues ->
                            PinmapNavGraph(
                                navController = navController,
                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                modifier = Modifier.padding(paddingValues)
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class TabItem(val label: String, val icon: ImageVector, val route: String)
