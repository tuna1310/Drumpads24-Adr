package com.drumpads.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF260D45), // Deep purple top tint matching video
                        Color(0xFF140D2A),
                        Color(0xFF0C0E1B),
                        Color(0xFF070810)
                    )
                )
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP SECTION
            Column {
                // 1. Top Bar: Back, Crown, [Clips|Pads], Lock, Settings
                TopNavigationBar(
                    isClipsMode = uiState.isClipsMode,
                    isPadLocked = uiState.isPadLocked,
                    onBackClick = { viewModel.navigateTo(ActiveScreen.MAIN) },
                    onCrownClick = { viewModel.navigateTo(ActiveScreen.PAYWALL) },
                    onModeChange = { isClips -> viewModel.setClipsMode(isClips) },
                    onLockToggle = { viewModel.setPadLocked(!uiState.isPadLocked) },
                    onSettingsClick = { viewModel.navigateTo(ActiveScreen.SETTINGS) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Mode-Specific Sub-bar
                if (uiState.isClipsMode) {
                    ClipsTransportBar(
                        bpm = uiState.bpm,
                        isRecording = uiState.isRecording,
                        onStopClick = { viewModel.stopPlayback() },
                        onRecordClick = { viewModel.toggleRecording() },
                        onBpmDecrease = { viewModel.adjustBpm(-5) },
                        onBpmIncrease = { viewModel.adjustBpm(5) }
                    )
                } else {
                    PadsToolbar(
                        selectedBank = uiState.selectedBank,
                        isRecording = uiState.isRecording,
                        onSoundFxClick = { viewModel.toggleSoundFxTray() },
                        onRecordClick = { viewModel.toggleRecording() },
                        onSceneSelect = { bank -> viewModel.selectBank(bank) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 6 Quick FX Buttons: [Horn] [Elphnt] [Reload] [Scrtch] [Vinyl] [Watch]
                    QuickFxBar(
                        activeFx = uiState.activeQuickFx,
                        onFxClick = { fxName -> viewModel.triggerQuickFx(fxName) }
                    )
                }
            }

            // MIDDLE SECTION: GRID (Clips or Pads)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isClipsMode) {
                    ClipsMatrixView(
                        activeClips = uiState.activeClips,
                        selectedClipIndex = uiState.selectedClipIndex,
                        bpm = uiState.bpm,
                        onClipClick = { index -> viewModel.toggleClip(index) }
                    )
                } else {
                    GlowingPadsGridView(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                }
            }

            // BOTTOM SECTION: Controls & Branding
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (uiState.isClipsMode) {
                    // Clip Action Bar: Edit, Duplicate, Delete
                    ClipActionBar(
                        onEdit = { viewModel.openPadPlayer("Clip Editor") },
                        onDuplicate = { viewModel.duplicateSelectedClip() },
                        onDelete = { viewModel.deleteSelectedClip() }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Audio FX Faders (Volume, Echo 1/16, Echo 1/8, Reverb, Distortion)
                    AudioFadersStrip(
                        volume = uiState.volumeLevel,
                        echo116 = uiState.echo116Level,
                        echo18 = uiState.echo18Level,
                        reverb = uiState.reverbLevel,
                        distortion = uiState.distortionLevel,
                        onFaderChange = { type, level -> viewModel.setFaderLevel(type, level) },
                        onVipClick = { viewModel.navigateTo(ActiveScreen.PAYWALL) }
                    )
                } else if (uiState.isSoundFxOpen) {
                    // Drawer FX Faders when Sound FX is toggled
                    AudioFadersStrip(
                        volume = uiState.volumeLevel,
                        echo116 = uiState.echo116Level,
                        echo18 = uiState.echo18Level,
                        reverb = uiState.reverbLevel,
                        distortion = uiState.distortionLevel,
                        onFaderChange = { type, level -> viewModel.setFaderLevel(type, level) },
                        onVipClick = { viewModel.navigateTo(ActiveScreen.PAYWALL) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Branding: Drum Pads 24
                NeonAppBranding()
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

// -------------------------------------------------------------
// 1. TOP NAVIGATION BAR
// -------------------------------------------------------------
@Composable
private fun TopNavigationBar(
    isClipsMode: Boolean,
    isPadLocked: Boolean,
    onBackClick: () -> Unit,
    onCrownClick: () -> Unit,
    onModeChange: (Boolean) -> Unit,
    onLockToggle: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back Down Chevron
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        // Crown VIP Icon
        IconButton(
            onClick = onCrownClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "VIP Pass",
                tint = Color(0xFFFF2A85), // Vivid magenta/pink crown
                modifier = Modifier.size(24.dp)
            )
        }

        // Center Segmented Toggle: [Clips] [Pads] (Matching Video pill)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF281C48).copy(alpha = 0.85f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                .padding(3.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Clips Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isClipsMode) Color.White else Color.Transparent)
                        .clickable { onModeChange(true) }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Clips",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isClipsMode) Color.Black else Color.White.copy(alpha = 0.7f)
                    )
                }

                // Pads Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (!isClipsMode) Color.White else Color.Transparent)
                        .clickable { onModeChange(false) }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Pads",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (!isClipsMode) Color.Black else Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Lock Toggle Pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (isPadLocked) Color(0xFF6C3CE9) else Color(0xFF322359))
                .clickable { onLockToggle() }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Lock",
                tint = if (isPadLocked) Color.White else Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(16.dp)
            )
        }

        // Gear Icon (Settings)
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// -------------------------------------------------------------
// 2A. CLIPS TRANSPORT BAR (Stop, Record, BPM)
// -------------------------------------------------------------
@Composable
private fun ClipsTransportBar(
    bpm: Int,
    isRecording: Boolean,
    onStopClick: () -> Unit,
    onRecordClick: () -> Unit,
    onBpmDecrease: () -> Unit,
    onBpmIncrease: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Stop Button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { onStopClick() }
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Stop", fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
        }

        // Record Button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { onRecordClick() }
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .border(2.dp, if (isRecording) DangerRed else Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isRecording) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(DangerRed)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Record",
                fontSize = 11.sp,
                color = if (isRecording) DangerRed else Color.White.copy(alpha = 0.75f)
            )
        }

        // BPM Capsule Control: [-] 120 BPM [+]
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF2C1F52))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "—",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .clickable { onBpmDecrease() }
                    .padding(horizontal = 8.dp)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "$bpm",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "BPM",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            Text(
                text = "+",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .clickable { onBpmIncrease() }
                    .padding(horizontal = 8.dp)
            )
        }
    }
}

