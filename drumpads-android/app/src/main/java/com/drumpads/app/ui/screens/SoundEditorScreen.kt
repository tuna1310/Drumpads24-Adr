package com.drumpads.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.drumpads.app.ui.pads.ActiveScreen
import com.drumpads.app.ui.pads.DrumPadsUiState
import com.drumpads.app.ui.pads.DrumPadsViewModel
import com.drumpads.app.ui.theme.*
import kotlin.math.roundToInt

/**
 * Real PCM Waveform Audio Editor matching Google Play "EDIT SOUNDS" screenshot:
 * - Yellow top bar (Back, Edit sound, Confirm checkmark)
 * - Sound name text field
 * - Real PCM waveform visualizer with cyan S (Start) & E (End) crop overlay & timestamps
 * - PLAY/STOP, CROP, RESET actions
 * - Fade in & Fade out controls
 * - Large Red Mic Record button capturing real audio via Android AudioRecord!
 */
@Composable
fun SoundEditorScreen(
    uiState: DrumPadsUiState,
    viewModel: DrumPadsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val padIdx = uiState.editingPadIndex.coerceIn(0, (uiState.pads.size - 1).coerceAtLeast(0))
    val pad = uiState.pads.getOrNull(padIdx)

    var soundName by remember(padIdx, pad?.name) { mutableStateOf(pad?.name ?: "Sample") }
    var trimStart by remember(padIdx) { mutableStateOf(0f) }
    var trimEnd by remember(padIdx) { mutableStateOf(1f) }
    var fadeIn by remember(padIdx) { mutableStateOf(0f) }
    var fadeOut by remember(padIdx) { mutableStateOf(0f) }

    val micPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.toggleMicSampleRecording(padIdx)
        } else {
            viewModel.showStatus("Microphone permission denied")
        }
    }

    val totalSec = uiState.editorSampleDurationSec
    val startSec = totalSec * trimStart
    val endSec = totalSec * trimEnd
    val croppedSec = (endSec - startSec).coerceAtLeast(0.01f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121218))
    ) {
        // Signature Yellow Header matching Play Store "EDIT SOUNDS"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFB703))
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { viewModel.navigateTo(ActiveScreen.PAD_EDITOR) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
            }
            Text(
                text = "Edit sound (Pad #${padIdx + 1})",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black
            )
            IconButton(onClick = {
                viewModel.applyCropAndFadeToPad(padIdx, soundName, trimStart, trimEnd, fadeIn, fadeOut)
                viewModel.navigateTo(ActiveScreen.PAD_EDITOR)
            }) {
                Icon(Icons.Default.Check, contentDescription = "Save", tint = Color.Black)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sound Name Input
            OutlinedTextField(
                value = soundName,
                onValueChange = { soundName = it },
                label = { Text("Sound name", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.GraphicEq, contentDescription = null, tint = AccentCyan) },
                trailingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = TextSecondary) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = NavyBorder
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // REAL PCM WAVEFORM VIEW WITH S / E CROP REGION
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1E1E26))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(145.dp)
                ) {
                    val peaks = uiState.editorWaveformPeaks
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val midY = h / 2f

                        // Dim excluded regions outside [trimStart, trimEnd]
                        drawRect(Color.Black.copy(alpha = 0.55f), Offset(0f, 0f), Size(w * trimStart, h))
                        drawRect(Color.Black.copy(alpha = 0.55f), Offset(w * trimEnd, 0f), Size(w * (1f - trimEnd), h))

                        // Center baseline
                        drawLine(Color.White.copy(alpha = 0.15f), Offset(0f, midY), Offset(w, midY), strokeWidth = 1f)

                        // Draw REAL waveform bars from PCM amplitudes!
                        if (peaks.isNotEmpty()) {
                            val barW = w / peaks.size.toFloat()
                            peaks.forEachIndexed { idx, amp ->
                                val normX = idx.toFloat() / peaks.size.toFloat()
                                val inRegion = normX in trimStart..trimEnd
                                val barH = (amp * h * 0.85f).coerceAtLeast(4f)
                                drawRect(
                                    color = if (inRegion) Color.White else Color.Gray.copy(alpha = 0.45f),
                                    topLeft = Offset(idx * barW + 1f, midY - barH / 2f),
                                    size = Size((barW - 2f).coerceAtLeast(2f), barH)
                                )
                            }
                        }

                        // Cyan S & E vertical crop boundary lines
                        val xStart = w * trimStart
                        val xEnd = w * trimEnd
                        drawLine(Color(0xFF00B0FF), Offset(xStart, 0f), Offset(xStart, h), strokeWidth = 4f)
                        drawLine(Color(0xFF00B0FF), Offset(xEnd, 0f), Offset(xEnd, h), strokeWidth = 4f)
                    }

                    // S / E Badges
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF00B0FF))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) { Text("S", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp) }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF00B0FF))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) { Text("E", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp) }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Timestamps row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(String.format("%.2fs", startSec), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00B0FF))
                    Text(String.format("Selection: %.2fs", croppedSec), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(String.format("%.2fs", endSec), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00B0FF))
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Interactive Range Sliders for precise S / E crop
                Text("Start (S)", fontSize = 11.sp, color = TextSecondary)
                Slider(
                    value = trimStart,
                    onValueChange = { trimStart = it.coerceAtMost(trimEnd - 0.05f) },
                    valueRange = 0f..0.95f,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF00B0FF), activeTrackColor = Color(0xFF00B0FF))
                )
                Text("End (E)", fontSize = 11.sp, color = TextSecondary)
                Slider(
                    value = trimEnd,
                    onValueChange = { trimEnd = it.coerceAtLeast(trimStart + 0.05f) },
                    valueRange = 0.05f..1f,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF00B0FF), activeTrackColor = Color(0xFF00B0FF))
                )
            }

            // Action Buttons: PLAY | CROP | RESET
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.onPadDown(padIdx) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B2B38)),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("PLAY ▶", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        viewModel.applyCropAndFadeToPad(padIdx, soundName, trimStart, trimEnd, fadeIn, fadeOut)
                        trimStart = 0f
                        trimEnd = 1f
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF)),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("CROP", color = Color.White, fontWeight = FontWeight.Black)
                }
                Button(
                    onClick = {
                        viewModel.resetPadSample(padIdx)
                        trimStart = 0f
                        trimEnd = 1f
                        fadeIn = 0f
                        fadeOut = 0f
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B2B38)),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("RESET", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            // Fade In / Fade Out Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(NavyCard)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Fade in: ${(fadeIn * 100).roundToInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentCyan)
                    Slider(
                        value = fadeIn,
                        onValueChange = { fadeIn = it },
                        valueRange = 0f..0.45f,
                        colors = SliderDefaults.colors(thumbColor = AccentCyan, activeTrackColor = AccentCyan)
                    )
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Fade out: ${(fadeOut * 100).roundToInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentYellow)
                    Slider(
                        value = fadeOut,
                        onValueChange = { fadeOut = it },
                        valueRange = 0f..0.45f,
                        colors = SliderDefaults.colors(thumbColor = AccentYellow, activeTrackColor = AccentYellow)
                    )
                }
            }

            // Large Red Mic Record Button (captures real audio via Android AudioRecord!)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (uiState.isMicRecordingSample) "🔴 Recording from Mic... Tap to Stop!" else "Tap to record custom sample from Microphone",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (uiState.isMicRecordingSample) DangerRed else TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(if (uiState.isMicRecordingSample) Color(0xFFFF1744) else Color(0xFFD50000))
                        .clickable {
                            if (uiState.isMicRecordingSample) {
                                viewModel.toggleMicSampleRecording(padIdx)
                            } else {
                                val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) {
                                    viewModel.toggleMicSampleRecording(padIdx)
                                } else {
                                    micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (uiState.isMicRecordingSample) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Mic Record",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}
