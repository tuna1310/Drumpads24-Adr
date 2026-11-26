package com.drumpads.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drumpads.app.ui.pads.ActiveScreen
import com.drumpads.app.ui.pads.DrumPadsUiState
import com.drumpads.app.ui.pads.DrumPadsViewModel
import com.drumpads.app.ui.theme.*

@Composable
fun SettingsScreen(
    uiState: DrumPadsUiState,
    viewModel: DrumPadsViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo(ActiveScreen.MAIN) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Text("Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        }

        // Studio Toggles
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(NavyCard)
                    .border(1.dp, NavyBorder, RoundedCornerShape(18.dp))
            ) {
                SettingToggleRow("Show Pad Names on Grid", uiState.showPadLabels) {
                    viewModel.setShowPadLabels(it)
                }
                HorizontalDivider(color = NavyBorder.copy(alpha = 0.5f))
                SettingToggleRow("Pad Light Show", uiState.isLightShowEnabled) {
                    viewModel.setLightShowEnabled(it)
                }
                HorizontalDivider(color = NavyBorder.copy(alpha = 0.5f))
                SettingToggleRow("Haptic Feedback", uiState.isHapticEnabled) {
                    viewModel.setHapticEnabled(it)
                }
                HorizontalDivider(color = NavyBorder.copy(alpha = 0.5f))
                SettingToggleRow("32-Voice DSP Stress Test", uiState.isStressTestActive) {
                    viewModel.setStressTest(it)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Engine & Telemetry Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(NavyCard)
                    .border(1.dp, NavyBorder, RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF38E54D), modifier = Modifier.size(18.dp))
                    Text("All Studio Features Unlocked", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Text(
                    text = "Engine: Real-Time AudioTrack 44.1kHz Stereo • 32-Voice Polyphony + Choke Groups • 10 Live DSP Effects (Lo-Fi, Filter, Pitch, Reverb, Delay 1/16 & 1/8, Distortion, Gate, Wobble) + Real WAV & MP4 Exporter",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
                if (uiState.isStressTestActive) {
                    Text(
                        text = "Stress Test Running: ${uiState.stressSecondsRemaining}s left • XRUNs: ${uiState.xrunCount}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentYellow
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 15.sp, color = TextPrimary)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF00E676),
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = NavyCardElevated
            )
        )
    }
}
