package com.sinus.pinmap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sinus.pinmap.data.database.PinmapDatabase
import com.sinus.pinmap.data.repository.*
import com.sinus.pinmap.ui.components.NavigationDrawer
import com.sinus.pinmap.ui.navigation.PinmapNavGraph
import com.sinus.pinmap.ui.theme.PinmapTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 初始化数据库
        val database = PinmapDatabase.getDatabase(this)
        val pinRepository = PinRepository(database.pinDao())
        val categoryRepository = CategoryRepository(database.categoryDao())
        val customFieldRepository = CustomFieldRepository(database.customFieldDao())
        val attachmentRepository = AttachmentRepository(database.attachmentDao())
        val offlineMapRepository = OfflineMapRepository(database.offlineMapDao())

        setContent {
            PinmapTheme {
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route ?: ""

                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = false,
                    drawerContent = {
                        NavigationDrawer(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                // 立即关闭抽屉并导航，无动画延迟
                                scope.launch {
                                    drawerState.snapTo(DrawerValue.Closed)
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                    },
                    scrimColor = androidx.compose.ui.graphics.Color.Transparent
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            TopAppBar(
                                title = { Text("Pinmap") },
                                navigationIcon = {
                                    IconButton(onClick = {
                                        scope.launch {
                                            drawerState.open()
                                        }
                                    }) {
                                        Icon(Icons.Default.Menu, contentDescription = "菜单")
                                    }
                                }
                            )
                        }
                    ) { paddingValues ->
                        PinmapNavGraph(navController = navController)
                    }
                }
            }
        }
    }
}