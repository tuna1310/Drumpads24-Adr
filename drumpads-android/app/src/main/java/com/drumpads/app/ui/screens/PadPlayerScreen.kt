package com.drumpads.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drumpads.app.core.audio.DspEffectParams
import com.drumpads.app.domain.model.ClipSlot
import com.drumpads.app.domain.model.Pad
import com.drumpads.app.domain.model.PadPlayMode
import com.drumpads.app.domain.model.RecordingMode
import com.drumpads.app.ui.pads.ActiveScreen
import com.drumpads.app.ui.pads.DrumPadsUiState
import com.drumpads.app.ui.pads.DrumPadsViewModel
import com.drumpads.app.ui.pads.PadBank
import com.drumpads.app.ui.pads.TutorialSessionState
import com.drumpads.app.ui.theme.*
import kotlin.math.roundToInt

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
                        Color(0xFF260D45),
                        Color(0xFF140D2A),
                        Color(0xFF0C0E1B),
                        Color(0xFF070810)
                    )
                )
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP SECTION
            Column {
                // 1. Top Navigation Bar: Back, Edit Pads, [Clips|Pads], Lock, Settings
                TopNavigationBar(
                    packName = uiState.selectedPackName,
                    isClipsMode = uiState.isClipsMode,
                    isPadLocked = uiState.isPadLocked,
                    onBackClick = { viewModel.navigateTo(ActiveScreen.MAIN) },
                    onEditorClick = { viewModel.openPadEditor(0) },
                    onModeChange = { isClips -> viewModel.setClipsMode(isClips) },
                    onLockToggle = { viewModel.setPadLocked(!uiState.isPadLocked) },
                    onSettingsClick = { viewModel.navigateTo(ActiveScreen.SETTINGS) }
                )

                // Active Tutorial Banner (Beat Maker Crew Lightshow Practice)
                val tutorial = uiState.activeTutorial
                if (tutorial != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    TutorialGuideBanner(
                        tutorial = tutorial,
                        onExit = { viewModel.exitTutorial() }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 2. Mode-Specific Sub-bar
                if (uiState.isClipsMode) {
                    ClipsTransportBar(
                        bpm = uiState.bpm,
                        isRecording = uiState.isRecording,
                        recordingSeconds = uiState.recordingDurationSeconds,
                        recordingMode = uiState.recordingMode,
                        isMetronomeOn = uiState.isMetronomeEnabled,
                        onStopClick = { viewModel.stopPlayback() },
                        onRecordClick = { viewModel.toggleRecording() },
                        onToggleRecMode = {
                            val next = if (uiState.recordingMode == RecordingMode.FREE) RecordingMode.LOOP else RecordingMode.FREE
                            viewModel.setRecordingMode(next)
                        },
                        onMetronomeClick = { viewModel.toggleMetronome() },
                        onBpmDecrease = { viewModel.adjustBpm(-2) },
                        onBpmIncrease = { viewModel.adjustBpm(2) }
                    )
                } else {
                    PadsToolbar(
                        selectedBank = uiState.selectedBank,
                        isRecording = uiState.isRecording,
                        recordingSeconds = uiState.recordingDurationSeconds,
                        recordingMode = uiState.recordingMode,
                        isSoundFxOpen = uiState.isSoundFxOpen,
                        isHoldMode = uiState.isHoldMode,
                        onSoundFxClick = { viewModel.toggleSoundFxTray() },
                        onRecordClick = { viewModel.toggleRecording() },
                        onToggleRecMode = {
                            val next = if (uiState.recordingMode == RecordingMode.FREE) RecordingMode.LOOP else RecordingMode.FREE
                            viewModel.setRecordingMode(next)
                        },
                        onHoldToggle = { viewModel.toggleHoldMode() },
                        onSceneSelect = { bank -> viewModel.selectBank(bank) }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

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
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isClipsMode) {
                    ClipsMatrixView(
                        clips = uiState.clips,
                        activeClips = uiState.activeClips,
                        selectedClipIndex = uiState.selectedClipIndex,
                        currentStep = uiState.currentSequencerStep,
                        isRecording = uiState.isRecording,
                        onClipClick = { index -> viewModel.toggleClip(index) }
                    )
                } else {
                    GlowingPadsGridView(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                }
            }

            // BOTTOM SECTION: Controls, Full DSP FX Tray & Branding
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (uiState.isClipsMode) {
                    ClipActionBar(
                        onEdit = { viewModel.openClipEditor(uiState.selectedClipIndex) },
                        onDuplicate = { viewModel.duplicateSelectedClip() },
                        onDelete = { viewModel.deleteSelectedClip() },
                        onToggleFx = { viewModel.toggleSoundFxTray() },
                        isFxOpen = uiState.isSoundFxOpen
                    )
                    if (uiState.isSoundFxOpen) {
                        Spacer(modifier = Modifier.height(8.dp))
                        FullAudioFxDrawer(
                            dsp = uiState.dspParams,
                            peakLevel = uiState.livePeakLevel,
                            onFaderChange = { type, level -> viewModel.setFaderLevel(type, level) },
                            onFillClick = { viewModel.triggerFill() },
                            onCleanClick = { viewModel.triggerClean() },
                            onResetClick = { viewModel.resetAllFx() }
                        )
                    }
                } else if (uiState.isSoundFxOpen) {
                    FullAudioFxDrawer(
                        dsp = uiState.dspParams,
                        peakLevel = uiState.livePeakLevel,
                        onFaderChange = { type, level -> viewModel.setFaderLevel(type, level) },
                        onFillClick = { viewModel.triggerFill() },
                        onCleanClick = { viewModel.triggerClean() },
                        onResetClick = { viewModel.resetAllFx() }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${uiState.selectedPackName} • ${uiState.activeGenre.label} • ${uiState.bpm} BPM",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    NeonAppBranding()
                }
                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }
}

@Composable
private fun TutorialGuideBanner(
    tutorial: TutorialSessionState,
    onExit: () -> Unit
) {
    val currentLabel = tutorial.steps.getOrNull(tutorial.currentStepIndex)?.label ?: "Done!"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1F2A56))
            .border(1.dp, AccentCyan, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.School, contentDescription = null, tint = AccentYellow, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (tutorial.isCompleted) "Lesson Complete! (${tutorial.accuracyPercent}%)" else "Step ${tutorial.currentStepIndex + 1}/${tutorial.steps.size}: Tap $currentLabel",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = tutorial.title,
                fontSize = 10.sp,
                color = AccentCyan
            )
        }
        Text(
            text = "EXIT",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = AccentPink,
            modifier = Modifier
                .clickable { onExit() }
                .padding(horizontal = 6.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun TopNavigationBar(
    packName: String,
    isClipsMode: Boolean,
    isPadLocked: Boolean,
    onBackClick: () -> Unit,
    onEditorClick: () -> Unit,
    onModeChange: (Boolean) -> Unit,
    onLockToggle: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(26.dp))
        }

        // Pad Editor Button (3x3 Grid icon matching Google Play Store reference!)
        IconButton(onClick = onEditorClick, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Default.GridView, contentDescription = "Pad Editor", tint = AccentYellow, modifier = Modifier.size(22.dp))
        }

        // Segmented Toggle: [Clips] [Pads]
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF281C48).copy(alpha = 0.85f))
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
                .padding(3.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isClipsMode) Color.White else Color.Transparent)
                        .clickable { onModeChange(true) }
                        .padding(horizontal = 14.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Clips",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isClipsMode) Color.Black else Color.White.copy(alpha = 0.7f)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (!isClipsMode) Color.White else Color.Transparent)
                        .clickable { onModeChange(false) }
                        .padding(horizontal = 14.dp, vertical = 5.dp),
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

        // Lock Toggle
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (isPadLocked) Color(0xFF6C3CE9) else Color(0xFF322359))
                .clickable { onLockToggle() }
                .padding(horizontal = 9.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPadLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = "Lock",
                tint = if (isPadLocked) Color.White else Color.White.copy(alpha = 0.45f),
                modifier = Modifier.size(16.dp)
            )
        }

        IconButton(onClick = onSettingsClick, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun ClipsTransportBar(
    bpm: Int,
    isRecording: Boolean,
    recordingSeconds: Int,
    recordingMode: RecordingMode,
    isMetronomeOn: Boolean,
    onStopClick: () -> Unit,
    onRecordClick: () -> Unit,
    onToggleRecMode: () -> Unit,
    onMetronomeClick: () -> Unit,
    onBpmDecrease: () -> Unit,
    onBpmIncrease: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Stop
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onStopClick() }) {
            Box(modifier = Modifier.size(20.dp).clip(RoundedCornerShape(4.dp)).background(Color.White))
            Spacer(modifier = Modifier.height(3.dp))
            Text(text = "Stop", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
        }

        // Record + Timer + Mode chip
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onRecordClick() }) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .border(2.dp, if (isRecording) DangerRed else Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isRecording) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(DangerRed))
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isRecording) String.format("%02d:%02d", recordingSeconds / 60, recordingSeconds % 60) else "Record",
                    fontSize = 10.sp,
                    fontWeight = if (isRecording) FontWeight.Bold else FontWeight.Normal,
                    color = if (isRecording) DangerRed else Color.White.copy(alpha = 0.8f)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (recordingMode == RecordingMode.LOOP) Color(0xFF6C3CE9) else Color(0xFF281C48))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .clickable { onToggleRecMode() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (recordingMode == RecordingMode.LOOP) "LOOP" else "FREE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Metronome
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onMetronomeClick() }) {
            Icon(
                Icons.Default.AvTimer,
                contentDescription = "Metronome",
                tint = if (isMetronomeOn) AccentCyan else Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
            Text("Metro", fontSize = 10.sp, color = if (isMetronomeOn) AccentCyan else Color.White.copy(alpha = 0.7f))
        }

        // BPM Control
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF2C1F52))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(18.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("−", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White,
                modifier = Modifier.clickable { onBpmDecrease() }.padding(horizontal = 6.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 4.dp)) {
                Text("$bpm", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("BPM", fontSize = 8.sp, color = Color.White.copy(alpha = 0.6f))
            }
            Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White,
                modifier = Modifier.clickable { onBpmIncrease() }.padding(horizontal = 6.dp))
        }
    }
}

