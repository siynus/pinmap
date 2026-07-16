package com.sinus.pinmap.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.amap.api.maps.offlinemap.OfflineMapCity
import com.amap.api.maps.offlinemap.OfflineMapStatus
import com.sinus.pinmap.ui.utils.OfflineMapManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineMapScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val offlineManager = remember { OfflineMapManager(context) }

    var cities by remember { mutableStateOf<List<OfflineMapCity>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    val states = offlineManager.mDownloadStates // read from observable state map

    LaunchedEffect(Unit) {
        cities = offlineManager.getAllCities()
    }

    val filteredCities = remember(cities, searchQuery) {
        if (searchQuery.isBlank()) cities
        else cities.filter { it.city.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("离线地图") }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("搜索城市") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    { IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, contentDescription = "清除") } }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent
                )
            )

            if (filteredCities.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (searchQuery.isNotBlank()) "未找到匹配城市" else "暂无可用城市", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredCities) { city ->
                        val pair = states[city.city]
                        val state = pair?.first ?: city.state
                        val progress = pair?.second ?: city.getcompleteCode()
                        val downloaded = state == OfflineMapStatus.SUCCESS
                        val downloading = state == OfflineMapStatus.LOADING || state == OfflineMapStatus.UNZIP || state == OfflineMapStatus.WAITING
                        val paused = state == OfflineMapStatus.PAUSE || state == OfflineMapStatus.STOP

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = city.city, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = when {
                                            downloaded -> "已完成"
                                            downloading -> "下载中 $progress%"
                                            paused -> "已暂停 $progress%"
                                            else -> "${city.size / (1024 * 1024)}MB"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (downloading || paused) {
                                        LinearProgressIndicator(
                                            progress = { progress / 100f },
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                        )
                                    }
                                }
                                if (downloaded) {
                                    TextButton(onClick = { scope.launch { offlineManager.removeByCityName(city.city) } }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                                        Text("删除")
                                    }
                                } else if (downloading) {
                                    TextButton(onClick = { offlineManager.pauseByCityName(city.city) }) { Text("暂停") }
                                } else {
                                    TextButton(onClick = { offlineManager.downloadByCityName(city.city) }) { Text("下载") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
