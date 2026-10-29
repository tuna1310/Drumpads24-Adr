package com.drumpads.app.ui.pads

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drumpads.app.core.audio.AudioEngine
import com.drumpads.app.core.haptic.HapticManager
import com.drumpads.app.domain.repository.PadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject

@HiltViewModel
class DrumPadsViewModel @Inject constructor(
    private val padRepository: PadRepository,
    private val audioEngine: AudioEngine,
    private val hapticManager: HapticManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DrumPadsUiState(pads = padRepository.getPads()))
    val uiState: StateFlow<DrumPadsUiState> = _uiState.asStateFlow()

    private var stressJob: Job? = null

    init {
        audioEngine.init()
        audioEngine.start()
        loadStarterSamples()
    }

    private fun loadStarterSamples() {
        viewModelScope.launch(Dispatchers.IO) {
            val pads = padRepository.getPads()
            for (pad in pads) {
                try {
                    context.assets.open(pad.samplePath).use { stream ->
                        val pcm = parseWavPcm(stream)
                        if (pcm != null && pcm.isNotEmpty()) {
                            audioEngine.loadSample(pad.index, pcm)
                        }
                    }
                } catch (_: Throwable) {
                    // AudioEngine synthesized fallback handles this automatically
                }
            }
        }
    }

    private fun parseWavPcm(stream: InputStream): ShortArray? {
        val bytes = stream.readBytes()
        if (bytes.size < 44) return null
        var offset = 12
        while (offset + 8 <= bytes.size) {
            val chunkId = String(bytes, offset, 4)
            val chunkSize = ByteBuffer.wrap(bytes, offset + 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
            if (chunkId == "data") {
                val dataOffset = offset + 8
                val numShorts = minOf(chunkSize, bytes.size - dataOffset) / 2
                val shortBuf = ByteBuffer.wrap(bytes, dataOffset, numShorts * 2).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                val result = ShortArray(numShorts)
                shortBuf.get(result)
                return result
            }
            offset += 8 + chunkSize
        }
        return null
    }

    fun onPadDown(padIndex: Int) {
        audioEngine.triggerPad(padIndex)
        if (_uiState.value.isHapticEnabled) {
            hapticManager.playPadHaptic()
        }
        _uiState.update { current ->
            current.copy(activePads = current.activePads + padIndex)
        }
    }

    fun onPadUp(padIndex: Int) {
        _uiState.update { current ->
            current.copy(activePads = current.activePads - padIndex)
        }
    }

    // Navigation methods
    fun selectTab(tab: NavTab) {
        _uiState.update { it.copy(selectedTab = tab, activeScreen = ActiveScreen.MAIN) }
    }

    fun navigateTo(screen: ActiveScreen) {
        _uiState.update { it.copy(activeScreen = screen) }
    }

    fun openPadPlayer(packName: String = "Neon Pulse 24") {
        _uiState.update {
            it.copy(
                selectedPackName = packName,
                activeScreen = ActiveScreen.PAD_PLAYER
            )
        }
    }

    fun selectBank(bank: PadBank) {
        _uiState.update { it.copy(selectedBank = bank) }
    }

    fun toggleClipsMode() {
        val next = !_uiState.value.isClipsMode
        setClipsMode(next)
    }

    fun setClipsMode(isClips: Boolean) {
        _uiState.update { it.copy(isClipsMode = isClips) }
        if (isClips && _uiState.value.activeClips.isNotEmpty()) {
            startClipLoop()
        }
    }

    fun setPadLocked(locked: Boolean) {
        _uiState.update { it.copy(isPadLocked = locked) }
    }

    fun adjustBpm(delta: Int) {
        _uiState.update {
            val newBpm = (it.bpm + delta).coerceIn(60, 200)
            it.copy(bpm = newBpm)
        }
        if (clipLoopJob?.isActive == true) {
            startClipLoop()
        }
    }

    fun stopPlayback() {
        clipLoopJob?.cancel()
        _uiState.update { it.copy(activeClips = emptySet()) }
    }

    fun toggleClip(clipIndex: Int) {
        _uiState.update { current ->
            val next = if (current.activeClips.contains(clipIndex)) {
                current.activeClips - clipIndex
            } else {
                current.activeClips + clipIndex
            }
            current.copy(activeClips = next, selectedClipIndex = clipIndex)
        }
        if (_uiState.value.activeClips.isNotEmpty()) {
            startClipLoop()
        } else {
            clipLoopJob?.cancel()
        }
    }

    fun selectClip(clipIndex: Int) {
        _uiState.update { it.copy(selectedClipIndex = clipIndex) }
    }

    fun deleteSelectedClip() {
        val sel = _uiState.value.selectedClipIndex
        _uiState.update { it.copy(activeClips = it.activeClips - sel) }
    }

    fun duplicateSelectedClip() {
        val sel = _uiState.value.selectedClipIndex
        val nextSlot = (sel + 1) % 12
        _uiState.update { it.copy(activeClips = it.activeClips + nextSlot, selectedClipIndex = nextSlot) }
        startClipLoop()
    }

    private var clipLoopJob: Job? = null

    private fun startClipLoop() {
        clipLoopJob?.cancel()
        clipLoopJob = viewModelScope.launch(Dispatchers.Default) {
            var step = 0
            while (_uiState.value.activeClips.isNotEmpty()) {
                val bpm = _uiState.value.bpm
                val stepMs = (60_000L / bpm) / 4
                val active = _uiState.value.activeClips
                if (active.contains(0) && (step % 8 == 0 || step % 8 == 6)) {
                    audioEngine.triggerPad(0)
                }
                if (active.contains(1) && (step % 8 == 4)) {
                    audioEngine.triggerPad(2)
                }
                if (active.contains(2) && (step % 2 == 0)) {
                    audioEngine.triggerPad(4)
                }
                if (active.contains(3) && (step % 4 == 0)) {
                    val noteIndex = 12 + ((step / 4) % 6)
                    audioEngine.triggerPad(noteIndex)
                }
                if (active.contains(4) && (step % 8 == 2)) {
                    audioEngine.triggerPad(6)
                }
                if (active.contains(5) && (step % 16 == 12)) {
                    audioEngine.triggerPad(18)
                }

                delay(stepMs)
                step = (step + 1) % 32
            }
        }
    }

    fun triggerQuickFx(fxName: String) {
        if (_uiState.value.isHapticEnabled) {
            hapticManager.playPadHaptic()
        }
        val padIndex = when (fxName.lowercase()) {
            "horn" -> 20
            "elphnt" -> 21
            "reload" -> 19
            "scrtch" -> 22
            "vinyl" -> 23
            "watch" -> 4
            else -> 0
        }
        audioEngine.triggerPad(padIndex)
        _uiState.update { it.copy(activeQuickFx = fxName) }
        viewModelScope.launch {
            delay(250)
            _uiState.update { if (it.activeQuickFx == fxName) it.copy(activeQuickFx = null) else it }
        }
    }

    fun toggleSoundFxTray() {
        _uiState.update { it.copy(isSoundFxOpen = !it.isSoundFxOpen) }
    }

    fun setFaderLevel(fxType: String, level: Float) {
        val clamped = level.coerceIn(0f, 1f)
        _uiState.update {
            when (fxType) {
                "Volume" -> it.copy(volumeLevel = clamped)
                "Echo 1/16" -> it.copy(echo116Level = clamped)
                "Echo 1/8" -> it.copy(echo18Level = clamped)
                "Reverb" -> it.copy(reverbLevel = clamped)
                "Distortion" -> it.copy(distortionLevel = clamped)
                else -> it
            }
        }
    }

    fun toggleRecording() {
        _uiState.update { it.copy(isRecording = !it.isRecording) }
    }

    fun toggleMiniPlayer() {
        _uiState.update { it.copy(isMiniPlayerPlaying = !it.isMiniPlayerPlaying) }
    }

    // Settings & Toggles
    fun setHapticEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isHapticEnabled = enabled) }
    }

    fun setLightShowEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isLightShowEnabled = enabled) }
    }

    fun toggleSettings(open: Boolean) {
        _uiState.update {
            if (open) it.copy(activeScreen = ActiveScreen.SETTINGS, isSettingsOpen = true)
            else it.copy(activeScreen = ActiveScreen.MAIN, isSettingsOpen = false)
        }
    }

    fun updateProfile(name: String, bio: String, link: String) {
        _uiState.update {
            it.copy(userName = name, userBio = bio, userLink = link)
        }
    }

    fun setStressTest(enabled: Boolean) {
        stressJob?.cancel()
        audioEngine.setStressTest(enabled)

        if (enabled) {
            _uiState.update { it.copy(isStressTestActive = true, stressSecondsRemaining = 60) }
            stressJob = viewModelScope.launch(Dispatchers.Default) {
                var remaining = 60
                while (remaining > 0) {
                    delay(1000)
                    remaining--
                    val xruns = audioEngine.getXRunCount()
                    _uiState.update {
                        it.copy(stressSecondsRemaining = remaining, xrunCount = xruns)
                    }
                }
                audioEngine.setStressTest(false)
                _uiState.update { it.copy(isStressTestActive = false, stressSecondsRemaining = 0) }
            }
        } else {
            _uiState.update { it.copy(isStressTestActive = false, stressSecondsRemaining = 0) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stressJob?.cancel()
        clipLoopJob?.cancel()
        audioEngine.stop()
    }
}
