package com.sinus.pinmap.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sinus.pinmap.ui.navigation.Screen

/**
 * 导航抽屉
 */
@Composable
fun NavigationDrawer(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(modifier = modifier.width(280.dp)) {
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Pinmap",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Divider()

        NavigationDrawerItem(
            label = { Text("地图") },
            selected = currentRoute == Screen.Map.route,
            onClick = { onNavigate(Screen.Map.route) }
        )

        NavigationDrawerItem(
            label = { Text("标记列表") },
            selected = currentRoute == Screen.PinList.route,
            onClick = { onNavigate(Screen.PinList.route) }
        )

        NavigationDrawerItem(
            label = { Text("分类管理") },
            selected = currentRoute == Screen.CategoryList.route,
            onClick = { onNavigate(Screen.CategoryList.route) }
        )

        NavigationDrawerItem(
            label = { Text("离线地图") },
            selected = currentRoute == Screen.OfflineMap.route,
            onClick = { onNavigate(Screen.OfflineMap.route) }
        )
    }
}