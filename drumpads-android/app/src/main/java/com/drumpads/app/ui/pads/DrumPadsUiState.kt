package com.drumpads.app.ui.pads

import com.drumpads.app.domain.model.Pad

data class DrumPadsUiState(
    val pads: List<Pad> = emptyList(),
    val activePads: Set<Int> = emptySet(),
    val isHapticEnabled: Boolean = true,
    val isStressTestActive: Boolean = false,
    val stressSecondsRemaining: Int = 0,
    val xrunCount: Int = 0,
    val isSettingsOpen: Boolean = false
)
