package com.sinus.pinmap.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sinus.pinmap.ui.utils.AuthState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val currentKey = remember { AuthState.getSavedKey() ?: "" }
    var keyInput by remember { mutableStateOf(currentKey) }
    var showRestartDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("API Key 设置", style = MaterialTheme.typography.titleLarge)
        Text(
            "输入高德地图 API Key，点击保存后手动重启应用生效。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = keyInput,
            onValueChange = { keyInput = it },
            label = { Text("API Key") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (keyInput.isNotBlank() && keyInput != currentKey) {
                    AuthState.saveKey(keyInput)
                    showRestartDialog = true
                }
            },
            enabled = keyInput.isNotBlank() && keyInput != currentKey,
            modifier = Modifier.fillMaxWidth()
        ) { Text("保存") }

        if (showRestartDialog) {
            AlertDialog(
                onDismissRequest = { showRestartDialog = false },
                title = { Text("已保存") },
                text = { Text("API Key 已保存，请手动重启应用使新 Key 生效。") },
                confirmButton = {
                    Button(onClick = { showRestartDialog = false }) { Text("确定") }
                }
            )
        }
    }
}
