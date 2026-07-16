package com.sinus.pinmap.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.sinus.pinmap.data.entity.FieldType

@Composable
fun CreateFieldTemplateDialog(
    onConfirm: (fieldName: String, fieldType: FieldType) -> Unit,
    onDismiss: () -> Unit
) {
    var fieldName by remember { mutableStateOf("") }
    var selectedFieldType by remember { mutableStateOf(FieldType.TEXT) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "添加字段",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = fieldName,
                    onValueChange = { fieldName = it },
                    label = { Text("字段名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "字段类型",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFieldType == FieldType.TEXT,
                        onClick = { selectedFieldType = FieldType.TEXT },
                        label = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
                                Text("文本")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    FilterChip(
                        selected = selectedFieldType == FieldType.IMAGE,
                        onClick = { selectedFieldType = FieldType.IMAGE },
                        label = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(20.dp))
                                Text("图片")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    FilterChip(
                        selected = selectedFieldType == FieldType.NUMBER,
                        onClick = { selectedFieldType = FieldType.NUMBER },
                        label = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(20.dp))
                                Text("数字")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    FilterChip(
                        selected = selectedFieldType == FieldType.VIDEO,
                        onClick = { selectedFieldType = FieldType.VIDEO },
                        label = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                Text("视频")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { onConfirm(fieldName, selectedFieldType) },
                        enabled = fieldName.isNotBlank()
                    ) {
                        Text("添加")
                    }
                }
            }
        }
    }
}
