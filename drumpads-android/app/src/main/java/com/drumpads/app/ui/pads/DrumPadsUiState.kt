package com.drumpads.app.ui.pads

import com.drumpads.app.domain.model.Pad

enum class NavTab {
    SOUNDS, FEED, MY, ACCOUNT
}

enum class ActiveScreen {
    MAIN, PAD_PLAYER, SETTINGS, EDIT_PROFILE, RECENTLY_USED, PAYWALL
}

enum class PadBank {
    BANK_A, BANK_B, FULL_24
}

data class SoundPackItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val genre: String,
    val isPro: Boolean = false,
    val colorHex: String = "#FF416C",
    val bpm: Int = 120
)

data class FeedPost(
    val id: String,
    val authorName: String,
    val timeAgo: String,
    val caption: String,
    val bpm: Int,
    val trackTitle: String,
    val genre: String,
    val likesCount: Int = 42
)

data class DrumPadsUiState(
    val pads: List<Pad> = emptyList(),
    val activePads: Set<Int> = emptySet(),
    val isHapticEnabled: Boolean = true,
    val isLightShowEnabled: Boolean = true,
    val isStressTestActive: Boolean = false,
    val stressSecondsRemaining: Int = 0,
    val xrunCount: Int = 0,
    val isSettingsOpen: Boolean = false,

    // Navigation
    val selectedTab: NavTab = NavTab.SOUNDS,
    val activeScreen: ActiveScreen = ActiveScreen.MAIN,

    // Pad Player Mode
    val selectedBank: PadBank = PadBank.BANK_A,
    val selectedPackName: String = "Neon Pulse 24",
    val bpm: Int = 120,
    val isRecording: Boolean = false,
    val isClipsMode: Boolean = false,
    val isPadLocked: Boolean = false,
    val activeClips: Set<Int> = setOf(0),
    val selectedClipIndex: Int = 0,
    val isSoundFxOpen: Boolean = false,
    val activeQuickFx: String? = null,
    val volumeLevel: Float = 0.85f,
    val echo116Level: Float = 0.3f,
    val echo18Level: Float = 0.0f,
    val reverbLevel: Float = 0.45f,
    val distortionLevel: Float = 0.0f,

    // Mini Player
    val miniPlayerTrack: String? = "21 savage x baby keem x drake type beat",
    val miniPlayerDuration: String = "03:25",
    val isMiniPlayerPlaying: Boolean = false,

    // Profile
    val userName: String = "Tuấn Phạm Anh",
    val userBio: String = "Beat maker & Sound designer",
    val userLink: String = "https://instagram.com/tuanphamanh",
    val followersCount: Int = 0,
    val followingCount: Int = 1,
    val isProUser: Boolean = false
)