@Composable
private fun PadsToolbar(
    selectedBank: PadBank,
    isRecording: Boolean,
    recordingSeconds: Int,
    recordingMode: RecordingMode,
    isSoundFxOpen: Boolean,
    isHoldMode: Boolean,
    onSoundFxClick: () -> Unit,
    onRecordClick: () -> Unit,
    onToggleRecMode: () -> Unit,
    onHoldToggle: () -> Unit,
    onSceneSelect: (PadBank) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sound FX Drawer Toggle
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { onSoundFxClick() }
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "Sound FX",
                tint = if (isSoundFxOpen) AccentCyan else Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = "Sound FX",
                fontSize = 10.sp,
                fontWeight = if (isSoundFxOpen) FontWeight.Bold else FontWeight.Normal,
                color = if (isSoundFxOpen) AccentCyan else Color.White.copy(alpha = 0.8f)
            )
        }

        // Record Button + Free/Loop Mode Toggle
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(DangerRed))
                    }
                }
                Text(
                    text = if (isRecording) String.format("%02d:%02d", recordingSeconds / 60, recordingSeconds % 60) else "Record",
                    fontSize = 10.sp,
                    fontWeight = if (isRecording) FontWeight.Bold else FontWeight.Normal,
                    color = if (isRecording) DangerRed else Color.White.copy(alpha = 0.8f)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (recordingMode == RecordingMode.LOOP) Color(0xFF6C3CE9) else Color(0xFF281C48))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .clickable { onToggleRecMode() }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (recordingMode == RecordingMode.LOOP) "LOOP" else "FREE",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // HOLD Mode Toggle (from Google Play reference screenshot!)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isHoldMode) AccentYellow else Color(0xFF281C48))
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .clickable { onHoldToggle() }
                .padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            Text(
                text = "HOLD",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isHoldMode) Color.Black else Color.White.copy(alpha = 0.8f)
            )
        }

        // Scene A | B | 24 Switcher
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF352562))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            ) {
                listOf(PadBank.BANK_A to "A", PadBank.BANK_B to "B", PadBank.FULL_24 to "24").forEach { (bank, label) ->
                    val sel = selectedBank == bank
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (sel) Color.White else Color.Transparent)
                            .clickable { onSceneSelect(bank) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (sel) Color.Black else Color.White.copy(alpha = 0.65f)
                        )
                    }
                }
            }
            Text(text = "Scene", fontSize = 10.sp, color = Color.White.copy(alpha = 0.75f))
        }
    }
}

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
                    .padding(vertical = 7.dp),
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
                Box(modifier = Modifier.width(1.dp).height(26.dp).background(Color.White.copy(alpha = 0.12f)))
            }
        }
    }
}

