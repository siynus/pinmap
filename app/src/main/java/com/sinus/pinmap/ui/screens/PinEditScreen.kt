package com.sinus.pinmap.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sinus.pinmap.data.entity.Pin

/**
 * 标记编辑页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinEditScreen(
    pinId: Long,
    existingPin: Pin? = null,
    onSave: (title: String, description: String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf(existingPin?.title ?: "") }
    var description by remember { mutableStateOf(existingPin?.description ?: "") }

    // 当 existingPin 变化时，更新状态
    LaunchedEffect(existingPin) {
        if (existingPin != null && pinId > 0) {
            title = existingPin.title
            description = existingPin.description ?: ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (pinId > 0) "编辑标记" else "新建标记") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Text("取消")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { onSave(title, description) },
                        enabled = title.isNotBlank()
                    ) {
                        Text("保存")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("标题") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("描述") },
                modifier = Modifier.fillMaxSize(),
                minLines = 5
            )

            // TODO: Phase 2: 添加图片、音频附件功能
            // TODO: Phase 3: 添加自定义字段功能
        }
    }
}