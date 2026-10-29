package com.drumpads.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.drumpads.app.ui.components.AppBottomNavBar
import com.drumpads.app.ui.components.MiniPlayerBar
import com.drumpads.app.ui.pads.ActiveScreen
import com.drumpads.app.ui.pads.DrumPadsViewModel
import com.drumpads.app.ui.pads.NavTab
import com.drumpads.app.ui.theme.NavyBackground

@Composable
fun MainAppContainer(
    viewModel: DrumPadsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    AnimatedContent(
        targetState = uiState.activeScreen,
        label = "activeScreenTransition"
    ) { screen ->
        when (screen) {
            ActiveScreen.MAIN -> {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .background(NavyBackground)
                ) {
                    // Tab content
                    when (uiState.selectedTab) {
                        NavTab.SOUNDS -> SoundsScreen(uiState = uiState, viewModel = viewModel)
                        NavTab.FEED -> FeedScreen(uiState = uiState, viewModel = viewModel)
                        NavTab.MY -> MyStudioScreen(uiState = uiState, viewModel = viewModel)
                        NavTab.ACCOUNT -> AccountScreen(uiState = uiState, viewModel = viewModel)
                    }

                    // Floating Bottom Stack: Mini Player + Bottom Nav Bar
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (uiState.miniPlayerTrack != null) {
                            MiniPlayerBar(
                                trackTitle = uiState.miniPlayerTrack!!,
                                duration = uiState.miniPlayerDuration,
                                isPlaying = uiState.isMiniPlayerPlaying,
                                onTogglePlay = { viewModel.toggleMiniPlayer() }
                            )
                        }

                        AppBottomNavBar(
                            selectedTab = uiState.selectedTab,
                            onTabSelected = { viewModel.selectTab(it) }
                        )
                    }
                }
            }
            ActiveScreen.PAD_PLAYER -> {
                PadPlayerScreen(uiState = uiState, viewModel = viewModel)
            }
            ActiveScreen.SETTINGS -> {
                SettingsScreen(uiState = uiState, viewModel = viewModel)
            }
            ActiveScreen.EDIT_PROFILE -> {
                EditProfileScreen(uiState = uiState, viewModel = viewModel)
            }
            ActiveScreen.RECENTLY_USED -> {
                RecentlyUsedScreen(viewModel = viewModel)
            }
            ActiveScreen.PAYWALL -> {
                PaywallScreen(viewModel = viewModel)
            }
        }
    }
}