@Composable
private fun GlowingPadsGridView(
    uiState: DrumPadsUiState,
    viewModel: DrumPadsViewModel
) {
    val cols = if (uiState.selectedBank == PadBank.FULL_24) 4 else 3
    val rows = if (uiState.selectedBank == PadBank.FULL_24) 6 else 4
    val startIndex = if (uiState.selectedBank == PadBank.BANK_B) 12 else 0
    val targetTutPad = uiState.activeTutorial?.takeIf { !it.isCompleted }?.targetPadIndex

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(if (rows == 6) 5.dp else 8.dp)
    ) {
        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(if (cols == 4) 5.dp else 8.dp)
            ) {
                for (col in 0 until cols) {
                    val absoluteIndex = startIndex + row * cols + col
                    val pad = uiState.pads.getOrNull(absoluteIndex)
                    val baseColor = parseHexColor(pad?.colorHex ?: "#38E54D")
                    val isPressed = uiState.activePads.contains(absoluteIndex)
                    val isTutorialTarget = (targetTutPad == absoluteIndex)

                    PadCell(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        pad = pad,
                        baseColor = baseColor,
                        isPressed = isPressed,
                        isTutorialTarget = isTutorialTarget,
                        showLabel = uiState.showPadLabels,
                        compact = (rows == 6),
                        onDown = { viewModel.onPadDown(absoluteIndex) },
                        onUp = { viewModel.onPadUp(absoluteIndex) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PadCell(
    modifier: Modifier = Modifier,
    pad: Pad?,
    baseColor: Color,
    isPressed: Boolean,
    isTutorialTarget: Boolean,
    showLabel: Boolean,
    compact: Boolean,
    onDown: () -> Unit,
    onUp: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "padScale"
    )

    val backgroundBrush = remember(isPressed, baseColor) {
        if (isPressed) {
            Brush.verticalGradient(listOf(Color.White, Color(0xFFF3F4F6)))
        } else {
            Brush.verticalGradient(
                listOf(
                    Color(
                        (baseColor.red * 0.80f + 0.20f).coerceIn(0f, 1f),
                        (baseColor.green * 0.80f + 0.20f).coerceIn(0f, 1f),
                        (baseColor.blue * 0.80f + 0.20f).coerceIn(0f, 1f),
                        1f
                    ),
                    baseColor,
                    Color(
                        (baseColor.red * 0.80f).coerceIn(0f, 1f),
                        (baseColor.green * 0.80f).coerceIn(0f, 1f),
                        (baseColor.blue * 0.80f).coerceIn(0f, 1f),
                        1f
                    )
                )
            )
        }
    }

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(if (compact) 10.dp else 14.dp))
            .background(backgroundBrush)
            .border(
                width = when {
                    isTutorialTarget -> 3.5.dp
                    isPressed -> 2.5.dp
                    else -> 1.dp
                },
                color = when {
                    isTutorialTarget -> AccentCyan
                    isPressed -> Color.White
                    else -> Color.White.copy(alpha = 0.22f)
                },
                shape = RoundedCornerShape(if (compact) 10.dp else 14.dp)
            )
            .pointerInput(pad?.index) {
                awaitEachGesture {
                    awaitFirstDown()
                    onDown()
                    do {
                        val event = awaitPointerEvent()
                    } while (event.changes.any { it.pressed })
                    onUp()
                }
            }
            .padding(if (compact) 4.dp else 8.dp)
    ) {
        if (isTutorialTarget) {
            Text(
                text = "TAP",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(6.dp))
                    .background(AccentCyan)
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            )
        } else if (pad != null && (pad.playMode != PadPlayMode.ONE_SHOT || pad.chokeGroup > 0)) {
            val tag = if (pad.playMode == PadPlayMode.LOOP) "∞" else if (pad.chokeGroup > 0) "C${pad.chokeGroup}" else ""
            if (tag.isNotEmpty()) {
                Text(
                    text = tag,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPressed) Color.Black else Color.White.copy(alpha = 0.85f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.35f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }

        if (showLabel && pad != null) {
            Text(
                text = pad.name,
                fontSize = if (compact) 9.sp else 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPressed) Color.Black else Color.White.copy(alpha = 0.92f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isPressed) Color.Transparent else Color.Black.copy(alpha = 0.25f))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }
    }
}

@Composable
private fun ClipsMatrixView(
    clips: List<ClipSlot>,
    activeClips: Set<Int>,
    selectedClipIndex: Int,
    currentStep: Int,
    isRecording: Boolean,
    onClipClick: (Int) -> Unit
) {
    val playheadProgress = (currentStep % 16) / 16f

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(clips.size) { index ->
            val clip = clips[index]
            val isPlaying = activeClips.contains(index)
            val isSelected = selectedClipIndex == index
            val isRecordingIntoThis = isRecording && isSelected

            ClipTile(
                clip = clip,
                isPlaying = isPlaying,
                isSelected = isSelected,
                isRecordingTarget = isRecordingIntoThis,
                playheadProgress = playheadProgress,
                onClick = { onClipClick(index) }
            )
        }
    }
}

