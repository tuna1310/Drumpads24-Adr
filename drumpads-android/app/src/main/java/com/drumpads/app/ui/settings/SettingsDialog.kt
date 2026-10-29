package com.drumpads.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.drumpads.app.ui.pads.DrumPadsUiState

@Composable
fun SettingsDialog(
    uiState: DrumPadsUiState,
    onHapticChanged: (Boolean) -> Unit,
    onStressTestChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Cài đặt & Debug Audio") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Haptic feedback toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Phản hồi rung (Haptic)", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "Rung nhẹ khi chạm pad",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.isHapticEnabled,
                        onCheckedChange = onHapticChanged
                    )
                }

                Divider()

                // Stress test toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Chế độ Stress Test (32 Voice)", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "Phát liên tục 32 voice trong 60s để đo xrun",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (uiState.isStressTestActive) {
                            Text(
                                text = "Đang chạy: còn ${uiState.stressSecondsRemaining}s | XRuns: ${uiState.xrunCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Switch(
                        checked = uiState.isStressTestActive,
                        onCheckedChange = onStressTestChanged
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng")
            }
        }
    )
}
