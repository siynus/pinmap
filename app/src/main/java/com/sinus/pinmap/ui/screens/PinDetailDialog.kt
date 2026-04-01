package com.sinus.pinmap.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.sinus.pinmap.data.entity.Pin

/**
 * 标记详情弹窗
 */
@Composable
fun PinDetailDialog(
    pin: Pin,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit
) {
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
                    text = pin.title,
                    style = MaterialTheme.typography.headlineSmall
                )

                if (pin.description != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = pin.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { onDelete(pin.id) }) {
                        Text("删除")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(onClick = { onEdit(pin.id) }) {
                        Text("编辑")
                    }
                }
            }
        }
    }
}