package com.sinus.pinmap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.visible
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
                var selectedTabIndex by remember { mutableIntStateOf(TAB_MAP) }

                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                val tabs = remember {
                    listOf(
                        TabItem("地图", Icons.Default.LocationOn, Screen.Map.mRoute),
                        TabItem("标记列表", Icons.AutoMirrored.Filled.List, Screen.PinList.mRoute),
                        TabItem("分类管理", Icons.Default.Edit, Screen.CategoryList.mRoute),
                        TabItem("设置", Icons.Default.Settings, Screen.Settings.mRoute, visible = true),
                        TabItem("离线地图", Icons.Default.Info, Screen.OfflineMap.mRoute, visible = false)
                    )
                }

                val visibleTabs = tabs.filter { it.visible }
                val tabRoutes = visibleTabs.map { it.route }
                val isTabScreen = navRoute == "empty"

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = selectedTabIndex != TAB_MAP && isTabScreen,
                    drawerContent = {
                        NavigationDrawer(
                            currentRoute = visibleTabs.getOrNull(selectedTabIndex)?.route ?: navRoute,
                            onNavigate = { route ->
                                val tabIndex = visibleTabs.indexOfFirst { it.route == route }
                                if (tabIndex >= 0) {
                                    scope.launch {
                                        selectedTabIndex = tabIndex
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
                                        when (selectedTabIndex) {
                                            TAB_MAP -> "地图"
                                            TAB_PIN_LIST -> "标记列表"
                                            TAB_CATEGORY_LIST -> "分类管理"
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
                            MapScreen(
                                modifier = Modifier.fillMaxSize().visible(selectedTabIndex == TAB_MAP),
                                onNavigateToEdit = { pinId ->
                                    navController.navigate(Screen.PinEdit.createRoute(pinId))
                                },
                                onNavigateToCreate = { lat, lng ->
                                    navController.navigate(Screen.PinEdit.createRoute(null, lat, lng))
                                }
                            )
                            PinListScreen(
                                modifier = Modifier.fillMaxSize().visible(selectedTabIndex == TAB_PIN_LIST),
                                onPinClick = { pinId ->
                                    navController.navigate(Screen.PinEdit.createRoute(pinId))
                                }
                            )
                            CategoryListScreen(
                                modifier = Modifier.fillMaxSize().visible(selectedTabIndex == TAB_CATEGORY_LIST),
                                onNavigateToFieldTemplates = { categoryId ->
                                    navController.navigate(Screen.FieldTemplates.createRoute(categoryId))
                                }
                            )

                            if (!isTabScreen) {
                                SubPagesNavGraph(
                                    navController = navController
                                )
                            }
                        }

                        if (isTabScreen) {
                            NavigationBar {
                                visibleTabs.forEachIndexed { index, tab ->
                                        NavigationBarItem(
                                        selected = selectedTabIndex == index,
                                        onClick = {
                                            if (selectedTabIndex != index) {
                                                selectedTabIndex = index
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

private const val TAB_MAP = 0
private const val TAB_PIN_LIST = 1
private const val TAB_CATEGORY_LIST = 2
