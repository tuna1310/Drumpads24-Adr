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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drumpads.app.domain.model.CommunityPost
import com.drumpads.app.domain.model.MusicGenre
import com.drumpads.app.ui.pads.ActiveScreen
import com.drumpads.app.ui.pads.DrumPadsUiState
import com.drumpads.app.ui.pads.DrumPadsViewModel
import com.drumpads.app.ui.theme.*

@Composable
fun FeedScreen(
    uiState: DrumPadsUiState,
    viewModel: DrumPadsViewModel,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("Featured") }
    var selectedGenre by remember { mutableStateOf(MusicGenre.ALL) }
    var activeCommentPost by remember { mutableStateOf<CommunityPost?>(null) }
    var commentDraft by remember { mutableStateOf("") }

    val filterTabs = listOf("Featured", "Following", "Latest")
    val genreFilters = listOf(MusicGenre.ALL, MusicGenre.TRAP, MusicGenre.PHONK, MusicGenre.HIP_HOP, MusicGenre.DUBSTEP, MusicGenre.LOFI, MusicGenre.EDM)

    val displayedPosts = remember(uiState.feedPosts, selectedFilter, selectedGenre, uiState.followedAuthors) {
        uiState.feedPosts.filter { post ->
            val genreOk = selectedGenre == MusicGenre.ALL || post.genre == selectedGenre
            val tabOk = when (selectedFilter) {
                "Following" -> uiState.followedAuthors.contains(post.authorName) || post.isUserPost
                "Latest" -> true
                else -> true
            }
            genreOk && tabOk
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground),
        contentPadding = PaddingValues(bottom = 145.dp)
    ) {
        // 1. Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Feed", fontSize = 24.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                    Text("Beat Maker Crew • Community Sessions", fontSize = 11.sp, color = TextSecondary)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { viewModel.navigateTo(ActiveScreen.RECORDINGS) },
                        modifier = Modifier.size(38.dp).clip(CircleShape).background(NavyCard)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Post Beat", tint = AccentYellow, modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = { viewModel.selectTab(com.drumpads.app.ui.pads.NavTab.SOUNDS) },
                        modifier = Modifier.size(38.dp).clip(CircleShape).background(NavyCard)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Explore", tint = TextPrimary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // 2. Filter Tabs: Featured | Following | Latest
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                filterTabs.forEach { tab ->
                    val isSel = selectedFilter == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSel) NavyCardElevated else NavyCard)
                            .border(1.dp, if (isSel) AccentCyan else NavyBorder, RoundedCornerShape(12.dp))
                            .clickable { selectedFilter = tab }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(tab, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isSel) TextPrimary else TextSecondary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 3. Genre Chips
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(genreFilters) { genre ->
                    val isSel = selectedGenre == genre
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSel) Color(genre.accentColor) else NavyCard)
                            .clickable { selectedGenre = genre }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(genre.label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.Black else Color.White)
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 4. Community Posts & Beat Maker Crew Tutorials
        items(displayedPosts, key = { it.id }) { post ->
            val isPlaying = uiState.playingPostId == post.id
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 7.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(NavyCard)
                    .border(
                        width = if (post.isCrewTutorial) 1.5.dp else 1.dp,
                        color = if (post.isCrewTutorial) AccentCyan.copy(alpha = 0.65f) else NavyBorder,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(16.dp)
            ) {
                // Author Row + Follow Button
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(post.avatarStart), Color(post.avatarEnd)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(post.authorInitials, fontWeight = FontWeight.Black, color = Color.White, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(post.authorName, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            if (post.isCrewTutorial) {
                                Text(
                                    "CREW TIP",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black,
                                    modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(AccentCyan).padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text("${post.timeAgo} • ${post.genre.label}", fontSize = 11.sp, color = TextSecondary)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (post.isFollowingAuthor) NavyCardElevated else AccentCyan)
                            .clickable { viewModel.toggleFollowAuthor(post.authorName) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (post.isFollowingAuthor) "Following" else "+ Follow",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (post.isFollowingAuthor) AccentCyan else Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(post.trackTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(post.caption, fontSize = 13.sp, color = TextSecondary, lineHeight = 19.sp)
                Spacer(modifier = Modifier.height(12.dp))

                // Interactive Audio Preview Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFF1F1C2C), Color(0xFF3A2D6A))))
                        .clickable { viewModel.togglePlayPost(post) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = AccentYellow,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (isPlaying) "Playing live beat..." else "Tap to listen performance", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("${post.genre.label} • ${post.bpm} BPM", fontSize = 11.sp, color = AccentCyan)
                    }
                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = if (isPlaying) AccentYellow else TextMuted)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Signature Action CTA matching Play Store "EXPLORE COMMUNITY": Download & Jam OR Practice Lightshow Lesson!
                if (post.isCrewTutorial) {
                    Button(
                        onClick = { viewModel.startCrewTutorial(post) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.School, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PRACTICE INTERACTIVE LESSON", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                } else {
                    Button(
                        onClick = { viewModel.openPackById(post.packId) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB703)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("DOWNLOAD & JAM SOUND PACK", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Social Actions: Like | Comments | Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.clickable { viewModel.toggleLikePost(post.id) }.padding(4.dp)
                    ) {
                        Icon(
                            imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (post.isLiked) AccentPink else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text("${post.likesCount}", fontSize = 13.sp, color = if (post.isLiked) AccentPink else TextSecondary, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.clickable { activeCommentPost = post }.padding(4.dp)
                    ) {
                        Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Comments", tint = AccentCyan, modifier = Modifier.size(19.dp))
                        Text("${post.comments.size} Comments", fontSize = 13.sp, color = TextSecondary)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.clickable { viewModel.sharePost(post) }.padding(4.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = AccentYellow, modifier = Modifier.size(19.dp))
                        Text("Share", fontSize = 13.sp, color = TextSecondary)
                    }
                }
            }
        }
    }

    // Comments Bottom / Alert Dialog
    if (activeCommentPost != null) {
        val freshPost = uiState.feedPosts.firstOrNull { it.id == activeCommentPost!!.id } ?: activeCommentPost!!
        AlertDialog(
            onDismissRequest = { activeCommentPost = null },
            containerColor = NavyCard,
            title = { Text("Comments (${freshPost.comments.size})", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (freshPost.comments.isEmpty()) {
                        Text("No comments yet. Be the first beatmaker to reply!", color = TextSecondary, fontSize = 13.sp)
                    } else {
                        freshPost.comments.takeLast(5).forEach { c ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NavyCardElevated)
                                    .padding(10.dp)
                            ) {
                                Text("${c.author} • ${c.timeAgo}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentCyan)
                                Text(c.text, fontSize = 13.sp, color = Color.White)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = commentDraft,
                        onValueChange = { commentDraft = it },
                        label = { Text("Write a comment...", color = TextSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addCommentToPost(freshPost.id, commentDraft)
                    commentDraft = ""
                }) { Text("Post", color = AccentCyan, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { activeCommentPost = null }) { Text("Close", color = TextSecondary) }
            }
        )
    }
}