@Composable
private fun ClipTile(
    clip: ClipSlot,
    isPlaying: Boolean,
    isSelected: Boolean,
    isRecordingTarget: Boolean,
    playheadProgress: Float,
    onClick: () -> Unit
) {
    val accent = parseHexColor(clip.colorHex)
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isPlaying) Color(0xFF281E60) else Color(0xFF1B1A32))
            .border(
                width = if (isRecordingTarget || isSelected) 2.dp else 1.dp,
                color = when {
                    isRecordingTarget -> DangerRed
                    isSelected -> Color.White
                    isPlaying -> accent
                    else -> Color.White.copy(alpha = 0.1f)
                },
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(6.dp)
    ) {
        if (clip.isEmpty) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("+", fontSize = 20.sp, color = Color.White.copy(alpha = 0.55f), fontWeight = FontWeight.Light)
                Text("Clip ${clip.index + 1}", fontSize = 9.sp, color = Color.White.copy(alpha = 0.4f))
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                // REAL pattern visualization from clip.hits!
                Column(
                    modifier = Modifier.fillMaxSize().padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    val distinctPads = clip.hits.map { it.padIndex }.distinct().take(4)
                    distinctPads.forEach { pIdx ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (step in 0 until 8) {
                                val hitPresent = clip.hits.any { it.padIndex == pIdx && (it.step / 2) == step }
                                Box(
                                    modifier = Modifier
                                        .size(if (hitPresent) 5.dp else 3.dp)
                                        .clip(CircleShape)
                                        .background(if (hitPresent) accent else Color.White.copy(alpha = 0.1f))
                                )
                            }
                        }
                    }
                }

                Text(
                    text = clip.name,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.BottomStart)
                )

                if (isPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(playheadProgress.coerceAtLeast(0.05f))
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
private fun ClipActionBar(
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onToggleFx: () -> Unit,
    isFxOpen: Boolean
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF382A72))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(22.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ActionPillItem(Icons.Default.Edit, "Edit", onEdit)
        Box(modifier = Modifier.width(1.dp).height(28.dp).background(Color.White.copy(alpha = 0.15f)))
        ActionPillItem(Icons.Default.ContentCopy, "Copy", onDuplicate)
        Box(modifier = Modifier.width(1.dp).height(28.dp).background(Color.White.copy(alpha = 0.15f)))
        ActionPillItem(Icons.Default.Delete, "Clear", onDelete)
        Box(modifier = Modifier.width(1.dp).height(28.dp).background(Color.White.copy(alpha = 0.15f)))
        ActionPillItem(Icons.Default.Tune, if (isFxOpen) "Hide FX" else "FX", onToggleFx)
    }
}

