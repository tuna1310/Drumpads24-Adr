package com.drumpads.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drumpads.app.ui.pads.ActiveScreen
import com.drumpads.app.ui.pads.DrumPadsUiState
import com.drumpads.app.ui.pads.DrumPadsViewModel
import com.drumpads.app.ui.theme.*

@Composable
fun RecentlyUsedScreen(
    uiState: DrumPadsUiState,
    viewModel: DrumPadsViewModel,
    favoritesOnly: Boolean = false,
    modifier: Modifier = Modifier
) {
    val packsToShow = remember(uiState.allSoundPacks, uiState.recentPackIds, uiState.favoritePackIds, favoritesOnly) {
        if (favoritesOnly) {
            uiState.allSoundPacks.filter { uiState.favoritePackIds.contains(it.id) }
        } else {
            uiState.recentPackIds.mapNotNull { id -> uiState.allSoundPacks.firstOrNull { it.id == id } }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateTo(ActiveScreen.MAIN) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text(
                text = if (favoritesOnly) "Favorite Sound Packs" else "Recently Used",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        if (packsToShow.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No sound packs here yet", color = TextSecondary, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(packsToShow, key = { it.id }) { pack ->
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
    }
}
