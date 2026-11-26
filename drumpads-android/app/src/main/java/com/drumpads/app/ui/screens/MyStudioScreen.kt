package com.drumpads.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drumpads.app.ui.pads.ActiveScreen
import com.drumpads.app.ui.pads.DrumPadsUiState
import com.drumpads.app.ui.pads.DrumPadsViewModel
import com.drumpads.app.ui.theme.*

private data class StudioQuickTool(
    val title: String,
    val icon: ImageVector,
    val iconColor: Color,
    val onClick: () -> Unit
)

private data class StudioLibraryRow(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun MyStudioScreen(
    uiState: DrumPadsUiState,
    viewModel: DrumPadsViewModel,
    modifier: Modifier = Modifier
) {
    val quickTools = listOf(
        StudioQuickTool("Create sound pack", Icons.Default.AddCircle, Color(0xFFF77F00)) {
            viewModel.navigateTo(ActiveScreen.PROJECT_EDITOR)
        },
        StudioQuickTool("Edit pads & sounds", Icons.Default.GridView, Color(0xFFD62246)) {
            viewModel.openPadEditor(0)
        },
        StudioQuickTool("Waveform & Mic Editor", Icons.Default.GraphicEq, AccentCyan) {
            viewModel.openSoundEditor(0)
        },
        StudioQuickTool("Open step sequencer", Icons.Default.Tune, Color(0xFF06D6A0)) {
            viewModel.openClipEditor(0)
        }
    )

    val libraryItems = listOf(
        StudioLibraryRow(
            "Recordings (${uiState.recordings.size})",
            "Play, rename, export WAV or 9:16 MP4 Video",
            Icons.Default.Mic
        ) { viewModel.navigateTo(ActiveScreen.RECORDINGS) },

        StudioLibraryRow(
            "Projects / Custom Packs (${uiState.customSoundPacks.size})",
            "Build and manage your own 24-pad kits",
            Icons.Default.Folder
        ) { viewModel.navigateTo(ActiveScreen.PROJECT_EDITOR) },

        StudioLibraryRow(
            "Favorite sound packs (${uiState.favoritePackIds.size})",
            "Quick access to starred beats",
            Icons.Default.Favorite
        ) { viewModel.navigateTo(ActiveScreen.FAVORITES) },

        StudioLibraryRow(
            "Recently used (${uiState.recentPackIds.size})",
            "Jump right back into recent sessions",
            Icons.Default.Refresh
        ) { viewModel.navigateTo(ActiveScreen.RECENTLY_USED) }
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground),
        contentPadding = PaddingValues(bottom = 18.dp)
    ) {
        item {
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("My Studio", fontSize = 20.sp, fontWeight = FontWeight.Black, color = TextPrimary)
            }
        }

        // Studio Creation Tools
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(quickTools) { item ->
                    Column(
                        modifier = Modifier
                            .width(132.dp)
                            .height(126.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(NavyCard)
                            .border(1.dp, NavyBorder, RoundedCornerShape(16.dp))
                            .clickable { item.onClick() }
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(item.iconColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(item.icon, contentDescription = null, tint = item.iconColor, modifier = Modifier.size(22.dp))
                        }
                        Text(item.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary, lineHeight = 16.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Library Rows
        items(libraryItems) { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 5.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(NavyCard)
                    .border(1.dp, NavyBorder, RoundedCornerShape(16.dp))
                    .clickable { item.onClick() }
                    .padding(horizontal = 16.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(item.icon, contentDescription = item.title, tint = AccentYellow, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(item.subtitle, fontSize = 11.sp, color = TextSecondary)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
            }
        }
    }
}
