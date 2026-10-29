package com.drumpads.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val stories = listOf(
        Pair("Kilimanjaro", listOf(Color(0xFFFF9966), Color(0xFFFF5E62))),
        Pair("Rain Dance", listOf(Color(0xFF2193B0), Color(0xFF6DD5ED))),
        Pair("Equilibrium", listOf(Color(0xFF8A2387), Color(0xFFE94057))),
        Pair("Medium Rare", listOf(Color(0xFFF12711), Color(0xFFF5AF19))),
        Pair("Heavenly", listOf(Color(0xFF00C9FF), Color(0xFF92FE9D))),
        Pair("Cyber Drift", listOf(Color(0xFF4776E6), Color(0xFF8E54E9)))
    )

    val newPacks = listOf(
        Triple("You Are Dead", "DrumPads 24", listOf(Color(0xFFF857A6), Color(0xFFFF5858))),
        Triple("Bleed", "DrumPads 24", listOf(Color(0xFFFF0844), Color(0xFFFFB199))),
        Triple("Zombie Land", "DrumPads 24", listOf(Color(0xFF11998E), Color(0xFF38EF7D))),
        Triple("Retro City", "Neon Synth", listOf(Color(0xFF5F2C82), Color(0xFF49A09D)))
    )

    val upcomingPacks = listOf("Cocoon", "Dancing", "Dark Matter", "Sunset Chill")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground),
        contentPadding = PaddingValues(bottom = 90.dp)
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
                Text(
                    text = "Drum Pads 24",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = AccentCyan
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { viewModel.openPadPlayer("Cyber Neon 24") },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(NavyCard)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Random Pack",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.navigateTo(ActiveScreen.PAYWALL) },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(NavyCard)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 2. Horizontal Stories Row
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(stories) { (name, gradient) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { viewModel.openPadPlayer(name) }
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
                            Text(
                                text = name.take(2).uppercase(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // 3. Recently Used Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recently used",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "See all >",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentYellow,
                    modifier = Modifier.clickable { viewModel.navigateTo(ActiveScreen.RECENTLY_USED) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Recently used card banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(NavyCard)
                    .border(1.dp, NavyBorder, RoundedCornerShape(18.dp))
                    .clickable { viewModel.openPadPlayer("Hyphy") }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFFE94057), Color(0xFFF27121)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hyphy",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Drum Pads 24 • 120 BPM • 24 Pads",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Open",
                    tint = TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // 4. New Sound Packs Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "New",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "See all >",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentYellow,
                    modifier = Modifier.clickable { viewModel.navigateTo(ActiveScreen.PAYWALL) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(newPacks) { (title, subtitle, gradient) ->
                    Column(
                        modifier = Modifier
                            .width(136.dp)
                            .clickable { viewModel.openPadPlayer(title) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(136.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.linearGradient(gradient))
                                .padding(10.dp)
                        ) {
                            // Pro badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(ProBadgeColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Pro",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            // Center title on artwork
                            Text(
                                text = title.uppercase(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                modifier = Modifier.align(Alignment.Center)
                            )

                            // Play preview button
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitle,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // 5. Upcoming Section
        item {
            Text(
                text = "Upcoming",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(upcomingPacks) { name ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(NavyCard)
                            .border(1.dp, NavyBorder, RoundedCornerShape(12.dp))
                            .clickable { viewModel.toggleMiniPlayer() }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(NavyCardElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = AccentCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

