package com.drumpads.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.drumpads.app.domain.model.RecordingItem
import com.drumpads.app.ui.pads.ActiveScreen
import com.drumpads.app.ui.pads.DrumPadsUiState
import com.drumpads.app.ui.pads.DrumPadsViewModel
import com.drumpads.app.ui.theme.*
import java.io.File

@Composable
fun RecordingsScreen(
    uiState: DrumPadsUiState,
    viewModel: DrumPadsViewModel,
    modifier: Modifier = Modifier
) {
    var renameTarget by remember { mutableStateOf<RecordingItem?>(null) }
    var renameText by remember { mutableStateOf("") }
    var publishTarget by remember { mutableStateOf<RecordingItem?>(null) }
    var publishCaption by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { viewModel.navigateTo(ActiveScreen.MAIN) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text("My Recordings (${uiState.recordings.size})", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            TextButton(onClick = { viewModel.navigateTo(ActiveScreen.PAD_PLAYER) }) {
                Text("+ New Rec", color = AccentCyan, fontWeight = FontWeight.Bold)
            }
        }

        if (uiState.isExportingVideo) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF281E60))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = AccentCyan, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Rendering 9:16 H.264 MP4 Performance Video...", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        if (uiState.recordings.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = TextMuted, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(14.dp))
                Text("No recordings yet", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Open any Sound Pack, tap Record (Free or Loop mode), play pads, then stop to capture a studio 44.1kHz WAV track!",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { viewModel.navigateTo(ActiveScreen.PAD_PLAYER) },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Open Pad Player", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.recordings, key = { it.id }) { rec ->
                    val isPlaying = uiState.playingRecordingId == rec.id
                    val fileSizeKb = remember(rec.filePath) { (File(rec.filePath).length() / 1024L).coerceAtLeast(1L) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(NavyCard)
                            .border(1.dp, if (isPlaying) AccentCyan else NavyBorder, RoundedCornerShape(18.dp))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(AccentPink, AccentCyan)))
                                    .clickable { viewModel.togglePlayRecording(rec) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(rec.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(
                                    text = "${rec.durationFormatted} • ${rec.packName} (${rec.bpm} BPM) • ${rec.mode.label} • ${fileSizeKb} KB WAV",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }

                            IconButton(onClick = {
                                renameTarget = rec
                                renameText = rec.title
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Rename", tint = TextSecondary, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { viewModel.deleteRecording(rec.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DangerRed, modifier = Modifier.size(18.dp))
                            }
                        }

                        if (isPlaying) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { uiState.recordingPlaybackProgress },
                                color = AccentCyan,
                                trackColor = NavyCardElevated,
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Export Actions Row: Share WAV | Save Music | Video MP4 | Post Feed
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ExportChip("Share WAV", Icons.Default.Share, AccentCyan, Modifier.weight(1f)) {
                                viewModel.shareRecordingWav(rec)
                            }
                            ExportChip("Save Music", Icons.Default.Download, AccentYellow, Modifier.weight(1f)) {
                                viewModel.saveRecordingToMusic(rec)
                            }
                            ExportChip("Video MP4", Icons.Default.Videocam, AccentPink, Modifier.weight(1f)) {
                                viewModel.exportRecordingVideo(rec)
                            }
                            ExportChip("Post Feed", Icons.Default.Public, Color(0xFF38E54D), Modifier.weight(1f)) {
                                publishTarget = rec
                                publishCaption = "Check out my ${rec.packName} beat at ${rec.bpm} BPM! 🔥🥁"
                            }
                        }
                    }
                }
            }
        }
    }

    // Rename Dialog
    if (renameTarget != null) {
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            containerColor = NavyCard,
            title = { Text("Rename Recording", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameRecording(renameTarget!!.id, renameText)
                    renameTarget = null
                }) { Text("Save", color = AccentCyan, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancel", color = TextSecondary) }
            }
        )
    }

    // Publish to Community Feed Dialog
    if (publishTarget != null) {
        AlertDialog(
            onDismissRequest = { publishTarget = null },
            containerColor = NavyCard,
            title = { Text("Share to Beat Maker Feed", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(publishTarget!!.title, fontSize = 13.sp, color = AccentYellow, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = publishCaption,
                        onValueChange = { publishCaption = it },
                        label = { Text("Caption", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.publishRecordingToFeed(publishTarget!!, publishCaption)
                    publishTarget = null
                }) { Text("Publish 🚀", color = Color(0xFF38E54D), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { publishTarget = null }) { Text("Cancel", color = TextSecondary) }
            }
        )
    }
}

@Composable
private fun ExportChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(NavyCardElevated)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.height(3.dp))
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
    }
}
