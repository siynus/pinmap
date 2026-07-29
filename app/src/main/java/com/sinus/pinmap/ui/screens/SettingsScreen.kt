package com.sinus.pinmap.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.sinus.pinmap.data.database.PinmapDatabase
import com.sinus.pinmap.data.repository.CategoryRepository
import com.sinus.pinmap.data.repository.FieldValueRepository
import com.sinus.pinmap.data.repository.PinRepository
import com.sinus.pinmap.data.repository.FieldTemplateRepository
import com.sinus.pinmap.ui.utils.AuthState
import com.sinus.pinmap.ui.utils.PinExporter
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { PinmapDatabase.getDatabase(context) }
    val pinRepo = remember { PinRepository(database.pinStore()) }
    val categoryRepo = remember { CategoryRepository(database.categoryStore()) }
    val templateRepo = remember { FieldTemplateRepository(database.fieldTemplateStore()) }
    val valueRepo = remember { FieldValueRepository(database.fieldValueStore()) }

    val currentKey = remember { AuthState.getSavedKey() ?: "" }
    var isExporting by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
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
                    scope.launch {
                        try {
                            val cats = categoryRepo.getAllCategories().first()
                            val templates = templateRepo.getAllFieldTemplates().first()
                            val pins = pinRepo.getAllPins().first()
                            val allValues = pins.associate { pin ->
                                pin.id to valueRepo.getFieldValuesByPin(pin.id).first()
                            }
                            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                            val uri = PinExporter.export(context, cats, templates, pins, allValues, "export_$dateStr.pinmap")
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
        ) {
            if (isExporting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text("导出全部标记")
        }
    }
}
