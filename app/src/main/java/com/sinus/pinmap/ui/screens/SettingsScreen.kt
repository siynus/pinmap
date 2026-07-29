package com.sinus.pinmap.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sinus.pinmap.data.database.PinmapDatabase
import com.sinus.pinmap.ui.utils.AuthState
import com.sinus.pinmap.ui.utils.PinExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { PinmapDatabase.getDatabase(context) }

    val currentKey = remember { AuthState.getSavedKey() ?: "" }
    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableStateOf(0f) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("API Key 设置", style = MaterialTheme.typography.titleLarge)
            Text(
                "当前 API Key 在 Application 初始化时设置，暂不支持在线修改。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box {
                OutlinedTextField(
                    value = currentKey,
                    onValueChange = {},
                    label = { Text("API Key") },
                    readOnly = true,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable {
                            Toast.makeText(context, "暂不可用", Toast.LENGTH_SHORT).show()
                        }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text("数据管理", style = MaterialTheme.typography.titleLarge)

            Button(
                onClick = {
                    if (!isExporting) {
                        isExporting = true
                        exportProgress = 0f
                        scope.launch {
                            try {
                                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                val uri = withContext(Dispatchers.IO) {
                                    PinExporter.exportAll(context, database,
                                        onProgress = { done, total -> exportProgress = done.toFloat() / total },
                                        fileName = "export_$dateStr.pinmap"
                                    )
                                }
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "application/octet-stream"
                                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(android.content.Intent.createChooser(intent, "导出全部标记"))
                            } catch (_: Exception) { }
                            isExporting = false
                        }
                    }
                },
                enabled = !isExporting,
                modifier = Modifier.fillMaxWidth()
            ) { Text("导出全部标记") }
        }

        if (isExporting) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        "正在导出... (${(exportProgress * 100).toInt()}%)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
