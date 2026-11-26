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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drumpads.app.ui.pads.ActiveScreen
import com.drumpads.app.ui.pads.DrumPadsUiState
import com.drumpads.app.ui.pads.DrumPadsViewModel
import com.drumpads.app.ui.theme.*

@Composable
fun AccountScreen(
    uiState: DrumPadsUiState,
    viewModel: DrumPadsViewModel,
    modifier: Modifier = Modifier
) {
    val myPosts = remember(uiState.feedPosts) { uiState.feedPosts.filter { it.isUserPost } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 18.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(uiState.userName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NavyCard)
                                .border(1.dp, NavyBorder, CircleShape)
                                .clickable { viewModel.navigateTo(ActiveScreen.EDIT_PROFILE) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = TextPrimary, modifier = Modifier.size(18.dp))
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NavyCard)
                                .border(1.dp, NavyBorder, CircleShape)
                                .clickable { viewModel.navigateTo(ActiveScreen.SETTINGS) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextPrimary, modifier = Modifier.size(19.dp))
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(NavyCard)
                        .border(1.dp, NavyBorder, RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(64.dp).clip(CircleShape).background(AccentYellow),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Headphones, contentDescription = "Avatar", tint = Color.Black, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(uiState.userName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        if (uiState.userBio.isNotBlank()) {
                            Text(uiState.userBio, fontSize = 12.sp, color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${uiState.followersCount} Followers   ${uiState.followedAuthors.size} Following   ${myPosts.size} Posts",
                            fontSize = 12.sp,
                            color = AccentCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (myPosts.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No published beats yet.", fontSize = 14.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Record a beat and tap 'Post Feed' in My Recordings!",
                            fontSize = 12.sp,
                            color = AccentCyan,
                            modifier = Modifier.clickable { viewModel.navigateTo(ActiveScreen.RECORDINGS) }
                        )
                    }
                }
            } else {
                item {
                    Text("MY PUBLISHED BEATS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                }
                items(myPosts, key = { it.id }) { post ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(NavyCard)
                            .clickable { viewModel.togglePlayPost(post) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PlayCircleFilled, contentDescription = null, tint = AccentYellow, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(post.trackTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("${post.genre.label} • ${post.bpm} BPM • ${post.likesCount} likes", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { viewModel.navigateTo(ActiveScreen.PAD_PLAYER) },
            containerColor = AccentYellow,
            contentColor = Color.Black,
            shape = CircleShape,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 148.dp).size(54.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Open Pad Player", modifier = Modifier.size(28.dp))
        }
    }
}
