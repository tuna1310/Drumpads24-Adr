package com.drumpads.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drumpads.app.domain.model.MusicGenre
import com.drumpads.app.domain.model.SoundPack
import com.drumpads.app.ui.pads.ActiveScreen
import com.drumpads.app.ui.pads.DrumPadsUiState
import com.drumpads.app.ui.pads.DrumPadsViewModel
import com.drumpads.app.ui.theme.*

@Composable
fun SoundsScreen(
    uiState: DrumPadsUiState,
    viewModel: DrumPadsViewModel,
    modifier: Modifier = Modifier
) {
    var showSearchBar by remember { mutableStateOf(false) }

    val featuredStories = remember(uiState.allSoundPacks) {
        uiState.allSoundPacks.filter { it.isFeaturedStory }
    }
    val newPacks = remember(uiState.allSoundPacks) {
        uiState.allSoundPacks.filter { it.isNewRelease }
    }
    val filteredPacks = remember(uiState.allSoundPacks, uiState.selectedLibraryGenre, uiState.librarySearchQuery) {
        uiState.allSoundPacks.filter { pack ->
            val genreMatch = uiState.selectedLibraryGenre == MusicGenre.ALL || pack.genre == uiState.selectedLibraryGenre
            val q = uiState.librarySearchQuery.trim()
            val queryMatch = q.isEmpty() ||
                pack.title.contains(q, ignoreCase = true) ||
                pack.genre.label.contains(q, ignoreCase = true) ||
                pack.subtitle.contains(q, ignoreCase = true) ||
                pack.artist.contains(q, ignoreCase = true)
            genreMatch && queryMatch
        }
    }
    val recentFirst = uiState.recentPackIds.firstOrNull()?.let { id ->
        uiState.allSoundPacks.firstOrNull { it.id == id }
    } ?: uiState.allSoundPacks.first()

    Box(modifier = modifier.fillMaxSize().background(NavyBackground)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 145.dp)
        ) {
            // 1. Top Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Drum Pads 24", fontSize = 22.sp, fontWeight = FontWeight.Black, color = AccentCyan)
                        Text("${uiState.allSoundPacks.size} Sound Packs • 12 Genres", fontSize = 11.sp, color = TextSecondary)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { viewModel.openRandomPack() },
                            modifier = Modifier.size(38.dp).clip(CircleShape).background(NavyCard)
                        ) {
                            Icon(Icons.Default.Casino, contentDescription = "Random Pack", tint = AccentYellow, modifier = Modifier.size(20.dp))
                        }

                        IconButton(
                            onClick = { showSearchBar = !showSearchBar },
                            modifier = Modifier.size(38.dp).clip(CircleShape).background(if (showSearchBar) AccentCyan else NavyCard)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = if (showSearchBar) Color.Black else TextPrimary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // Optional Search Bar
            if (showSearchBar || uiState.librarySearchQuery.isNotEmpty()) {
                item {
                    OutlinedTextField(
                        value = uiState.librarySearchQuery,
                        onValueChange = { viewModel.setLibrarySearchQuery(it) },
                        placeholder = { Text("Search genre (Phonk, Lofi, Trap...), pack name, artist...", color = TextMuted, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AccentCyan) },
                        trailingIcon = {
                            if (uiState.librarySearchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setLibrarySearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = NavyBorder
                        ),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            // 2. Horizontal Stories Row (Featured Artists & Packs)
            if (uiState.librarySearchQuery.isBlank() && uiState.selectedLibraryGenre == MusicGenre.ALL) {
                item {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(featuredStories) { pack ->
                            val gradient = listOf(Color(pack.gradientStart), Color(pack.gradientEnd))
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { viewModel.openSoundPack(pack) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, Brush.linearGradient(gradient), CircleShape)
                                        .padding(3.dp)
                                        .clip(CircleShape)
                                        .background(Brush.radialGradient(gradient)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(pack.title.take(2).uppercase(), fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                                }
                                Spacer(modifier = Modifier.height(5.dp))
                                Text(pack.title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1)
                                Text(pack.genre.label, fontSize = 9.sp, color = TextSecondary, maxLines = 1)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // 3. Recently Used Banner
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recently used", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(
                            "See all >",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentYellow,
                            modifier = Modifier.clickable { viewModel.navigateTo(ActiveScreen.RECENTLY_USED) }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val recentPacks = uiState.recentPackIds.mapNotNull { id ->
                            uiState.allSoundPacks.firstOrNull { it.id == id }
                        }.ifEmpty { uiState.allSoundPacks.take(4) }
                        items(recentPacks) { pack ->
                            Box(modifier = Modifier.width(300.dp)) {
                                SoundPackRowCard(
                                    pack = pack,
                                    isPreviewing = uiState.previewingPackId == pack.id,
                                    isFavorite = uiState.favoritePackIds.contains(pack.id),
                                    onOpen = { viewModel.openSoundPack(pack) },
                                    onPreview = { viewModel.togglePreviewPack(pack) },
                                    onToggleFavorite = { viewModel.toggleFavoritePack(pack.id) }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                }

                // 4. New Releases Horizontal Carousel
                item {
                    Text(
                        text = "New Releases & Hot Packs",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(newPacks) { pack ->
                            val isPreviewing = uiState.previewingPackId == pack.id
                            Column(
                                modifier = Modifier
                                    .width(134.dp)
                                    .clickable { viewModel.openSoundPack(pack) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(134.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Brush.linearGradient(listOf(Color(pack.gradientStart), Color(pack.gradientEnd))))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = pack.genre.label.uppercase(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.Black,
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.White.copy(alpha = 0.85f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )

                                    Text(
                                        text = pack.title.uppercase(),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        maxLines = 2,
                                        modifier = Modifier.align(Alignment.Center)
                                    )

                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.6f))
                                            .clickable { viewModel.togglePreviewPack(pack) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isPreviewing) Icons.Default.Stop else Icons.Default.PlayArrow,
                                            contentDescription = "Preview",
                                            tint = if (isPreviewing) AccentYellow else Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Text(
                                        text = "${pack.bpm} BPM",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.align(Alignment.BottomEnd)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(pack.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(pack.artist, fontSize = 11.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // 5. 12-Genre Filter Strip (EDM, Trap, Hip-Hop, House, Dubstep, Phonk, Lo-Fi, Synthwave, DnB, Techno, R&B, Pop)
            item {
                Text(
                    text = "Browse All Sound Packs",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MusicGenre.entries.forEach { genre ->
                        val isSel = uiState.selectedLibraryGenre == genre
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isSel) Color(genre.accentColor) else NavyCard)
                                .border(1.dp, if (isSel) Color.White.copy(alpha = 0.4f) else NavyBorder, RoundedCornerShape(18.dp))
                                .clickable { viewModel.selectLibraryGenre(genre) }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = genre.label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.Black else TextPrimary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 6. Filtered Sound Packs List (matching Play Store "EXPLORE SOUNDS" list!)
            items(filteredPacks, key = { it.id }) { pack ->
                SoundPackRowCard(
                    pack = pack,
                    isPreviewing = uiState.previewingPackId == pack.id,
                    isFavorite = uiState.favoritePackIds.contains(pack.id),
                    onOpen = { viewModel.openSoundPack(pack) },
                    onPreview = { viewModel.togglePreviewPack(pack) },
                    onToggleFavorite = { viewModel.toggleFavoritePack(pack.id) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // Floating Action Button (+) to Create Custom Sound Pack (matching Play Store "EXPLORE SOUNDS" screenshot!)
        FloatingActionButton(
            onClick = { viewModel.navigateTo(ActiveScreen.PROJECT_EDITOR) },
            containerColor = Color(0xFFFFB703),
            contentColor = Color.Black,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 148.dp)
                .size(54.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create Sound Pack", modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
fun SoundPackRowCard(
    pack: SoundPack,
    isPreviewing: Boolean,
    isFavorite: Boolean,
    onOpen: () -> Unit,
    onPreview: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(NavyCard)
            .border(1.dp, if (isPreviewing) AccentCyan else NavyBorder, RoundedCornerShape(16.dp))
            .clickable { onOpen() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(Color(pack.gradientStart), Color(pack.gradientEnd))))
                .clickable { onPreview() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPreviewing) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = "Preview",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(pack.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (pack.isCustom) {
                    Text(
                        "CUSTOM",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black,
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(AccentYellow).padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
            Text("${pack.genre.label} • ${pack.bpm} BPM • ${pack.artist}", fontSize = 11.sp, color = AccentCyan, maxLines = 1)
            Text(pack.subtitle, fontSize = 11.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (isFavorite) AccentPink else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFB703))
                .clickable { onOpen() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Bolt, contentDescription = "Load Pack", tint = Color.Black, modifier = Modifier.size(18.dp))
        }
    }
}