// -------------------------------------------------------------
// 2B. PADS TOOLBAR (Sound FX, Record, Scene A/B)
// -------------------------------------------------------------
@Composable
private fun PadsToolbar(
    selectedBank: PadBank,
    isRecording: Boolean,
    onSoundFxClick: () -> Unit,
    onRecordClick: () -> Unit,
    onSceneSelect: (PadBank) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sound FX Toggle Button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { onSoundFxClick() }
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Sound FX",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = "Sound FX", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
        }

        // Record Button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { onRecordClick() }
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .border(2.dp, if (isRecording) DangerRed else Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isRecording) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(DangerRed)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Record",
                fontSize = 11.sp,
                color = if (isRecording) DangerRed else Color.White.copy(alpha = 0.8f)
            )
        }

        // Scene A / B Toggle
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF352562))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            ) {
                // Scene A
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (selectedBank == PadBank.BANK_A) Color.White else Color.Transparent)
                        .clickable { onSceneSelect(PadBank.BANK_A) }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "A",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedBank == PadBank.BANK_A) Color.Black else Color.White.copy(alpha = 0.6f)
                    )
                }

                // Scene B
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (selectedBank == PadBank.BANK_B) Color.White else Color.Transparent)
                        .clickable { onSceneSelect(PadBank.BANK_B) }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "B",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedBank == PadBank.BANK_B) Color.Black else Color.White.copy(alpha = 0.6f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = "Scene", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
        }
    }
}

