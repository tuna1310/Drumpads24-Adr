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

private data class QuickToolItem(
    val title: String,
    val icon: ImageVector,
    val iconColor: Color
)

private data class LibraryMenuItem(
    val title: String,
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
        QuickToolItem("Extract vocals with Unmix", Icons.Default.Star, Color(0xFF4361EE)),
        QuickToolItem("Create sound pack", Icons.Default.Add, Color(0xFFF77F00)),
        QuickToolItem("Edit sounds with Audio Editor", Icons.Default.Edit, Color(0xFFD62246)),
        QuickToolItem("Open sequencer", Icons.Default.Menu, Color(0xFF06D6A0))
    )

    val libraryItems = listOf(
        LibraryMenuItem("Recordings", Icons.Default.PlayArrow) { viewModel.openPadPlayer("Recordings Session") },
        LibraryMenuItem("Projects", Icons.Default.Info) { viewModel.openPadPlayer("Project 1") },
        LibraryMenuItem("Favorite sound packs", Icons.Default.Favorite) { viewModel.navigateTo(ActiveScreen.PAYWALL) },
        LibraryMenuItem("Downloaded sound packs", Icons.Default.Check) { viewModel.navigateTo(ActiveScreen.RECENTLY_USED) },
        LibraryMenuItem("Recently used", Icons.Default.Refresh) { viewModel.navigateTo(ActiveScreen.RECENTLY_USED) }
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // 1. Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "My",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }

        // 2. Quick Action Cards (Horizontal scroll, exactly matching Image 6)
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(quickTools) { item ->
                    Column(
                        modifier = Modifier
                            .width(130.dp)
                            .height(130.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(NavyCard)
                            .border(1.dp, NavyBorder, RoundedCornerShape(16.dp))
                            .clickable { viewModel.openPadPlayer(item.title) }
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
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = item.iconColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Text(
                            text = item.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
        }

        // 3. Library List Items
        items(libraryItems) { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { item.onClick() }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = TextSecondary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = item.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = TextSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
