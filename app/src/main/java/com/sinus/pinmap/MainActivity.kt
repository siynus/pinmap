package com.sinus.pinmap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sinus.pinmap.ui.components.NavigationDrawer
import com.sinus.pinmap.ui.navigation.Screen
import com.sinus.pinmap.ui.navigation.SubPagesNavGraph
import com.sinus.pinmap.ui.screens.CategoryListScreen
import com.sinus.pinmap.ui.screens.MapScreen
import com.sinus.pinmap.ui.screens.PinListScreen
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
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val navRoute = navBackStackEntry?.destination?.route ?: ""
                var selectedTab by remember { mutableStateOf(Screen.Map.mRoute) }

                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                val tabs = remember {
                    listOf(
                        TabItem("地图", Icons.Default.LocationOn, Screen.Map.mRoute),
                        TabItem("标记列表", Icons.AutoMirrored.Filled.List, Screen.PinList.mRoute),
                        TabItem("分类管理", Icons.Default.Edit, Screen.CategoryList.mRoute),
                        TabItem("设置", Icons.Default.Settings, Screen.Settings.mRoute, visible = false),
                        TabItem("离线地图", Icons.Default.Info, Screen.OfflineMap.mRoute, visible = false)
                    )
                }

                val tabRoutes = tabs.filter { it.visible }.map { it.route }
                val isTabScreen = navRoute == "empty"
                val displayRoute = if (isTabScreen) selectedTab else navRoute

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = selectedTab != Screen.Map.mRoute && isTabScreen,
                    drawerContent = {
                        NavigationDrawer(
                            currentRoute = displayRoute,
                            onNavigate = { route ->
                                if (route in tabRoutes) {
                                    scope.launch {
                                        selectedTab = route
                                        drawerState.snapTo(DrawerValue.Closed)
                                        if (!isTabScreen) {
                                            navController.navigate("empty") { popUpTo("empty") { inclusive = true } }
                                        }
                                    }
                                } else {
                                    scope.launch {
                                        drawerState.snapTo(DrawerValue.Closed)
                                        navController.navigate(route) {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                        }
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
                                        when (displayRoute) {
                                            Screen.Map.mRoute -> "地图"
                                            Screen.PinList.mRoute -> "标记列表"
                                            Screen.CategoryList.mRoute -> "分类管理"
                                            Screen.Settings.mRoute -> "设置"
                                            else -> "Pinmap"
                                        }
                                    )
                                },
                                navigationIcon = {
                                    IconButton(onClick = {}) {
                                        Icon(Icons.Default.Menu, contentDescription = "菜单")
                                    }
                                }
                            )
                        }

                        Box(Modifier.weight(1f)) {
                            if (isTabScreen) {
                                Box(
                                    modifier = Modifier
                                        .then(if (selectedTab == Screen.Map.mRoute) Modifier.fillMaxSize() else Modifier.size(0.dp))
                                ) {
                                    MapScreen(
                                        onNavigateToEdit = { pinId ->
                                            navController.navigate(Screen.PinEdit.createRoute(pinId))
                                        },
                                        onNavigateToCreate = { lat, lng ->
                                            navController.navigate(Screen.PinEdit.createRoute(null, lat, lng))
                                        }
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .then(if (selectedTab == Screen.PinList.mRoute) Modifier.fillMaxSize() else Modifier.size(0.dp))
                                ) {
                                    PinListScreen(
                                        onPinClick = { pinId ->
                                            navController.navigate(Screen.PinEdit.createRoute(pinId))
                                        }
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .then(if (selectedTab == Screen.CategoryList.mRoute) Modifier.fillMaxSize() else Modifier.size(0.dp))
                                ) {
                                    CategoryListScreen(
                                        onNavigateToFieldTemplates = { categoryId ->
                                            navController.navigate(Screen.FieldTemplates.createRoute(categoryId))
                                        }
                                    )
                                }
                            } else {
                                SubPagesNavGraph(
                                    navController = navController
                                )
                            }
                        }

                        if (isTabScreen) {
                            NavigationBar {
                                tabs.filter { it.visible }.forEach { tab ->
                                    NavigationBarItem(
                                        selected = selectedTab == tab.route,
                                        onClick = {
                                            if (selectedTab != tab.route) {
                                                selectedTab = tab.route
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