@Composable
private fun ActionPillItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.clickable { onClick() }.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

/**
 * Full 10-FX Real-Time DSP Audio Effects Drawer:
 * Rhythm FX: [FILL] [CLEAN] [RESET] + Live Stereo Peak Meter
 * 10 Draggable Live DSP Faders: Volume, Lo-Fi, Filter, Pitch, Reverb, Delay 1/16, Delay 1/8, Distortion, Gate, Wobble
 */
@Composable
private fun FullAudioFxDrawer(
    dsp: DspEffectParams,
    peakLevel: Float,
    onFaderChange: (String, Float) -> Unit,
    onFillClick: () -> Unit,
    onCleanClick: () -> Unit,
    onResetClick: () -> Unit
) {
    val faders = listOf(
        Triple("Volume", dsp.masterVolume, Color(0xFF00DFD8)),
        Triple("Lo-Fi", dsp.lofiLevel, Color(0xFFFFD600)),
        Triple("Filter", dsp.filterCutoff, Color(0xFF38E54D)),
        Triple("Pitch", ((dsp.masterPitchSemitones / 24f) + 0.5f).coerceIn(0f, 1f), Color(0xFFFF2A85)),
        Triple("Reverb", dsp.reverbLevel, Color(0xFF8B5CF6)),
        Triple("Delay 1/16", dsp.delay116Level, Color(0xFF00B0FF)),
        Triple("Delay 1/8", dsp.delay18Level, Color(0xFF00E5FF)),
        Triple("Distortion", dsp.distortionLevel, Color(0xFFFF5E62)),
        Triple("Gate", dsp.gateLevel, Color(0xFFE040FB)),
        Triple("Wobble", dsp.wobbleLevel, Color(0xFFFF7A00))
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF15122B))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        // Rhythm FX Bar: FILL, CLEAN, RESET + Live Audio Peak Meter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFFFF2A85), Color(0xFFFF7A00))))
                    .clickable { onFillClick() }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("⚡ FILL", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF06D6A0))
                    .clickable { onCleanClick() }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("✨ CLEAN", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.Black)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF2B224C))
                    .clickable { onResetClick() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("Reset", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.85f))
            }

            // Live Audio Level Meter
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFF0B091A))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(peakLevel.coerceIn(0.02f, 1f))
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFF38E54D), Color(0xFFFFD600), Color(0xFFFF2A85)))
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 10 Draggable Vertical DSP Faders
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            faders.forEach { (title, value, color) ->
                InteractiveVerticalFader(
                    title = title,
                    value = value,
                    accentColor = color,
                    displayValue = if (title == "Pitch") {
                        val st = ((value - 0.5f) * 24f).roundToInt()
                        if (st > 0) "+${st}st" else "${st}st"
                    } else {
                        "${(value * 100).roundToInt()}%"
                    },
                    onChange = { newVal -> onFaderChange(title, newVal) }
                )
            }
        }
    }
}