// -------------------------------------------------------------
// 3. QUICK FX BAR (6 Buttons from Video)
// -------------------------------------------------------------
@Composable
private fun QuickFxBar(
    activeFx: String?,
    onFxClick: (String) -> Unit
) {
    val fxList = listOf("Horn", "Elphnt", "Reload", "Scrtch", "Vinyl", "Watch")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1738))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
    ) {
        fxList.forEachIndexed { index, name ->
            val isActive = activeFx == name
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(if (isActive) Color(0xFF6C3CE9) else Color.Transparent)
                    .clickable { onFxClick(name) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isActive) Color.White else Color.White.copy(alpha = 0.85f)
                )
            }
            if (index < fxList.lastIndex) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(Color.White.copy(alpha = 0.12f))
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 4. MAIN GLOWING NEON PADS (Video 2: 3 cols x 4 rows)
// -------------------------------------------------------------
@Composable
private fun GlowingPadsGridView(
    uiState: DrumPadsUiState,
    viewModel: DrumPadsViewModel
) {
    val isSceneB = uiState.selectedBank == PadBank.BANK_B
    val startIndex = if (isSceneB) 12 else 0

    // Exact color mappings matching Drum Pads 24 store listing & app UI
    val padColors = remember(isSceneB) {
        if (!isSceneB) {
            // Scene A: Signature Drum Pads 24 Kit
            // Row 0: 3 Lime Green pads
            // Row 1: 1 Lime Green, 1 Vivid Orange, 1 Neon Magenta/Purple
            // Row 2: 3 Vivid Orange pads (center pad turns radiant white when tapped)
            // Row 3: 3 Electric Sky Blue / Cyan pads
            listOf(
                Color(0xFF38E54D), Color(0xFF38E54D), Color(0xFF38E54D), // Row 0
                Color(0xFF38E54D), Color(0xFFFF7A00), Color(0xFFE040FB), // Row 1
                Color(0xFFFF7A00), Color(0xFFFF7A00), Color(0xFFFF7A00), // Row 2
                Color(0xFF00B0FF), Color(0xFF00B0FF), Color(0xFF00B0FF)  // Row 3
            )
        } else {
            // Scene B: Alternative Electronic Kit
            listOf(
                Color(0xFFFF2E93), Color(0xFFFF2E93), Color(0xFFFF2E93), // Row 0: Hot Pink
                Color(0xFF9D00FF), Color(0xFF9D00FF), Color(0xFF9D00FF), // Row 1: Electric Violet
                Color(0xFFFFD600), Color(0xFFFFD600), Color(0xFFFFD600), // Row 2: Neon Amber
                Color(0xFF00F5D4), Color(0xFF00F5D4), Color(0xFF00F5D4)  // Row 3: Neon Aqua
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (row in 0 until 4) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (col in 0 until 3) {
                    val localIndex = row * 3 + col
                    val absolutePadIndex = startIndex + localIndex
                    val baseColor = padColors.getOrElse(localIndex) { Color(0xFFE67E22) }
                    val isPressed = uiState.activePads.contains(absolutePadIndex)

                    PadCell(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        baseColor = baseColor,
                        isPressed = isPressed,
                        isLightShowEnabled = uiState.isLightShowEnabled,
                        onDown = { viewModel.onPadDown(absolutePadIndex) },
                        onUp = { viewModel.onPadUp(absolutePadIndex) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PadCell(
    modifier: Modifier = Modifier,
    baseColor: Color,
    isPressed: Boolean,
    isLightShowEnabled: Boolean,
    onDown: () -> Unit,
    onUp: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "scale"
    )

    // Authentic hardware pad surface: solid, rich, vivid neon colors (no murky translucency)
    // When pressed, turns pure brilliant solid white just like in the Drum Pads 24 screenshots
    val backgroundBrush = remember(isPressed, baseColor, isLightShowEnabled) {
        if (isPressed) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFFF8F9FA)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(
                        red = (baseColor.red * 0.82f + 0.18f).coerceIn(0f, 1f),
                        green = (baseColor.green * 0.82f + 0.18f).coerceIn(0f, 1f),
                        blue = (baseColor.blue * 0.82f + 0.18f).coerceIn(0f, 1f),
                        alpha = 1.0f
                    ),
                    baseColor,
                    Color(
                        red = (baseColor.red * 0.85f).coerceIn(0f, 1f),
                        green = (baseColor.green * 0.85f).coerceIn(0f, 1f),
                        blue = (baseColor.blue * 0.85f).coerceIn(0f, 1f),
                        alpha = 1.0f
                    )
                )
            )
        }
    }

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundBrush)
            .border(
                width = if (isPressed) 2.5.dp else 1.dp,
                color = if (isPressed) Color.White else Color.White.copy(alpha = 0.22f),
                shape = RoundedCornerShape(14.dp)
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    onDown()
                    do {
                        val event = awaitPointerEvent()
                    } while (event.changes.any { it.pressed })
                    onUp()
                }
            }
    )
}

// -------------------------------------------------------------
// 5. CLIPS MATRIX VIEW (Video 1: 4 cols x 3 rows = 12 clips)
// -------------------------------------------------------------
@Composable
private fun ClipsMatrixView(
    activeClips: Set<Int>,
    selectedClipIndex: Int,
    bpm: Int,
    onClipClick: (Int) -> Unit
) {
    // Continuous playhead sweeping line from top (0f) to bottom (1f)
    val infiniteTransition = rememberInfiniteTransition(label = "playhead")
    val loopDurationMs = (60_000L / bpm) * 2 // 2-bar loop duration
    val playheadProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = loopDurationMs.toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(12) { index ->
            val isPlaying = activeClips.contains(index)
            val isSelected = selectedClipIndex == index
            val isEmptySlot = index >= 8

            ClipTile(
                index = index,
                isPlaying = isPlaying,
                isSelected = isSelected,
                isEmptySlot = isEmptySlot,
                playheadProgress = playheadProgress,
                onClick = { onClipClick(index) }
            )
        }
    }
}

