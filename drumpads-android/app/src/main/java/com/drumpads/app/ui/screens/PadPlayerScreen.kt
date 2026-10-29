package com.drumpads.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drumpads.app.domain.model.Pad
import com.drumpads.app.ui.pads.ActiveScreen
import com.drumpads.app.ui.pads.DrumPadsUiState
import com.drumpads.app.ui.pads.DrumPadsViewModel
import com.drumpads.app.ui.pads.PadBank
import com.drumpads.app.ui.theme.*

@Composable
fun PadPlayerScreen(
    uiState: DrumPadsUiState,
    viewModel: DrumPadsViewModel,
    modifier: Modifier = Modifier
) {
    // Bank A: pads 0..11, Bank B: pads 12..23, Full: 0..23
    val displayedPads = remember(uiState.selectedBank, uiState.pads) {
        when (uiState.selectedBank) {
            PadBank.BANK_A -> uiState.pads.filter { it.index in 0..11 }
            PadBank.BANK_B -> uiState.pads.filter { it.index in 12..23 }
            PadBank.FULL_24 -> uiState.pads
        }
    }

    val columns = if (uiState.selectedBank == PadBank.FULL_24) 4 else 3

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        // 1. Top Control Bar (Matching Image 1)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(ActiveScreen.MAIN) },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(NavyCard)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = uiState.selectedPackName,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "BPM ${uiState.bpm} • 4/4",
                    fontSize = 11.sp,
                    color = AccentCyan
                )
            }

            // Record Button (Glowing red circle)
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (uiState.isRecording) DangerRed else NavyCard)
                    .border(1.dp, if (uiState.isRecording) Color.White else NavyBorder, CircleShape)
                    .clickable { viewModel.toggleRecording() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(if (uiState.isRecording) RoundedCornerShape(3.dp) else CircleShape)
                        .background(if (uiState.isRecording) Color.White else DangerRed)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 2. Secondary Bar (Clips / Pads switch, Bank A / B / 24 switch, FX)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Clips / Pads switch
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(NavyCard)
                    .border(1.dp, NavyBorder, RoundedCornerShape(16.dp))
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (!uiState.isClipsMode) NavyCardElevated else Color.Transparent)
                        .clickable { if (uiState.isClipsMode) viewModel.toggleClipsMode() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Pads",
                        fontSize = 12.sp,
                        fontWeight = if (!uiState.isClipsMode) FontWeight.Bold else FontWeight.Medium,
                        color = if (!uiState.isClipsMode) AccentYellow else TextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (uiState.isClipsMode) NavyCardElevated else Color.Transparent)
                        .clickable { if (!uiState.isClipsMode) viewModel.toggleClipsMode() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Clips",
                        fontSize = 12.sp,
                        fontWeight = if (uiState.isClipsMode) FontWeight.Bold else FontWeight.Medium,
                        color = if (uiState.isClipsMode) AccentYellow else TextSecondary
                    )
                }
            }

            // Bank A / B / 24 Pads selector
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(NavyCard)
                    .border(1.dp, NavyBorder, RoundedCornerShape(16.dp))
                    .padding(2.dp)
            ) {
                listOf(
                    Pair("A", PadBank.BANK_A),
                    Pair("B", PadBank.BANK_B),
                    Pair("24", PadBank.FULL_24)
                ).forEach { (label, bank) ->
                    val isSelected = uiState.selectedBank == bank
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) AccentCyan.copy(alpha = 0.25f) else Color.Transparent)
                            .clickable { viewModel.selectBank(bank) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) AccentCyan else TextSecondary
                        )
                    }
                }
            }

            // Sound FX Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(NavyCard)
                    .border(1.dp, NavyBorder, RoundedCornerShape(16.dp))
                    .clickable { viewModel.navigateTo(ActiveScreen.SETTINGS) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "FX",
                        tint = AccentPink,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "FX", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Main Pad Grid (3x4 or 4x6 depending on bank)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(displayedPads, key = { it.index }) { pad ->
                    val isPressed = uiState.activePads.contains(pad.index)
                    CyberPadCell(
                        pad = pad,
                        isPressed = isPressed,
                        isLightShowEnabled = uiState.isLightShowEnabled,
                        onTouchDown = { viewModel.onPadDown(pad.index) },
                        onTouchUp = { viewModel.onPadUp(pad.index) }
                    )
                }
            }
        }
    }
}

@Composable
fun CyberPadCell(
    pad: Pad,
    isPressed: Boolean,
    isLightShowEnabled: Boolean,
    onTouchDown: () -> Unit,
    onTouchUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val baseColor = remember(pad.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(pad.colorHex))
        } catch (_: Throwable) {
            Color(0xFFFF416C)
        }
    }

    val animatedBgColor by animateColorAsState(
        targetValue = when {
            isPressed -> Color.White
            isLightShowEnabled -> baseColor.copy(alpha = 0.9f)
            else -> NavyCardElevated
        },
        label = "cyberPadBg"
    )

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        label = "cyberPadScale"
    )

    Box(
        modifier = modifier
            .aspectRatio(1.0f)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(animatedBgColor)
            .border(
                width = if (isPressed) 2.5.dp else 1.dp,
                color = if (isPressed) AccentYellow else baseColor.copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp)
            )
            .pointerInput(pad.index) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    onTouchDown()
                    do {
                        val event = awaitPointerEvent()
                    } while (event.changes.any { it.pressed })
                    onTouchUp()
                }
            }
            .padding(10.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Column {
            Text(
                text = "${pad.index + 1}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = if (isPressed) Color.Black else Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = pad.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPressed) Color.Black else Color.White,
                maxLines = 1
            )
        }
    }
}

