package com.drumpads.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drumpads.app.domain.model.MusicGenre
import com.drumpads.app.ui.pads.ActiveScreen
import com.drumpads.app.ui.pads.DrumPadsUiState
import com.drumpads.app.ui.pads.DrumPadsViewModel
import com.drumpads.app.ui.theme.*

@Composable
fun ProjectEditorScreen(
    uiState: DrumPadsUiState,
    viewModel: DrumPadsViewModel,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf("My Custom Kit #${uiState.customSoundPacks.size + 1}") }
    var selectedGenre by remember { mutableStateOf(MusicGenre.TRAP) }
    var bpm by remember { mutableStateOf(selectedGenre.defaultBpm) }

    val colorThemes = listOf(
        Pair(0xFFFF2A85L, 0xFF00DFD8L),
        Pair(0xFFE94057L, 0xFFF27121L),
        Pair(0xFF4776E6L, 0xFF8E54E9L),
        Pair(0xFF11998EL, 0xFF38EF7DL),
        Pair(0xFFFA709AL, 0xFFFEE140L)
    )
    var selectedThemeIdx by remember { mutableStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFB703))
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { viewModel.navigateTo(ActiveScreen.MAIN) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
            }
            Text("Project Editor", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.Black)
            Text(
                text = "CREATE",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.14f))
                    .clickable {
                        val theme = colorThemes[selectedThemeIdx]
                        viewModel.createCustomSoundPack(
                            title = title,
                            genre = selectedGenre,
                            bpm = bpm,
                            gradientStart = theme.first,
                            gradientEnd = theme.second,
                            openEditorAfter = true
                        )
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(NavyCard)
                        .border(1.dp, NavyBorder, RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("NEW CUSTOM SOUND PACK", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentCyan)

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Sound Pack Name", color = TextSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = NavyBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Base Genre Template (24 Audio Samples)", fontSize = 12.sp, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MusicGenre.playableGenres.forEach { genre ->
                            val isSel = selectedGenre == genre
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSel) Color(genre.accentColor) else NavyCardElevated)
                                    .clickable {
                                        selectedGenre = genre
                                        bpm = genre.defaultBpm
                                    }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(genre.label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.Black else Color.White)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Default Tempo: $bpm BPM", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier.size(34.dp).clip(CircleShape).background(NavyCardElevated).clickable { bpm = (bpm - 5).coerceIn(60, 200) },
                                contentAlignment = Alignment.Center
                            ) { Text("−", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                            Box(
                                modifier = Modifier.size(34.dp).clip(CircleShape).background(NavyCardElevated).clickable { bpm = (bpm + 5).coerceIn(60, 200) },
                                contentAlignment = Alignment.Center
                            ) { Text("+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                        }
                    }

                    Text("Artwork Style", fontSize = 12.sp, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        colorThemes.forEachIndexed { idx, (c1, c2) ->
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Brush.linearGradient(listOf(Color(c1), Color(c2))))
                                    .border(if (selectedThemeIdx == idx) 3.dp else 0.dp, Color.White, RoundedCornerShape(12.dp))
                                    .clickable { selectedThemeIdx = idx }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                val theme = colorThemes[selectedThemeIdx]
                                viewModel.createCustomSoundPack(title, selectedGenre, bpm, theme.first, theme.second, openEditorAfter = true)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Create & Edit Pads", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                val theme = colorThemes[selectedThemeIdx]
                                viewModel.createCustomSoundPack(title, selectedGenre, bpm, theme.first, theme.second, openEditorAfter = false)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPink),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Create & Play ▶", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }
                }
            }

            if (uiState.customSoundPacks.isNotEmpty()) {
                item {
                    Text(
                        text = "MY CUSTOM SOUND PACKS (${uiState.customSoundPacks.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                }

                items(uiState.customSoundPacks) { pack ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(NavyCard)
                            .border(1.dp, NavyBorder, RoundedCornerShape(16.dp))
                            .clickable { viewModel.openSoundPack(pack) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(Color(pack.gradientStart), Color(pack.gradientEnd)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(pack.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("${pack.genre.label} • ${pack.bpm} BPM • 24 Pads", fontSize = 12.sp, color = TextSecondary)
                        }
                        IconButton(onClick = {
                            viewModel.openSoundPack(pack)
                            viewModel.openPadEditor(0)
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AccentCyan)
                        }
                        IconButton(onClick = { viewModel.deleteCustomPack(pack.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DangerRed)
                        }
                    }
                }
            }
        }
    }
}
