package com.sinus.pinmap.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.sinus.pinmap.data.entity.FieldTemplate
import com.sinus.pinmap.data.entity.FieldType

/**
 * 字段值编辑组件
 */
@OptIn(ExperimentalMaterial3Api::class)

/**
 * 字段值编辑组件
 */
@Composable
fun FieldValueEditor(
    fieldTemplate: FieldTemplate,
    value: String?,
    onValueChange: (String?) -> Unit,
    hasImagePermission: Boolean = false,
    onRequestImagePermission: () -> Unit = {},
    onSelectImage: () -> Unit = {},
    onImageClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    when (fieldTemplate.fieldType) {
        FieldType.TEXT -> {
            OutlinedTextField(
                value = value ?: "",
                onValueChange = onValueChange,
                label = { Text(fieldTemplate.fieldName) },
                modifier = modifier.fillMaxWidth(),
                minLines = 3
            )
        }

        FieldType.NUMBER -> {
            OutlinedTextField(
                value = value ?: "",
                onValueChange = onValueChange,
                label = { Text(fieldTemplate.fieldName) },
                modifier = modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                )
            )
        }

        FieldType.IMAGE -> {
            Card(
                modifier = modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = fieldTemplate.fieldName,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (value != null) {
                        // 显示图片预览
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .clickable { onImageClick() }
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(value),
                                contentDescription = fieldTemplate.fieldName,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 200.dp, max = 400.dp),
                                contentScale = ContentScale.Fit
                            )

                            // 删除按钮
                            Surface(
                                color = MaterialTheme.colorScheme.error,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(32.dp)
                                    .clickable { 
                                        onValueChange(null) 
                                    }
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "删除图片",
                                    tint = MaterialTheme.colorScheme.onError,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        }
                    } else {
                        // 显示上传按钮
                        OutlinedButton(
                            onClick = {
                                if (hasImagePermission) {
                                    onSelectImage()
                                } else {
                                    onRequestImagePermission()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("选择图片")
                        }
                    }
                }
            }
        }

        FieldType.DATE -> {
            OutlinedTextField(
                value = value ?: "",
                onValueChange = onValueChange,
                label = { Text(fieldTemplate.fieldName) },
                modifier = modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("YYYY-MM-DD") }
            )
        }

        FieldType.SINGLE_CHOICE -> {
            var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = modifier
            ) {
                OutlinedTextField(
                    value = value ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(fieldTemplate.fieldName) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    // TODO: 解析 options JSON 并显示选项
                    DropdownMenuItem(
                        text = { Text("选项1") },
                        onClick = {
                            onValueChange("选项1")
                            expanded = false
                        }
                    )
                }
            }
        }

        FieldType.MULTI_CHOICE -> {
            // TODO: 实现多选功能
            OutlinedTextField(
                value = value ?: "",
                onValueChange = onValueChange,
                label = { Text(fieldTemplate.fieldName) },
                modifier = modifier.fillMaxWidth(),
                readOnly = true,
                placeholder = { Text("多选功能开发中") }
            )
        }
    }
}
