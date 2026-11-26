package com.drumpads.app.ui.pads

import com.drumpads.app.core.audio.DspEffectParams
import com.drumpads.app.domain.model.*

enum class NavTab {
    SOUNDS, FEED, MY, ACCOUNT
}

enum class ActiveScreen {
    MAIN,
    PAD_PLAYER,
    PAD_EDITOR,
    SOUND_EDITOR,
    PROJECT_EDITOR,
    CLIP_EDITOR,
    RECORDINGS,
    SETTINGS,
    EDIT_PROFILE,
    RECENTLY_USED,
    FAVORITES,
    PAYWALL
}

enum class PadBank {
    BANK_A, BANK_B, FULL_24
}

data class TutorialSessionState(
    val postId: String,
    val title: String,
    val steps: List<TutorialStep>,
    val currentStepIndex: Int = 0,
    val hitsCorrect: Int = 0,
    val totalAttempts: Int = 0,
    val isCompleted: Boolean = false
) {
    val targetPadIndex: Int? get() = steps.getOrNull(currentStepIndex)?.padIndex
    val accuracyPercent: Int get() = if (totalAttempts == 0) 100 else ((hitsCorrect * 100) / totalAttempts).coerceIn(0, 100)
}

data class DrumPadsUiState(
    // Pads & Active Pack
    val pads: List<Pad> = emptyList(),
    val activePads: Set<Int> = emptySet(),
    val activePackId: String = "hyphy_trap",
    val selectedPackName: String = "Hyphy",
    val activeGenre: MusicGenre = MusicGenre.TRAP,

    // Sound Library
    val allSoundPacks: List<SoundPack> = SoundPackCatalog.allPacks,
    val customSoundPacks: List<SoundPack> = emptyList(),
    val selectedLibraryGenre: MusicGenre = MusicGenre.ALL,
    val librarySearchQuery: String = "",
    val favoritePackIds: Set<String> = emptySet(),
    val recentPackIds: List<String> = listOf("hyphy_trap", "neon_pulse_edm", "tokyo_drift_phonk", "lofi_sunset"),
    val previewingPackId: String? = null,

    // Navigation
    val selectedTab: NavTab = NavTab.SOUNDS,
    val activeScreen: ActiveScreen = ActiveScreen.MAIN,

    // Pad Player & Clips Mode
    val selectedBank: PadBank = PadBank.BANK_A,
    val bpm: Int = 138,
    val isClipsMode: Boolean = false,
    val isPadLocked: Boolean = false,
    val isHoldMode: Boolean = false,
    val isMetronomeEnabled: Boolean = false,
    val showPadLabels: Boolean = true,
    val isHapticEnabled: Boolean = true,
    val isLightShowEnabled: Boolean = true,

    // Clips & Step Sequencer (12 real editable clip slots!)
    val clips: List<ClipSlot> = ClipPatternPresets.buildDefaultClips(MusicGenre.TRAP),
    val activeClips: Set<Int> = emptySet(),
    val selectedClipIndex: Int = 0,
    val currentSequencerStep: Int = 0,
    val isClipPlaying: Boolean = false,

    // Real Recording & Audio Export
    val isRecording: Boolean = false,
    val recordingMode: RecordingMode = RecordingMode.FREE,
    val recordingDurationSeconds: Int = 0,
    val recordings: List<RecordingItem> = emptyList(),
    val playingRecordingId: String? = null,
    val recordingPlaybackProgress: Float = 0f,
    val isExportingVideo: Boolean = false,

    // Pad Editor & Sound Waveform Editor
    val editingPadIndex: Int = 0,
    val editorWaveformPeaks: List<Float> = emptyList(),
    val editorSampleDurationSec: Float = 0.5f,
    val isMicRecordingSample: Boolean = false,
    val micPeakLevel: Float = 0f,

    // Real 10-FX DSP Engine Controls
    val isSoundFxOpen: Boolean = false,
    val activeQuickFx: String? = null,
    val dspParams: DspEffectParams = DspEffectParams(),
    val livePeakLevel: Float = 0f,

    // Mini Player
    val miniPlayerTrack: String? = "21 savage x baby keem x drake type beat",
    val miniPlayerSubtitle: String = "Classified Beatz • 92 BPM",
    val miniPlayerDuration: String = "03:25",
    val isMiniPlayerPlaying: Boolean = false,
    val playingPostId: String? = null,

    // Community Feed & Beat Maker Crew Tutorials
    val feedPosts: List<CommunityPost> = CommunityCatalog.initialPosts,
    val followedAuthors: Set<String> = setOf("Beat Maker Crew"),
    val activeTutorial: TutorialSessionState? = null,

    // User Profile & Settings
    val userName: String = "Tuấn Phạm Anh",
    val userBio: String = "Beat maker & Finger drummer",
    val userLink: String = "https://instagram.com/tuanphamanh",
    val followersCount: Int = 12,
    val isProUser: Boolean = true,
    val isSettingsOpen: Boolean = false,
    val isStressTestActive: Boolean = false,
    val stressSecondsRemaining: Int = 0,
    val xrunCount: Int = 0,

    // User Feedback Toast / Banner
    val statusMessage: String? = null
)
