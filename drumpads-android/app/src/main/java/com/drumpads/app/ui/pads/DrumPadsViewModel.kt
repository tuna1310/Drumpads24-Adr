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
        // Wire SoundPool with Context so real WAV playback works on device
        audioEngine.initWithContext(context)
        audioEngine.start()
        loadStarterSamples()
    }

    private fun loadStarterSamples() {
        viewModelScope.launch(Dispatchers.IO) {
            val pads = padRepository.getPads()
            for (pad in pads) {
                // Load into SoundPool (real device playback)
                audioEngine.loadAssetSample(pad.index, pad.samplePath)
                // Also load PCM into synth voice pool (unit test fallback)
                try {
                    context.assets.open(pad.samplePath).use { stream ->
                        val pcm = parseWavPcm(stream)
                        if (pcm != null && pcm.isNotEmpty()) {
                            audioEngine.loadSample(pad.index, pcm)
                        }
                    }
                } catch (_: Throwable) {
                    // Synth fallback already has generated samples for this pad
                }
            }

            // Quick FX dedicated sounds (Horn, Elephant, Reload, Scratch, Vinyl, Watch)
            val quickFxFiles = listOf(
                24 to "starter/samples/fx_horn.wav",
                25 to "starter/samples/fx_elephant.wav",
                26 to "starter/samples/fx_reload.wav",
                27 to "starter/samples/fx_scratch.wav",
                28 to "starter/samples/fx_vinyl.wav",
                29 to "starter/samples/fx_watch.wav"
            )
            for ((idx, path) in quickFxFiles) {
                audioEngine.loadAssetSample(idx, path)
                try {
                    context.assets.open(path).use { stream ->
                        val pcm = parseWavPcm(stream)
                        if (pcm != null && pcm.isNotEmpty()) {
                            audioEngine.loadSample(idx, pcm)
                        }
                    }
                } catch (_: Throwable) {}
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
                // Clip 0: Kick Punch (Syncopated 808 Kick pattern from video)
                if (active.contains(0) && (step % 16 == 0 || step % 16 == 6 || step % 16 == 10 || step % 16 == 14)) {
                    audioEngine.triggerPad(0)
                }
                // Clip 1: Snare / Clap on beat 3 (step 8 & step 24)
                if (active.contains(1) && (step % 16 == 8)) {
                    audioEngine.triggerPad(2)
                }
                // Clip 2: 16th Hi-Hat Groove with roll
                if (active.contains(2) && (step % 2 == 0 || (step % 16 in 14..15))) {
                    audioEngine.triggerPad(4)
                }
                // Clip 3: Melodic Lead Hook
                if (active.contains(3) && (step % 4 == 0)) {
                    val noteIndex = 12 + ((step / 4) % 6)
                    audioEngine.triggerPad(noteIndex)
                }
                // Clip 4: 808 Sub Bass (Deep bassline following kick)
                if (active.contains(4) && (step % 16 == 0 || step % 16 == 6 || step % 16 == 10)) {
                    audioEngine.triggerPad(6)
                }
                // Clip 5: Percussion Tom & Open Hat
                if (active.contains(5) && (step % 16 == 4 || step % 16 == 12)) {
                    audioEngine.triggerPad(18)
                }
                // Clip 6: Vocal Chant ("Hey!" accent)
                if (active.contains(6) && (step % 32 == 28)) {
                    audioEngine.triggerPad(22)
                }
                // Clip 7: Crash & Impact on downbeat
                if (active.contains(7) && (step % 32 == 0)) {
                    audioEngine.triggerPad(20)
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
        val fxIndex = when (fxName.lowercase()) {
            "horn" -> 24
            "elphnt" -> 25
            "reload" -> 26
            "scrtch" -> 27
            "vinyl" -> 28
            "watch" -> 29
            else -> 24
        }
        audioEngine.triggerPad(fxIndex)
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