@Composable
private fun InteractiveVerticalFader(
    title: String,
    value: Float,
    accentColor: Color,
    displayValue: String,
    onChange: (Float) -> Unit
) {
    val currentValue by rememberUpdatedState(value)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(58.dp)
    ) {
        Text(
            text = displayValue,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .width(38.dp)
                .height(84.dp)
                .clip(RoundedCornerShape(19.dp))
                .background(Color(0xFF221A3E))
                .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(19.dp))
                .pointerInput(title) {
                    detectVerticalDragGestures { change, dragAmount ->
                        change.consume()
                        val delta = -dragAmount / size.height.toFloat()
                        onChange((currentValue + delta).coerceIn(0f, 1f))
                    }
                }
                .clickable {
                    // Quick toggle preset on tap
                    val next = if (title == "Pitch") 0.5f else if (currentValue > 0.3f) 0f else 0.7f
                    onChange(next)
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(value.coerceIn(0.02f, 1f))
                    .background(Brush.verticalGradient(listOf(accentColor, accentColor.copy(alpha = 0.45f))))
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight(0.8f)
                    .background(Color.White.copy(alpha = 0.25f))
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = title,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.85f),
            maxLines = 1
        )
    }
}

@Composable
private fun NeonAppBranding() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Drum Pads ", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFFFF2A85))
        Text("24", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF00E5FF))
    }
}

fun parseHexColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Throwable) {
        Color(0xFF38E54D)
    }
}
