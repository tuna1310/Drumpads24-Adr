package com.drumpads.app.ui.screens

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drumpads.app.domain.model.MusicGenre
import com.drumpads.app.domain.model.PadPlayMode
import com.drumpads.app.domain.model.SoundPackCatalog
import com.drumpads.app.ui.pads.ActiveScreen
import com.drumpads.app.ui.pads.DrumPadsUiState
import com.drumpads.app.ui.pads.DrumPadsViewModel
import com.drumpads.app.ui.theme.*
import kotlin.math.roundToInt

/**
 * Pad Editor matching Google Play "CREATE SOUND PACKS" screenshot:
 * - Two tabs: EDIT PADS and LOAD SOUND
 * - Side-by-side Scene A & Scene B mini 3x4 grids (all 24 pads!)
 * - Inspector: Sound row (-> Waveform Sound Editor), Pitch stepper, Play Mode (ONE SHOT / LOOP / HOLD),
 *   Color swatches, Group Choke (OFF / 1 / 2), Per-Pad FX (Reverb / Distortion), Swap Pads & Duplicate!
 */
@Composable
fun PadEditorScreen(
    uiState: DrumPadsUiState,
    viewModel: DrumPadsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(0) } // 0 = EDIT PADS, 1 = LOAD SOUND
    var swapModeActive by remember { mutableStateOf(false) }
    var duplicateModeActive by remember { mutableStateOf(false) }
    var libraryGenre by remember { mutableStateOf(uiState.activeGenre) }

    val selectedIdx = uiState.editingPadIndex.coerceIn(0, (uiState.pads.size - 1).coerceAtLeast(0))
    val selectedPad = uiState.pads.getOrNull(selectedIdx)

    // Real Android File Picker (WAV, MP3, M4A, OGG from local storage / Google Drive / cloud!)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            var displayName: String? = null
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIdx >= 0) {
                        displayName = cursor.getString(nameIdx)
                    }
                }
            } catch (_: Throwable) {}
            viewModel.importAudioFileToPad(selectedIdx, uri, displayName)
        }
    }

    val colorSwatches = listOf(
        "#38E54D", "#FF7A00", "#E040FB", "#00B0FF",
        "#FF2E93", "#9D00FF", "#FFD600", "#00F5D4"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFFB703))
            .statusBarsPadding()
            .background(NavyBackground)
            .navigationBarsPadding()
    ) {
        // Signature Yellow/Amber Header Bar matching Play Store "CREATE SOUND PACKS"
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFB703))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { viewModel.navigateTo(ActiveScreen.PAD_PLAYER) }) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Black)
                }
                Text(
                    text = "${uiState.selectedPackName} • Edit",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
                Text(
                    text = "PLAY ▶",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.12f))
                        .clickable {
                            viewModel.onPadDown(selectedIdx)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            // Two Tabs: EDIT PADS | LOAD SOUND
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("EDIT PADS", "LOAD SOUND").forEachIndexed { idx, title ->
                    val isSel = activeTab == idx
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { activeTab = idx }
                            .padding(top = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isSel) Color.Black else Color.Black.copy(alpha = 0.55f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(if (isSel) Color.Black else Color.Transparent)
                        )
                    }
                }
            }
        }

        // Side-by-side Scene A & Scene B mini grids (all 24 pads visible at once!)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF141326))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            if (swapModeActive || duplicateModeActive) {
                Text(
                    text = if (swapModeActive) "Tap another pad to SWAP with Pad #${selectedIdx + 1}" else "Tap destination pad to DUPLICATE Pad #${selectedIdx + 1} into",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentCyan,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MiniSceneGrid(
                    title = "SCENE A",
                    startIndex = 0,
                    uiState = uiState,
                    selectedIndex = selectedIdx,
                    modifier = Modifier.weight(1f),
                    onPadClick = { idx ->
                        when {
                            swapModeActive -> {
                                viewModel.swapPads(selectedIdx, idx)
                                swapModeActive = false
                            }
                            duplicateModeActive -> {
                                viewModel.duplicatePad(selectedIdx, idx)
                                duplicateModeActive = false
                            }
                            else -> {
                                viewModel.selectEditorPad(idx)
                                viewModel.onPadDown(idx)
                            }
                        }
                    }
                )
                MiniSceneGrid(
                    title = "SCENE B",
                    startIndex = 12,
                    uiState = uiState,
                    selectedIndex = selectedIdx,
                    modifier = Modifier.weight(1f),
                    onPadClick = { idx ->
                        when {
                            swapModeActive -> {
                                viewModel.swapPads(selectedIdx, idx)
                                swapModeActive = false
                            }
                            duplicateModeActive -> {
                                viewModel.duplicatePad(selectedIdx, idx)
                                duplicateModeActive = false
                            }
                            else -> {
                                viewModel.selectEditorPad(idx)
                                viewModel.onPadDown(idx)
                            }
                        }
                    }
                )
            }
        }

        // TAB BODY
        if (activeTab == 0 && selectedPad != null) {
            // EDIT PADS INSPECTOR
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Selected Sound Row -> opens SoundEditorScreen (Crop, Fade, Mic Record!)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(NavyCard)
                            .border(1.dp, AccentCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .clickable { viewModel.openSoundEditor(selectedIdx) }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = AccentCyan)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pad #${selectedIdx + 1}: ${selectedPad.name}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Tap to Crop Waveform, Fade In/Out, or Mic Record",
                                fontSize = 11.sp,
                                color = AccentCyan
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
                    }
                }

                // PITCH + PLAY MODE row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Pitch Stepper
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(NavyCard)
                                .padding(12.dp)
                        ) {
                            Text("PITCH", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(NavyCardElevated)
                                        .clickable {
                                            val next = (selectedPad.pitchSemitones - 1f).coerceIn(-12f, 12f)
                                            viewModel.updatePad(selectedPad.copy(pitchSemitones = next))
                                            viewModel.onPadDown(selectedIdx)
                                        },
                                    contentAlignment = Alignment.Center
                                ) { Text("−", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) }

                                Text(
                                    text = "${selectedPad.pitchSemitones.roundToInt()} st",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentYellow
                                )

                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(NavyCardElevated)
                                        .clickable {
                                            val next = (selectedPad.pitchSemitones + 1f).coerceIn(-12f, 12f)
                                            viewModel.updatePad(selectedPad.copy(pitchSemitones = next))
                                            viewModel.onPadDown(selectedIdx)
                                        },
                                    contentAlignment = Alignment.Center
                                ) { Text("+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                            }
                        }

                        // Play Mode
                        Column(
                            modifier = Modifier
                                .weight(1.3f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(NavyCard)
                                .padding(12.dp)
                        ) {
                            Text("PLAY MODE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                PadPlayMode.entries.forEach { mode ->
                                    val isSel = selectedPad.playMode == mode
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) AccentCyan else NavyCardElevated)
                                            .clickable { viewModel.updatePad(selectedPad.copy(playMode = mode)) }
                                            .padding(vertical = 7.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (mode == PadPlayMode.ONE_SHOT) "1-SHOT" else mode.label,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) Color.Black else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // COLOR + GROUP CHOKE + EDIT MODE (SWAP / DUPLICATE)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(NavyCard)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("PAD COLOR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            colorSwatches.forEach { hex ->
                                val isSel = selectedPad.colorHex.equals(hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(parseHexColor(hex))
                                        .border(if (isSel) 2.5.dp else 0.dp, Color.White, RoundedCornerShape(8.dp))
                                        .clickable { viewModel.updatePad(selectedPad.copy(colorHex = hex)) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("GROUP CHOKE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(0 to "OFF", 1 to "1", 2 to "2").forEach { (grp, lbl) ->
                                        val isSel = selectedPad.chokeGroup == grp
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSel) AccentYellow else NavyCardElevated)
                                                .clickable { viewModel.updatePad(selectedPad.copy(chokeGroup = grp)) }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(lbl, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.Black else Color.White)
                                        }
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(1.2f)) {
                                Text("EDIT MODE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (swapModeActive) AccentPink else Color(0xFF382A72))
                                            .clickable {
                                                swapModeActive = !swapModeActive
                                                duplicateModeActive = false
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("SWAP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (duplicateModeActive) AccentCyan else Color(0xFF281C48))
                                            .clickable {
                                                duplicateModeActive = !duplicateModeActive
                                                swapModeActive = false
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("DUPLICATE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (duplicateModeActive) Color.Black else Color.White)
                                    }
                                }
                            }
                        }
                    }
                }

                // VOLUME, PAN, PER-PAD REVERB & DISTORTION SLIDERS
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(NavyCard)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("PAD MIX & EFFECTS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)

                        PadSliderRow("Volume", "${(selectedPad.volume * 100).roundToInt()}%", selectedPad.volume, 0f..1.5f) {
                            viewModel.updatePad(selectedPad.copy(volume = it))
                        }
                        PadSliderRow(
                            "Stereo Pan",
                            when {
                                selectedPad.pan < -0.1f -> "L ${(kotlin.math.abs(selectedPad.pan) * 100).roundToInt()}"
                                selectedPad.pan > 0.1f -> "R ${(selectedPad.pan * 100).roundToInt()}"
                                else -> "Center"
                            },
                            selectedPad.pan,
                            -1f..1f
                        ) {
                            viewModel.updatePad(selectedPad.copy(pan = it))
                        }
                        PadSliderRow("Pad Reverb Send", "${(selectedPad.padReverbSend * 100).roundToInt()}%", selectedPad.padReverbSend, 0f..1f) {
                            viewModel.updatePad(selectedPad.copy(padReverbSend = it))
                        }
                        PadSliderRow("Pad Distortion", "${(selectedPad.padDistortion * 100).roundToInt()}%", selectedPad.padDistortion, 0f..1f) {
                            viewModel.updatePad(selectedPad.copy(padDistortion = it))
                        }
                    }
                }
            }
        } else {
            // LOAD SOUND TAB: Import File, Mic Record, or Pick from Built-In 12-Genre Sound Bank!
            val sampleList = remember(libraryGenre) { SoundPackCatalog.buildPadsForGenre(libraryGenre) }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { filePickerLauncher.launch(arrayOf("audio/*")) },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import Audio", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { viewModel.openSoundEditor(selectedIdx) },
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mic Sample", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                item {
                    Text("BUILT-IN 12-GENRE SAMPLE BANK", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MusicGenre.playableGenres.forEach { g ->
                            val isSel = libraryGenre == g
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSel) Color(g.accentColor) else NavyCard)
                                    .clickable { libraryGenre = g }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(g.label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.Black else Color.White)
                            }
                        }
                    }
                }

                itemsIndexed(sampleList) { idx, samplePad ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(NavyCard)
                            .clickable {
                                viewModel.assignBuiltInSampleToPad(selectedIdx, libraryGenre, idx)
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(parseHexColor(samplePad.colorHex)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${idx + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(samplePad.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("${libraryGenre.label} • ${samplePad.group.name}", fontSize = 11.sp, color = TextSecondary)
                        }
                        Text("ASSIGN", fontSize = 11.sp, fontWeight = FontWeight.Black, color = AccentYellow)
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniSceneGrid(
    title: String,
    startIndex: Int,
    uiState: DrumPadsUiState,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    onPadClick: (Int) -> Unit
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (r in 0 until 4) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (c in 0 until 3) {
                        val idx = startIndex + r * 3 + c
                        val pad = uiState.pads.getOrNull(idx)
                        val isSel = selectedIndex == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.15f)
                                .clip(RoundedCornerShape(5.dp))
                                .background(parseHexColor(pad?.colorHex ?: "#38E54D"))
                                .border(
                                    width = if (isSel) 2.5.dp else 0.5.dp,
                                    color = if (isSel) Color.White else Color.Black.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(5.dp)
                                )
                                .clickable { onPadClick(idx) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${idx + 1}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isSel) Color.White else Color.Black.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PadSliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 12.sp, color = TextPrimary)
            Text(valueText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentCyan)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = AccentCyan, activeTrackColor = AccentCyan)
        )
    }
}