@Composable
private fun ClipTile(
    index: Int,
    isPlaying: Boolean,
    isSelected: Boolean,
    isEmptySlot: Boolean,
    playheadProgress: Float,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isPlaying) Color(0xFF281E60) else Color(0xFF1B1A32)
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = when {
                    isSelected -> Color.White
                    isPlaying -> Color(0xFF8B5CF6)
                    else -> Color.White.copy(alpha = 0.08f)
                },
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(6.dp)
    ) {
        if (isEmptySlot) {
            // Empty slot with '+' icon
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    fontSize = 20.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Light
                )
            }
        } else {
            // MIDI Sequencer dots/bars
            Box(modifier = Modifier.fillMaxSize()) {
                MiniMidiPattern(clipIndex = index)

                // Sweeping playhead line when playing
                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(playheadProgress)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(Color.White)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniMidiPattern(clipIndex: Int) {
    val dotColors = listOf(
        Color(0xFFFFD166), // Yellow
        Color(0xFF06D6A0), // Green
        Color(0xFF118AB2), // Blue
        Color(0xFFEF476F)  // Red/Pink
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        for (r in 0 until 5) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (c in 0 until 5) {
                    val shouldShow = ((clipIndex * 7 + r * 3 + c) % 4) == 0
                    if (shouldShow) {
                        val color = dotColors[(clipIndex + r + c) % dotColors.size]
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                    } else {
                        Spacer(modifier = Modifier.size(3.dp))
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 6. CLIP ACTION BAR (Edit, Duplicate, Delete)
// -------------------------------------------------------------
@Composable
private fun ClipActionBar(
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF382A72))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
    ) {
        // Edit
        Row(
            modifier = Modifier
                .clickable { onEdit() }
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "Edit", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Box(modifier = Modifier.width(1.dp).height(36.dp).background(Color.White.copy(alpha = 0.15f)))

        // Duplicate
        Row(
            modifier = Modifier
                .clickable { onDuplicate() }
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Duplicate",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "Duplicate", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Box(modifier = Modifier.width(1.dp).height(36.dp).background(Color.White.copy(alpha = 0.15f)))

        // Delete
        Row(
            modifier = Modifier
                .clickable { onDelete() }
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "Delete", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

// -------------------------------------------------------------
// 7. AUDIO FX FADERS (Horizontal Capsule Sliders)
// -------------------------------------------------------------
@Composable
private fun AudioFadersStrip(
    volume: Float,
    echo116: Float,
    echo18: Float,
    reverb: Float,
    distortion: Float,
    onFaderChange: (String, Float) -> Unit,
    onVipClick: () -> Unit
) {
    val faders = listOf(
        FaderData("Volume", volume, isVip = false),
        FaderData("Echo 1/16", echo116, isVip = true),
        FaderData("Echo 1/8", echo18, isVip = true),
        FaderData("Reverb", reverb, isVip = true),
        FaderData("Distortion", distortion, isVip = true)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        faders.forEach { fader ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(64.dp)
            ) {
                // Capsule Slider Track
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(88.dp)
                        .clip(RoundedCornerShape(21.dp))
                        .background(Color(0xFF221A3E))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(21.dp))
                        .clickable {
                            if (fader.isVip) {
                                onVipClick()
                            } else {
                                val next = if (fader.value > 0.5f) 0.2f else 0.85f
                                onFaderChange(fader.title, next)
                            }
                        },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    // Filled level
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fader.value)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF8B5CF6), Color(0xFF4C1D95))
                                )
                            )
                    )

                    // Groove center line
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .fillMaxHeight(0.85f)
                            .background(Color.White.copy(alpha = 0.25f))
                    )

                    // VIP Crown badge at top right
                    if (fader.isVip) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF8B5CF6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "VIP",
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = fader.title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
        }
    }
}

private data class FaderData(
    val title: String,
    val value: Float,
    val isVip: Boolean
)

// -------------------------------------------------------------
// 8. NEON BRANDING
// -------------------------------------------------------------
@Composable
private fun NeonAppBranding() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Drum Pads ",
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFFFF2A85) // Vivid neon pink
        )
        Text(
            text = "24",
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF00E5FF) // Vivid electric cyan
        )
    }
}
