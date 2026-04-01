package com.sinus.pinmap.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

/**
 * 标记列表页面
 */
@Composable
fun PinListScreen(
    onPinClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // TODO: Phase 5: 实现标记列表页面
    // 包括：
    // - 显示所有标记
    // - 搜索功能
    // - 筛选功能（按分类）
    // - 点击标记跳转到编辑页面
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Text("标记列表页面 - 待实现")
    }
}