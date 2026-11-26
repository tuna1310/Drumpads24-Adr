package com.drumpads.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drumpads.app.ui.pads.ActiveScreen
import com.drumpads.app.ui.pads.DrumPadsUiState
import com.drumpads.app.ui.pads.DrumPadsViewModel
import com.drumpads.app.ui.theme.*

/**
 * 16-Step Pattern Sequencer Editor matching Google Play "CREATE BEATS" screenshot:
 * - Vivid magenta/purple studio backdrop
 * - < 120 BPM > selector
 * - 24-Pad instrument row (pick which pad to program on the 16-step grid)
 * - Interactive 4x4 (16-step) glowing grid with live sweeping step highlight
 * - 4 horizontal bar indicator dashes + < PATTERN / CLIP 1..12 > switcher + DONE button
 */
@Composable
fun ClipEditorScreen(
    uiState: DrumPadsUiState,
    viewModel: DrumPadsViewModel,
    modifier: Modifier = Modifier
) {
    var selectedPadIdx by remember { mutableStateOf(0) }
    val clipIdx = uiState.selectedClipIndex.coerceIn(0, 11)
    val clip = uiState.clips.getOrNull(clipIdx)
    val currentStep = uiState.currentSequencerStep % 16

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFD500F9),
                        Color(0xFF9C27B0),
                        Color(0xFF4A148C),
                        Color(0xFF1A0836)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: Back, < BPM >, Play/Stop
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo(ActiveScreen.PAD_PLAYER) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                // < 120 bpm >
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconButton(onClick = { viewModel.adjustBpm(-5) }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "-", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${uiState.bpm}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text("bpm", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                    IconButton(onClick = { viewModel.adjustBpm(5) }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "+", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }

                IconButton(onClick = { viewModel.toggleClipPlayback() }) {
                    Icon(
                        imageVector = if (uiState.isClipPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = "Play/Stop",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Pad Selector Carousel (Choose which of the 24 pads you're sequencing!)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "INSTRUMENT PAD: #${selectedPadIdx + 1} ${uiState.pads.getOrNull(selectedPadIdx)?.name ?: ""}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.pads.forEach { pad ->
                        val isSel = pad.index == selectedPadIdx
                        val hasHits = clip?.hits?.any { it.padIndex == pad.index } == true
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) Color.White else Color.Black.copy(alpha = 0.3f))
                                .border(
                                    width = if (hasHits) 2.dp else 1.dp,
                                    color = if (hasHits) AccentYellow else Color.White.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    selectedPadIdx = pad.index
                                    viewModel.onPadDown(pad.index)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = pad.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.Black else Color.White
                            )
                        }
                    }
                }
            }

            // 4x4 (16-STEP) GLOWING SEQUENCER GRID
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (row in 0 until 4) {
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (col in 0 until 4) {
                            val step = row * 4 + col
                            val isActive = clip?.hits?.any { it.padIndex == selectedPadIdx && it.step == step } == true
                            val isNow = uiState.isClipPlaying && currentStep == step

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        when {
                                            isActive -> Color.White.copy(alpha = 0.95f)
                                            isNow -> Color.White.copy(alpha = 0.45f)
                                            else -> Color.White.copy(alpha = 0.22f)
                                        }
                                    )
                                    .border(
                                        width = if (isNow) 3.dp else 1.dp,
                                        color = if (isNow) AccentYellow else Color.White.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable {
                                        viewModel.toggleClipStep(clipIdx, selectedPadIdx, step)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${step + 1}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isActive) Color(0xFF6A0080) else Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            // 4 Horizontal Beat Bar Indicators (matching Play Store screenshot!)
            Row(
                modifier = Modifier.fillMaxWidth(0.85f),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (bar in 0 until 4) {
                    val isCurrentBar = uiState.isClipPlaying && (currentStep / 4 == bar)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isCurrentBar) Color.White else Color.White.copy(alpha = 0.35f))
                    )
                }
            }

            // < PATTERN / CLIP X > Switcher + Clear + DONE
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    IconButton(onClick = { viewModel.selectClip((clipIdx + 11) % 12) }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Prev", tint = Color.White)
                    }
                    Text(
                        text = "PATTERN ${clipIdx + 1} (${clip?.name ?: ""})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    IconButton(onClick = { viewModel.selectClip((clipIdx + 1) % 12) }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = Color.White)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.clearClipPad(clipIdx, selectedPadIdx) },
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
                    ) {
                        Text("CLEAR PAD", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            if (clip != null && !clip.isEmpty && !uiState.activeClips.contains(clipIdx)) {
                                viewModel.toggleClip(clipIdx)
                            }
                            viewModel.navigateTo(ActiveScreen.PAD_PLAYER)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("DONE", color = Color(0xFF6A0080), fontWeight = FontWeight.Black, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 18.dp))
                    }
                }
            }
        }
    }
}
