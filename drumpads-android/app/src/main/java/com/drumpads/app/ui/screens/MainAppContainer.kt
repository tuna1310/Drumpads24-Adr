package com.drumpads.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drumpads.app.ui.components.AppBottomNavBar
import com.drumpads.app.ui.components.MiniPlayerBar
import com.drumpads.app.ui.pads.ActiveScreen
import com.drumpads.app.ui.pads.DrumPadsViewModel
import com.drumpads.app.ui.pads.NavTab
import com.drumpads.app.ui.theme.*

@Composable
fun MainAppContainer(
    viewModel: DrumPadsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Hardware back button navigates cleanly between sub-screens
    BackHandler(enabled = uiState.activeScreen != ActiveScreen.MAIN) {
        when (uiState.activeScreen) {
            ActiveScreen.SOUND_EDITOR -> viewModel.navigateTo(ActiveScreen.PAD_EDITOR)
            ActiveScreen.PAD_EDITOR, ActiveScreen.CLIP_EDITOR -> viewModel.navigateTo(ActiveScreen.PAD_PLAYER)
            else -> viewModel.navigateTo(ActiveScreen.MAIN)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NavyBackground)
    ) {
        AnimatedContent(
            targetState = uiState.activeScreen,
            label = "activeScreenTransition"
        ) { screen ->
            when (screen) {
                ActiveScreen.MAIN -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(NavyBackground)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .statusBarsPadding()
                        ) {
                            when (uiState.selectedTab) {
                                NavTab.SOUNDS -> SoundsScreen(uiState = uiState, viewModel = viewModel)
                                NavTab.FEED -> FeedScreen(uiState = uiState, viewModel = viewModel)
                                NavTab.MY -> MyStudioScreen(uiState = uiState, viewModel = viewModel)
                                NavTab.ACCOUNT -> AccountScreen(uiState = uiState, viewModel = viewModel)
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NavySurface)
                                .navigationBarsPadding(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(NavyBorder.copy(alpha = 0.7f))
                            )
                            if (uiState.miniPlayerTrack != null) {
                                MiniPlayerBar(
                                    trackTitle = uiState.miniPlayerTrack!!,
                                    duration = uiState.miniPlayerDuration,
                                    isPlaying = uiState.isMiniPlayerPlaying,
                                    onTogglePlay = { viewModel.toggleMiniPlayer() },
                                    onNext = { viewModel.nextMiniPlayerTrack() }
                                )
                            }
                            AppBottomNavBar(
                                selectedTab = uiState.selectedTab,
                                onTabSelected = { viewModel.selectTab(it) }
                            )
                        }
                    }
                }
                ActiveScreen.PAD_PLAYER -> PadPlayerScreen(uiState = uiState, viewModel = viewModel)
                ActiveScreen.PAD_EDITOR -> PadEditorScreen(uiState = uiState, viewModel = viewModel)
                ActiveScreen.SOUND_EDITOR -> SoundEditorScreen(uiState = uiState, viewModel = viewModel)
                ActiveScreen.PROJECT_EDITOR -> ProjectEditorScreen(uiState = uiState, viewModel = viewModel)
                ActiveScreen.CLIP_EDITOR -> ClipEditorScreen(uiState = uiState, viewModel = viewModel)
                ActiveScreen.RECORDINGS -> RecordingsScreen(uiState = uiState, viewModel = viewModel)
                ActiveScreen.SETTINGS -> SettingsScreen(uiState = uiState, viewModel = viewModel)
                ActiveScreen.EDIT_PROFILE -> EditProfileScreen(uiState = uiState, viewModel = viewModel)
                ActiveScreen.RECENTLY_USED -> RecentlyUsedScreen(uiState = uiState, viewModel = viewModel, favoritesOnly = false)
                ActiveScreen.FAVORITES -> RecentlyUsedScreen(uiState = uiState, viewModel = viewModel, favoritesOnly = true)
                ActiveScreen.PAYWALL -> PaywallScreen(viewModel = viewModel)
            }
        }

        // Global Status Toast Banner
        val msg = uiState.statusMessage
        if (msg != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 44.dp, start = 20.dp, end = 20.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(NavyCardElevated.copy(alpha = 0.96f))
                    .border(1.dp, AccentCyan, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(msg, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
