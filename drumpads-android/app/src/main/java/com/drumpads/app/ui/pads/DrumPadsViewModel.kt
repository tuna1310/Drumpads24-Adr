package com.drumpads.app.ui.pads

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drumpads.app.core.audio.*
import com.drumpads.app.core.haptic.HapticManager
import com.drumpads.app.data.StudioStorage
import com.drumpads.app.domain.model.*
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class DrumPadsViewModel @Inject constructor(
    private val padRepository: PadRepository,
    private val audioEngine: AudioEngine,
    private val hapticManager: HapticManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val storage = StudioStorage(context)
    private val wavRecorder = WavAudioRecorder(context)
    private val wavPlayer = WavPlaybackController()
    private val micRecorder = MicSampleRecorder()

    // Keeps original uncropped sample buffers so "RESET" in Sound Editor always restores cleanly
    private val originalSampleBuffers = Array<FloatArray?>(24) { null }

    private val _uiState = MutableStateFlow(DrumPadsUiState())
    val uiState: StateFlow<DrumPadsUiState> = _uiState.asStateFlow()

    private var stressJob: Job? = null
    private var clipLoopJob: Job? = null
    private var recordingTimerJob: Job? = null
    private var previewJob: Job? = null
    private var playbackProgressJob: Job? = null
    private var meterJob: Job? = null
    private var toastJob: Job? = null

    private var currentStepAtomic: Int = 0

    init {
        audioEngine.init()
        audioEngine.initWithContext(context)
        audioEngine.start()

        hydratePersistedState()
        loadQuickFxSamples()
        loadPackIntoEngine(SoundPackCatalog.allPacks.first(), openPlayer = false)
        startPeakMeterLoop()
    }

    private fun hydratePersistedState() {
        val savedRecs = storage.loadRecordings()
        val savedCustomPacks = storage.loadCustomPacks()
        val savedFavs = storage.loadStringSet("favorite_packs", setOf("hyphy_trap", "neon_pulse_edm"))
        val savedRecent = storage.loadRecentPackIds()
        val savedFollowed = storage.loadStringSet("followed_authors", setOf("Beat Maker Crew"))
        val savedUserPosts = storage.loadUserPosts()

        val mergedPosts = (savedUserPosts + CommunityCatalog.initialPosts).map { post ->
            post.copy(isFollowingAuthor = savedFollowed.contains(post.authorName))
        }
        val mergedPacks = savedCustomPacks + SoundPackCatalog.allPacks

        _uiState.update {
            it.copy(
                allSoundPacks = mergedPacks,
                customSoundPacks = savedCustomPacks,
                recordings = savedRecs,
                favoritePackIds = savedFavs,
                recentPackIds = savedRecent,
                followedAuthors = savedFollowed,
                feedPosts = mergedPosts,
                userName = storage.loadUserName(),
                userBio = storage.loadUserBio(),
                userLink = storage.loadUserLink()
            )
        }
    }

    private fun startPeakMeterLoop() {
        meterJob?.cancel()
        meterJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                val peak = audioEngine.getPeakLevel()
                if (peak > 0.005f || _uiState.value.livePeakLevel > 0.005f) {
                    _uiState.update { it.copy(livePeakLevel = peak) }
                }
                delay(60)
            }
        }
    }

    private fun loadQuickFxSamples() {
        viewModelScope.launch(Dispatchers.IO) {
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
            }
        }
    }

    /**
     * Loads a Sound Pack's real 24 audio samples (WAV assets for EDM starter, custom saved WAVs
     * for custom packs, or genre-tailored DSP synthesis for each of the 12 genres!) + BPM + Clips.
     */
    fun openSoundPack(pack: SoundPack, startInClips: Boolean = false) {
        stopPreviewAndMiniPlayer()
        loadPackIntoEngine(pack, openPlayer = true, startInClips = startInClips)
    }

    fun openPackById(packId: String, startInClips: Boolean = false) {
        val pack = _uiState.value.allSoundPacks.firstOrNull { it.id == packId }
            ?: SoundPackCatalog.getPackById(packId)
        openSoundPack(pack, startInClips)
    }

    fun openPadPlayer(packName: String = "Hyphy") {
        val matching = _uiState.value.allSoundPacks.firstOrNull {
            it.title.equals(packName, ignoreCase = true)
        }
        if (matching != null) {
            openSoundPack(matching)
        } else {
            _uiState.update {
                it.copy(
                    selectedPackName = packName,
                    activeScreen = ActiveScreen.PAD_PLAYER
                )
            }
        }
    }

    fun openRandomPack() {
        val packs = _uiState.value.allSoundPacks
        val others = packs.filter { it.id != _uiState.value.activePackId }
        val chosen = (if (others.isNotEmpty()) others else packs).random()
        showStatus("Loaded random pack: ${chosen.title} (${chosen.genre.label})")
        openSoundPack(chosen)
    }

    private fun loadPackIntoEngine(pack: SoundPack, openPlayer: Boolean, startInClips: Boolean = false) {
        stopPlayback()
        val packPads = if (pack.pads.size == 24) pack.pads else SoundPackCatalog.buildPadsForGenre(pack.genre)
        val defaultClips = ClipPatternPresets.buildDefaultClips(pack.genre, pack.id)

        // Update recents persistently
        val updatedRecents = (listOf(pack.id) + _uiState.value.recentPackIds.filter { it != pack.id }).take(12)
        storage.saveRecentPackIds(updatedRecents)

        audioEngine.setBpm(pack.bpm)
        audioEngine.setDspParams(_uiState.value.dspParams)

        _uiState.update {
            it.copy(
                activePackId = pack.id,
                selectedPackName = pack.title,
                activeGenre = pack.genre,
                pads = packPads,
                bpm = pack.bpm,
                clips = defaultClips,
                activeClips = emptySet(),
                selectedClipIndex = 0,
                isClipsMode = startInClips,
                recentPackIds = updatedRecents,
                activeTutorial = null,
                activeScreen = if (openPlayer) ActiveScreen.PAD_PLAYER else it.activeScreen
            )
        }

        // Load distinct audio buffers asynchronously on IO dispatcher
        viewModelScope.launch(Dispatchers.IO) {
            val synthesizedKit = GenreSampleSynthesizer.synthesizeKitForGenre(pack.genre, pack.id)
            for (pad in packPads) {
                var loadedBuf: FloatArray? = null

                // 1. Custom imported/recorded WAV file on disk takes highest priority
                if (!pad.customFilePath.isNullOrBlank()) {
                    val f = File(pad.customFilePath)
                    if (f.exists()) {
                        loadedBuf = AudioFileDecoder.parseWavToFloat(f.readBytes())
                    }
                }

                // 2. For Official Starter EDM pack, use the 24 studio WAV asset files
                if (loadedBuf == null && pack.id == "neon_pulse_edm") {
                    try {
                        context.assets.open(pad.samplePath).use { stream ->
                            loadedBuf = AudioFileDecoder.parseWavToFloat(stream.readBytes())
                        }
                    } catch (_: Throwable) {}
                }

                // 3. Otherwise use the authentic genre-specific synthesized sample kit
                val finalBuf = (loadedBuf ?: synthesizedKit[pad.index])
                originalSampleBuffers[pad.index] = finalBuf.copyOf()
                audioEngine.loadFloatSample(pad.index, finalBuf)
                audioEngine.updatePadConfig(pad)
            }
            refreshEditorWaveform(_uiState.value.editingPadIndex)
        }
    }

    // ------------------------------------------------------------------
    // LIVE PAD PLAYBACK, MULTI-TOUCH, HOLD, TUTORIAL & MULTI-LAYER REC
    // ------------------------------------------------------------------
    fun onPadDown(padIndex: Int) {
        if (padIndex !in 0 until 24) return
        val state = _uiState.value
        val pad = state.pads.getOrNull(padIndex)

        // If global HOLD mode is active, treat pad as HOLD mode temporarily
        if (pad != null) {
            val effectivePad = if (state.isHoldMode && pad.playMode == PadPlayMode.ONE_SHOT) {
                pad.copy(playMode = PadPlayMode.HOLD)
            } else {
                pad
            }
            audioEngine.updatePadConfig(effectivePad)
        }

        audioEngine.triggerPad(padIndex)
        if (state.isHapticEnabled) {
            hapticManager.playPadHaptic()
        }

        // Record pad event for MP4 Video Performance visualization
        if (state.isRecording) {
            wavRecorder.recordPadEvent(padIndex)
            // If in Loop Mode (or Clips Mode), stamp the live hit into the active ClipSlot for REAL Multi-Layer Recording!
            if (state.recordingMode == RecordingMode.LOOP || state.isClipsMode) {
                stampLiveHitIntoSelectedClip(padIndex)
            }
        }

        // Check Beat Maker Crew Tutorial progress
        val tut = state.activeTutorial
        if (tut != null && !tut.isCompleted) {
            handleTutorialPadHit(tut, padIndex)
        }

        _uiState.update { current ->
            current.copy(activePads = current.activePads + padIndex)
        }
    }

    fun onPadUp(padIndex: Int) {
        if (padIndex !in 0 until 24) return
        val state = _uiState.value
        val pad = state.pads.getOrNull(padIndex)
        if (state.isHoldMode || pad?.playMode == PadPlayMode.HOLD) {
            audioEngine.releasePad(padIndex)
        }
        _uiState.update { current ->
            current.copy(activePads = current.activePads - padIndex)
        }
    }

    private fun stampLiveHitIntoSelectedClip(padIndex: Int) {
        val state = _uiState.value
        val slotIdx = state.selectedClipIndex.coerceIn(0, 11)
        val step = currentStepAtomic % 16
        val updatedClips = state.clips.toMutableList()
        val slot = updatedClips[slotIdx]
        val hit = ClipHit(step = step, padIndex = padIndex)
        if (slot.hits.none { it.step == step && it.padIndex == padIndex }) {
            updatedClips[slotIdx] = slot.copy(hits = slot.hits + hit)
            _uiState.update {
                it.copy(
                    clips = updatedClips,
                    activeClips = it.activeClips + slotIdx
                )
            }
        }
    }

    private fun handleTutorialPadHit(tut: TutorialSessionState, padIndex: Int) {
        val expected = tut.targetPadIndex ?: return
        val isRight = (padIndex == expected)
        val nextCorrect = tut.hitsCorrect + if (isRight) 1 else 0
        val nextAttempts = tut.totalAttempts + 1
        val nextStep = if (isRight) tut.currentStepIndex + 1 else tut.currentStepIndex
        val completed = nextStep >= tut.steps.size
        _uiState.update {
            it.copy(
                activeTutorial = tut.copy(
                    currentStepIndex = nextStep.coerceAtMost(tut.steps.lastIndex),
                    hitsCorrect = nextCorrect,
                    totalAttempts = nextAttempts,
                    isCompleted = completed
                )
            )
        }
        if (completed) {
            showStatus("🎉 Lesson Complete! Accuracy: ${(nextCorrect * 100) / nextAttempts}%")
        }
    }

    fun triggerQuickFx(fxName: String) {
        if (_uiState.value.isHapticEnabled) hapticManager.playPadHaptic()
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
        if (_uiState.value.isRecording) wavRecorder.recordPadEvent(fxIndex)
        _uiState.update { it.copy(activeQuickFx = fxName) }
        viewModelScope.launch {
            delay(250)
            _uiState.update { if (it.activeQuickFx == fxName) it.copy(activeQuickFx = null) else it }
        }
    }

    // ------------------------------------------------------------------
    // CLIPS & STEP SEQUENCER (MULTI-LAYER REAL PLAYBACK & EDITING)
    // ------------------------------------------------------------------
    fun setClipsMode(isClips: Boolean) {
        if (_uiState.value.isPadLocked) {
            showStatus("Unlock pads to switch mode")
            return
        }
        _uiState.update { it.copy(isClipsMode = isClips) }
        if (isClips && _uiState.value.activeClips.isNotEmpty() && clipLoopJob?.isActive != true) {
            startClipSequencer()
        }
    }

    fun toggleClip(clipIndex: Int) {
        val slot = _uiState.value.clips.getOrNull(clipIndex) ?: return
        // If empty slot is tapped, select it & open Step Editor so user can build or record into it!
        if (slot.isEmpty && !_uiState.value.isRecording) {
            _uiState.update { it.copy(selectedClipIndex = clipIndex, activeScreen = ActiveScreen.CLIP_EDITOR) }
            return
        }
        _uiState.update { current ->
            val next = if (current.activeClips.contains(clipIndex)) {
                current.activeClips - clipIndex
            } else {
                current.activeClips + clipIndex
            }
            current.copy(activeClips = next, selectedClipIndex = clipIndex)
        }
        if (_uiState.value.activeClips.isNotEmpty() || _uiState.value.isMetronomeEnabled) {
            if (clipLoopJob?.isActive != true) startClipSequencer()
        } else {
            stopClipSequencerOnly()
        }
    }

    fun selectClip(clipIndex: Int) {
        _uiState.update { it.copy(selectedClipIndex = clipIndex.coerceIn(0, 11)) }
    }

    fun openClipEditor(clipIndex: Int = _uiState.value.selectedClipIndex) {
        _uiState.update {
            it.copy(
                selectedClipIndex = clipIndex.coerceIn(0, 11),
                activeScreen = ActiveScreen.CLIP_EDITOR
            )
        }
    }

    fun toggleClipStep(clipIndex: Int, padIndex: Int, step: Int) {
        val state = _uiState.value
        val updatedClips = state.clips.toMutableList()
        val slot = updatedClips.getOrNull(clipIndex) ?: return
        val exists = slot.hits.any { it.step == step && it.padIndex == padIndex }
        val newHits = if (exists) {
            slot.hits.filterNot { it.step == step && it.padIndex == padIndex }
        } else {
            audioEngine.triggerPad(padIndex)
            slot.hits + ClipHit(step = step, padIndex = padIndex)
        }
        updatedClips[clipIndex] = slot.copy(hits = newHits)
        _uiState.update { it.copy(clips = updatedClips) }
    }

    fun clearClipPad(clipIndex: Int, padIndex: Int) {
        val updated = _uiState.value.clips.toMutableList()
        val slot = updated.getOrNull(clipIndex) ?: return
        updated[clipIndex] = slot.copy(hits = slot.hits.filterNot { it.padIndex == padIndex })
        _uiState.update { it.copy(clips = updated) }
    }

    fun clearSelectedClip() {
        val idx = _uiState.value.selectedClipIndex
        val updated = _uiState.value.clips.toMutableList()
        val slot = updated.getOrNull(idx) ?: return
        updated[idx] = slot.copy(hits = emptyList())
        _uiState.update { it.copy(clips = updated, activeClips = it.activeClips - idx) }
        showStatus("Cleared ${slot.name}")
    }

    fun deleteSelectedClip() = clearSelectedClip()

    fun duplicateSelectedClip() {
        val state = _uiState.value
        val src = state.clips.getOrNull(state.selectedClipIndex) ?: return
        val targetIdx = state.clips.indexOfFirst { it.isEmpty }.takeIf { it >= 0 }
            ?: ((state.selectedClipIndex + 1) % 12)
        val updated = state.clips.toMutableList()
        updated[targetIdx] = src.copy(index = targetIdx, name = "${src.name} Copy")
        _uiState.update {
            it.copy(
                clips = updated,
                selectedClipIndex = targetIdx,
                activeClips = it.activeClips + targetIdx
            )
        }
        if (clipLoopJob?.isActive != true) startClipSequencer()
        showStatus("Duplicated to Clip ${targetIdx + 1}")
    }

    fun toggleClipPlayback() {
        if (_uiState.value.isClipPlaying) {
            stopPlayback()
        } else {
            val sel = _uiState.value.selectedClipIndex
            if (_uiState.value.activeClips.isEmpty()) {
                _uiState.update { it.copy(activeClips = setOf(sel)) }
            }
            startClipSequencer()
        }
    }

    fun toggleMetronome() {
        val next = !_uiState.value.isMetronomeEnabled
        _uiState.update { it.copy(isMetronomeEnabled = next) }
        if (next && clipLoopJob?.isActive != true) {
            startClipSequencer()
        } else if (!next && _uiState.value.activeClips.isEmpty() && !_uiState.value.isRecording) {
            stopClipSequencerOnly()
        }
    }

    fun adjustBpm(delta: Int) {
        val newBpm = (_uiState.value.bpm + delta).coerceIn(60, 200)
        audioEngine.setBpm(newBpm)
        _uiState.update { it.copy(bpm = newBpm) }
    }

    fun stopPlayback() {
        stopClipSequencerOnly()
        audioEngine.stopAllVoices()
        _uiState.update { it.copy(activeClips = emptySet(), isClipPlaying = false) }
    }

    private fun stopClipSequencerOnly() {
        clipLoopJob?.cancel()
        clipLoopJob = null
        currentStepAtomic = 0
        _uiState.update { it.copy(isClipPlaying = false, currentSequencerStep = 0) }
    }

    private fun startClipSequencer() {
        clipLoopJob?.cancel()
        _uiState.update { it.copy(isClipPlaying = true) }
        clipLoopJob = viewModelScope.launch(Dispatchers.Default) {
            var step = 0
            while (isActive) {
                val state = _uiState.value
                val shouldRun = state.activeClips.isNotEmpty() || state.isMetronomeEnabled ||
                    (state.isRecording && state.recordingMode == RecordingMode.LOOP)
                if (!shouldRun) break

                currentStepAtomic = step
                _uiState.update { it.copy(currentSequencerStep = step % 16) }

                // Metronome click on quarter notes (0, 4, 8, 12)
                if (state.isMetronomeEnabled && step % 4 == 0) {
                    audioEngine.playMetronomeClick(accent = (step % 16 == 0))
                }

                // Trigger all pads scheduled at this step across all active clips (multi-layer!)
                val triggeredPads = mutableSetOf<Int>()
                for (clipIdx in state.activeClips) {
                    val clip = state.clips.getOrNull(clipIdx) ?: continue
                    val localStep = step % clip.lengthSteps.coerceAtLeast(1)
                    for (hit in clip.hits) {
                        if (hit.step == localStep && triggeredPads.add(hit.padIndex)) {
                            audioEngine.triggerPad(hit.padIndex)
                            if (state.isRecording) {
                                wavRecorder.recordPadEvent(hit.padIndex)
                            }
                        }
                    }
                }

                // Visual flash of sequencer-triggered pads if Pad Light Show is on
                if (triggeredPads.isNotEmpty() && state.isLightShowEnabled) {
                    _uiState.update { it.copy(activePads = it.activePads + triggeredPads) }
                    launch {
                        delay(80)
                        _uiState.update { it.copy(activePads = it.activePads - triggeredPads) }
                    }
                }

                val stepMs = (60_000L / state.bpm.coerceIn(60, 200)) / 4L
                delay(stepMs)
                step = (step + 1) % 32
            }
            _uiState.update { it.copy(isClipPlaying = false, currentSequencerStep = 0) }
        }
    }

    // ------------------------------------------------------------------
    // REAL AUDIO EFFECTS (ALL 10 LIVE DSP FX + FILL & CLEAN)
    // ------------------------------------------------------------------
    fun toggleSoundFxTray() {
        _uiState.update { it.copy(isSoundFxOpen = !it.isSoundFxOpen) }
    }

    fun setFaderLevel(fxType: String, level: Float) {
        val current = _uiState.value.dspParams
        val updated = when (fxType) {
            "Volume" -> current.copy(masterVolume = level.coerceIn(0f, 1f))
            "Lo-Fi" -> current.copy(lofiLevel = level.coerceIn(0f, 1f))
            "Filter" -> current.copy(filterCutoff = level.coerceIn(0f, 1f))
            "Pitch" -> {
                // Map 0..1 fader to -12..+12 semitones (0.5 = 0 st)
                val semi = ((level.coerceIn(0f, 1f) - 0.5f) * 24f)
                current.copy(masterPitchSemitones = semi)
            }
            "Reverb" -> current.copy(reverbLevel = level.coerceIn(0f, 1f))
            "Delay 1/16", "Echo 1/16" -> current.copy(delay116Level = level.coerceIn(0f, 1f))
            "Delay 1/8", "Echo 1/8" -> current.copy(delay18Level = level.coerceIn(0f, 1f))
            "Distortion" -> current.copy(distortionLevel = level.coerceIn(0f, 1f))
            "Gate" -> current.copy(gateLevel = level.coerceIn(0f, 1f))
            "Wobble" -> current.copy(wobbleLevel = level.coerceIn(0f, 1f))
            else -> current
        }
        audioEngine.setDspParams(updated)
        _uiState.update { it.copy(dspParams = updated) }
    }

    /**
     * Rhythm Effect: FILL — generates an authentic live drum fill synced to BPM!
     */
    fun triggerFill() {
        if (_uiState.value.isHapticEnabled) hapticManager.playPadHaptic()
        showStatus("⚡ FILL triggered!")
        viewModelScope.launch(Dispatchers.Default) {
            val sixteenth = (60_000L / _uiState.value.bpm.coerceIn(60, 200)) / 4L
            val pattern = listOf(
                Pair(2, sixteenth),      // Snare
                Pair(19, sixteenth / 2), // High Tom
                Pair(19, sixteenth / 2), // High Tom roll
                Pair(2, sixteenth / 2),  // Snare
                Pair(18, sixteenth / 2), // Low Tom
                Pair(2, sixteenth / 2),  // Snare flam
                Pair(18, sixteenth / 2), // Low Tom
                Pair(20, 0L)             // Crash finale!
            )
            for ((padIdx, waitMs) in pattern) {
                audioEngine.triggerPad(padIdx)
                if (_uiState.value.isRecording) wavRecorder.recordPadEvent(padIdx)
                _uiState.update { it.copy(activePads = it.activePads + padIdx) }
                launch {
                    delay(90)
                    _uiState.update { it.copy(activePads = it.activePads - padIdx) }
                }
                if (waitMs > 0) delay(waitMs)
            }
        }
    }

    /**
     * Rhythm Effect: CLEAN — strips dense auxiliary clip layers back to clean core rhythm & clears heavy FX.
     */
    fun triggerClean() {
        if (_uiState.value.isHapticEnabled) hapticManager.playPadHaptic()
        val cleanFx = DspEffectParams(
            masterVolume = _uiState.value.dspParams.masterVolume,
            lofiLevel = 0f,
            filterCutoff = 1f,
            masterPitchSemitones = 0f,
            reverbLevel = 0.08f,
            delay116Level = 0f,
            delay18Level = 0f,
            distortionLevel = 0f,
            gateLevel = 0f,
            wobbleLevel = 0f
        )
        audioEngine.setDspParams(cleanFx)
        // Simplify busy clip layers down to kick + snare core (clips 0 & 1) if clips are running
        val simplifiedClips = _uiState.value.activeClips.filter { it <= 1 }.toSet()
        _uiState.update {
            it.copy(
                dspParams = cleanFx,
                activeClips = if (it.activeClips.isNotEmpty()) simplifiedClips.ifEmpty { setOf(0) } else emptySet()
            )
        }
        showStatus("✨ CLEAN mix applied")
    }

    fun resetAllFx() {
        val defaults = DspEffectParams()
        audioEngine.setDspParams(defaults)
        _uiState.update { it.copy(dspParams = defaults) }
        showStatus("All FX reset to studio default")
    }

    // ------------------------------------------------------------------
    // REAL POST-FX WAV RECORDING (FREE MODE & LOOP MODE)
    // ------------------------------------------------------------------
    fun setRecordingMode(mode: RecordingMode) {
        _uiState.update { it.copy(recordingMode = mode) }
        showStatus("Recording Mode: ${mode.label}")
    }

    fun toggleRecording() {
        if (_uiState.value.isRecording) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    fun startRecording() {
        if (_uiState.value.isRecording) return
        stopPreviewAndMiniPlayer()
        val started = wavRecorder.start(audioEngine)
        if (!started) {
            showStatus("Unable to start audio recording")
            return
        }

        val mode = _uiState.value.recordingMode
        _uiState.update { it.copy(isRecording = true, recordingDurationSeconds = 0) }

        // In Loop Mode, ensure the loop sequencer clock runs so live hits overdub into the clip slot!
        if (mode == RecordingMode.LOOP && clipLoopJob?.isActive != true) {
            startClipSequencer()
        }

        recordingTimerJob?.cancel()
        recordingTimerJob = viewModelScope.launch {
            var sec = 0
            // Loop mode automatically captures a tight 4-bar loop (or user can stop early!)
            val loopMaxSec = if (mode == RecordingMode.LOOP) {
                ((60.0 / _uiState.value.bpm) * 16.0).toInt().coerceIn(4, 30)
            } else {
                300 // 5 min max in Free Mode
            }
            while (isActive && _uiState.value.isRecording) {
                delay(1000)
                sec++
                _uiState.update { it.copy(recordingDurationSeconds = sec) }
                if (mode == RecordingMode.LOOP && sec >= loopMaxSec) {
                    stopRecording()
                    break
                }
            }
        }
    }

    fun stopRecording() {
        if (!_uiState.value.isRecording) return
        recordingTimerJob?.cancel()
        recordingTimerJob = null

        val state = _uiState.value
        val title = "${state.selectedPackName} Beat #${state.recordings.size + 1}"
        val item = wavRecorder.stop(
            audioEngine = audioEngine,
            title = title,
            packName = state.selectedPackName,
            packId = state.activePackId,
            bpm = state.bpm,
            mode = state.recordingMode
        )

        if (item != null) {
            val updated = listOf(item) + state.recordings
            storage.saveRecordings(updated)
            _uiState.update {
                it.copy(
                    isRecording = false,
                    recordingDurationSeconds = 0,
                    recordings = updated,
                    miniPlayerTrack = item.title,
                    miniPlayerSubtitle = "${item.packName} • ${item.bpm} BPM",
                    miniPlayerDuration = item.durationFormatted
                )
            }
            showStatus("✅ Saved real WAV: ${item.title} (${item.durationFormatted})")
        } else {
            _uiState.update { it.copy(isRecording = false, recordingDurationSeconds = 0) }
        }
    }

    // ------------------------------------------------------------------
    // RECORDINGS PLAYBACK, RENAME, DELETE, SHARE WAV, SAVE MUSIC, EXPORT MP4
    // ------------------------------------------------------------------
    fun togglePlayRecording(recording: RecordingItem) {
        if (wavPlayer.isPlayingId(recording.id)) {
            stopRecordingPlayback()
            return
        }
        stopPreviewAndMiniPlayer()
        val ok = wavPlayer.play(recording.id, recording.filePath) {
            _uiState.update { it.copy(playingRecordingId = null, recordingPlaybackProgress = 0f, isMiniPlayerPlaying = false) }
        }
        if (ok) {
            _uiState.update {
                it.copy(
                    playingRecordingId = recording.id,
                    recordingPlaybackProgress = 0f,
                    miniPlayerTrack = recording.title,
                    miniPlayerSubtitle = "${recording.packName} • ${recording.bpm} BPM",
                    miniPlayerDuration = recording.durationFormatted,
                    isMiniPlayerPlaying = true
                )
            }
            playbackProgressJob?.cancel()
            playbackProgressJob = viewModelScope.launch {
                while (isActive && wavPlayer.isPlayingId(recording.id)) {
                    _uiState.update { it.copy(recordingPlaybackProgress = wavPlayer.getCurrentProgress()) }
                    delay(80)
                }
            }
        } else {
            showStatus("Recording file not found on device")
        }
    }

    fun stopRecordingPlayback() {
        playbackProgressJob?.cancel()
        wavPlayer.stop()
        _uiState.update { it.copy(playingRecordingId = null, recordingPlaybackProgress = 0f, isMiniPlayerPlaying = false) }
    }

    fun renameRecording(id: String, newTitle: String) {
        val clean = newTitle.trim().ifBlank { return }
        val updated = _uiState.value.recordings.map { if (it.id == id) it.copy(title = clean) else it }
        storage.saveRecordings(updated)
        _uiState.update { it.copy(recordings = updated) }
    }

    fun deleteRecording(id: String) {
        if (_uiState.value.playingRecordingId == id) stopRecordingPlayback()
        val target = _uiState.value.recordings.firstOrNull { it.id == id }
        target?.let { runCatching { File(it.filePath).delete() } }
        val updated = _uiState.value.recordings.filterNot { it.id == id }
        storage.saveRecordings(updated)
        _uiState.update { it.copy(recordings = updated) }
        showStatus("Recording deleted")
    }

    fun shareRecordingWav(recording: RecordingItem) {
        val ok = ExportService.shareAudioFile(context, recording.filePath, recording.title)
        if (!ok) showStatus("Could not share audio file")
    }

    fun saveRecordingToMusic(recording: RecordingItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val savedPath = ExportService.saveToPublicMusic(context, recording)
            withContext(Dispatchers.Main) {
                if (savedPath != null) {
                    showStatus("✅ Saved to $savedPath")
                } else {
                    showStatus("Failed to export to Music folder")
                }
            }
        }
    }

    fun exportRecordingVideo(recording: RecordingItem) {
        if (_uiState.value.isExportingVideo) return
        _uiState.update { it.copy(isExportingVideo = true) }
        showStatus("🎬 Rendering 9:16 MP4 performance video...")
        viewModelScope.launch(Dispatchers.IO) {
            val mp4 = ExportService.exportPerformanceVideoMp4(context, recording)
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(isExportingVideo = false) }
                if (mp4 != null) {
                    showStatus("✅ Video exported (${mp4.length() / 1024} KB)! Opening Share...")
                    ExportService.shareVideoFile(context, mp4, recording.title)
                } else {
                    showStatus("Video export failed on this codec; sharing WAV instead")
                    ExportService.shareAudioFile(context, recording.filePath, recording.title)
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // PAD EDITOR, SOUND WAVEFORM EDITOR, AUDIO IMPORT & MIC SAMPLER
    // ------------------------------------------------------------------
    fun openPadEditor(padIndex: Int = 0) {
        val idx = padIndex.coerceIn(0, 23)
        refreshEditorWaveform(idx)
        _uiState.update { it.copy(editingPadIndex = idx, activeScreen = ActiveScreen.PAD_EDITOR) }
    }

    fun openSoundEditor(padIndex: Int = _uiState.value.editingPadIndex) {
        val idx = padIndex.coerceIn(0, 23)
        refreshEditorWaveform(idx)
        _uiState.update { it.copy(editingPadIndex = idx, activeScreen = ActiveScreen.SOUND_EDITOR) }
    }

    fun selectEditorPad(padIndex: Int) {
        val idx = padIndex.coerceIn(0, 23)
        refreshEditorWaveform(idx)
        _uiState.update { it.copy(editingPadIndex = idx) }
    }

    private fun refreshEditorWaveform(padIndex: Int) {
        val buf = audioEngine.getSampleBuffer(padIndex)
        val peaks = AudioFileDecoder.computeWaveformPeaks(buf, 60).toList()
        val dur = (buf.size.toFloat() / JvmAudioEngine.SAMPLE_RATE.toFloat()).coerceAtLeast(0.05f)
        _uiState.update { it.copy(editorWaveformPeaks = peaks, editorSampleDurationSec = dur) }
    }

    fun updatePad(updatedPad: Pad) {
        val idx = updatedPad.index.coerceIn(0, 23)
        val newList = _uiState.value.pads.toMutableList()
        if (idx >= newList.size) return
        newList[idx] = updatedPad
        audioEngine.updatePadConfig(updatedPad)
        _uiState.update { it.copy(pads = newList) }
        persistIfCurrentPackIsCustom(newList)
    }

    fun swapPads(indexA: Int, indexB: Int) {
        if (indexA !in 0 until 24 || indexB !in 0 until 24 || indexA == indexB) return
        val list = _uiState.value.pads.toMutableList()
        val padA = list[indexA].copy(index = indexB)
        val padB = list[indexB].copy(index = indexA)
        list[indexA] = padB
        list[indexB] = padA

        val bufA = audioEngine.getSampleBuffer(indexA).copyOf()
        val bufB = audioEngine.getSampleBuffer(indexB).copyOf()
        val origA = originalSampleBuffers[indexA]
        val origB = originalSampleBuffers[indexB]
        originalSampleBuffers[indexA] = origB
        originalSampleBuffers[indexB] = origA

        if (bufB.isNotEmpty()) audioEngine.loadFloatSample(indexA, bufB)
        if (bufA.isNotEmpty()) audioEngine.loadFloatSample(indexB, bufA)
        audioEngine.updatePadConfig(padB)
        audioEngine.updatePadConfig(padA)

        _uiState.update { it.copy(pads = list, editingPadIndex = indexB) }
        refreshEditorWaveform(indexB)
        persistIfCurrentPackIsCustom(list)
        showStatus("Swapped Pad ${indexA + 1} ↔ Pad ${indexB + 1}")
    }

    fun duplicatePad(sourceIndex: Int, targetIndex: Int) {
        if (sourceIndex !in 0 until 24 || targetIndex !in 0 until 24 || sourceIndex == targetIndex) return
        val list = _uiState.value.pads.toMutableList()
        val copyPad = list[sourceIndex].copy(index = targetIndex, name = "${list[sourceIndex].name} Copy")
        list[targetIndex] = copyPad

        val buf = audioEngine.getSampleBuffer(sourceIndex).copyOf()
        if (buf.isNotEmpty()) {
            audioEngine.loadFloatSample(targetIndex, buf)
            originalSampleBuffers[targetIndex] = buf.copyOf()
        }
        audioEngine.updatePadConfig(copyPad)

        _uiState.update { it.copy(pads = list, editingPadIndex = targetIndex) }
        refreshEditorWaveform(targetIndex)
        persistIfCurrentPackIsCustom(list)
        showStatus("Duplicated Pad ${sourceIndex + 1} → Pad ${targetIndex + 1}")
    }

    fun importAudioFileToPad(padIndex: Int, uri: Uri, displayName: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val decoded = AudioFileDecoder.decodeUriToFloat(context, uri)
            if (decoded == null || decoded.isEmpty()) {
                withContext(Dispatchers.Main) { showStatus("Could not decode audio file") }
                return@launch
            }
            val savedFile = File(storage.getCustomSamplesDir(), "pad_${padIndex}_${System.currentTimeMillis()}.wav")
            AudioFileDecoder.writeMonoWav(savedFile, decoded)

            originalSampleBuffers[padIndex] = decoded.copyOf()
            audioEngine.loadFloatSample(padIndex, decoded)

            val cleanName = (displayName ?: "Imported").substringBeforeLast('.').take(16)
            val currentPad = _uiState.value.pads.getOrNull(padIndex) ?: return@launch
            val updatedPad = currentPad.copy(
                name = cleanName,
                trimStart = 0f,
                trimEnd = 1f,
                fadeInRatio = 0f,
                fadeOutRatio = 0f,
                customFilePath = savedFile.absolutePath
            )
            audioEngine.updatePadConfig(updatedPad)

            withContext(Dispatchers.Main) {
                val list = _uiState.value.pads.toMutableList()
                list[padIndex] = updatedPad
                _uiState.update { it.copy(pads = list) }
                refreshEditorWaveform(padIndex)
                persistIfCurrentPackIsCustom(list)
                audioEngine.triggerPad(padIndex)
                showStatus("✅ Imported \"$cleanName\" onto Pad ${padIndex + 1}")
            }
        }
    }

    fun assignBuiltInSampleToPad(padIndex: Int, sourceGenre: MusicGenre, sourceSlotIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val kit = GenreSampleSynthesizer.synthesizeKitForGenre(sourceGenre)
            val sample = kit.getOrNull(sourceSlotIndex) ?: return@launch
            val templatePads = SoundPackCatalog.buildPadsForGenre(sourceGenre)
            val sampleName = templatePads.getOrNull(sourceSlotIndex)?.name ?: "Sample"

            val savedFile = File(storage.getCustomSamplesDir(), "lib_${padIndex}_${System.currentTimeMillis()}.wav")
            AudioFileDecoder.writeMonoWav(savedFile, sample)

            originalSampleBuffers[padIndex] = sample.copyOf()
            audioEngine.loadFloatSample(padIndex, sample)

            val currentPad = _uiState.value.pads.getOrNull(padIndex) ?: return@launch
            val updatedPad = currentPad.copy(
                name = sampleName,
                trimStart = 0f,
                trimEnd = 1f,
                customFilePath = savedFile.absolutePath
            )
            audioEngine.updatePadConfig(updatedPad)
            withContext(Dispatchers.Main) {
                val list = _uiState.value.pads.toMutableList()
                list[padIndex] = updatedPad
                _uiState.update { it.copy(pads = list) }
                refreshEditorWaveform(padIndex)
                persistIfCurrentPackIsCustom(list)
                audioEngine.triggerPad(padIndex)
                showStatus("Loaded $sampleName (${sourceGenre.label}) → Pad ${padIndex + 1}")
            }
        }
    }

    fun applyCropAndFadeToPad(
        padIndex: Int,
        newName: String,
        trimStart: Float,
        trimEnd: Float,
        fadeInRatio: Float,
        fadeOutRatio: Float
    ) {
        val currentBuf = audioEngine.getSampleBuffer(padIndex)
        if (currentBuf.isEmpty()) return
        val cropped = AudioFileDecoder.cropAndFade(currentBuf, trimStart, trimEnd, fadeInRatio, fadeOutRatio)
        val savedFile = File(storage.getCustomSamplesDir(), "crop_${padIndex}_${System.currentTimeMillis()}.wav")
        AudioFileDecoder.writeMonoWav(savedFile, cropped)

        audioEngine.loadFloatSample(padIndex, cropped)
        val pad = _uiState.value.pads.getOrNull(padIndex) ?: return
        val updatedPad = pad.copy(
            name = newName.trim().ifBlank { pad.name },
            trimStart = 0f,
            trimEnd = 1f,
            fadeInRatio = 0f,
            fadeOutRatio = 0f,
            customFilePath = savedFile.absolutePath
        )
        audioEngine.updatePadConfig(updatedPad)

        val list = _uiState.value.pads.toMutableList()
        list[padIndex] = updatedPad
        _uiState.update { it.copy(pads = list) }
        refreshEditorWaveform(padIndex)
        persistIfCurrentPackIsCustom(list)
        audioEngine.triggerPad(padIndex)
        showStatus("✂️ Cropped & applied fades to ${updatedPad.name}")
    }

    fun resetPadSample(padIndex: Int) {
        val orig = originalSampleBuffers[padIndex] ?: return
        audioEngine.loadFloatSample(padIndex, orig.copyOf())
        val pad = _uiState.value.pads.getOrNull(padIndex) ?: return
        val resetPad = pad.copy(trimStart = 0f, trimEnd = 1f, fadeInRatio = 0f, fadeOutRatio = 0f, pitchSemitones = 0f, volume = 1f, pan = 0f)
        audioEngine.updatePadConfig(resetPad)
        val list = _uiState.value.pads.toMutableList()
        list[padIndex] = resetPad
        _uiState.update { it.copy(pads = list) }
        refreshEditorWaveform(padIndex)
        showStatus("Reset Pad ${padIndex + 1} sample")
    }

    fun toggleMicSampleRecording(padIndex: Int) {
        if (micRecorder.isRecording()) {
            val captured = micRecorder.stop()
            _uiState.update { it.copy(isMicRecordingSample = false, micPeakLevel = 0f) }
            if (captured.size > 441) {
                val savedFile = File(storage.getCustomSamplesDir(), "mic_${padIndex}_${System.currentTimeMillis()}.wav")
                AudioFileDecoder.writeMonoWav(savedFile, captured)
                originalSampleBuffers[padIndex] = captured.copyOf()
                audioEngine.loadFloatSample(padIndex, captured)
                val pad = _uiState.value.pads.getOrNull(padIndex) ?: return
                val updated = pad.copy(
                    name = "Mic Rec ${padIndex + 1}",
                    trimStart = 0f,
                    trimEnd = 1f,
                    customFilePath = savedFile.absolutePath
                )
                audioEngine.updatePadConfig(updated)
                val list = _uiState.value.pads.toMutableList()
                list[padIndex] = updated
                _uiState.update { it.copy(pads = list) }
                refreshEditorWaveform(padIndex)
                persistIfCurrentPackIsCustom(list)
                audioEngine.triggerPad(padIndex)
                showStatus("🎤 Captured ${(captured.size / 44100f).let { String.format("%.2fs", it) }} sample!")
            } else {
                showStatus("Mic recording too short")
            }
        } else {
            val ok = micRecorder.start { peak ->
                _uiState.update { it.copy(micPeakLevel = peak) }
            }
            if (ok) {
                _uiState.update { it.copy(isMicRecordingSample = true) }
            } else {
                showStatus("Microphone permission needed to record samples")
            }
        }
    }

    // ------------------------------------------------------------------
    // PROJECT EDITOR (CUSTOM SOUND PACKS)
    // ------------------------------------------------------------------
    fun createCustomSoundPack(
        title: String,
        genre: MusicGenre,
        bpm: Int,
        gradientStart: Long,
        gradientEnd: Long,
        openEditorAfter: Boolean
    ) {
        val cleanTitle = title.trim().ifBlank { "My Custom Pack" }
        val newPack = SoundPack(
            id = "custom_${UUID.randomUUID().toString().take(8)}",
            title = cleanTitle,
            subtitle = "Custom ${genre.label} Project",
            artist = _uiState.value.userName,
            genre = genre,
            bpm = bpm.coerceIn(60, 200),
            gradientStart = gradientStart,
            gradientEnd = gradientEnd,
            isCustom = true,
            isDownloaded = true,
            downloadsLabel = "Custom",
            pads = SoundPackCatalog.buildPadsForGenre(genre)
        )
        val updatedCustoms = listOf(newPack) + _uiState.value.customSoundPacks
        storage.saveCustomPacks(updatedCustoms)
        _uiState.update {
            it.copy(
                customSoundPacks = updatedCustoms,
                allSoundPacks = updatedCustoms + SoundPackCatalog.allPacks
            )
        }
        loadPackIntoEngine(newPack, openPlayer = !openEditorAfter)
        if (openEditorAfter) {
            openPadEditor(0)
        }
        showStatus("✨ Created Custom Sound Pack: $cleanTitle")
    }

    fun deleteCustomPack(packId: String) {
        val updatedCustoms = _uiState.value.customSoundPacks.filterNot { it.id == packId }
        storage.saveCustomPacks(updatedCustoms)
        _uiState.update {
            it.copy(
                customSoundPacks = updatedCustoms,
                allSoundPacks = updatedCustoms + SoundPackCatalog.allPacks
            )
        }
        showStatus("Deleted custom pack")
    }

    private fun persistIfCurrentPackIsCustom(updatedPads: List<Pad>) {
        val id = _uiState.value.activePackId
        val customs = _uiState.value.customSoundPacks
        if (customs.any { it.id == id }) {
            val newCustoms = customs.map { if (it.id == id) it.copy(pads = updatedPads) else it }
            storage.saveCustomPacks(newCustoms)
            _uiState.update {
                it.copy(
                    customSoundPacks = newCustoms,
                    allSoundPacks = newCustoms + SoundPackCatalog.allPacks
                )
            }
        }
    }

    // ------------------------------------------------------------------
    // SOUND LIBRARY: SEARCH, FILTER, PREVIEW, FAVORITES
    // ------------------------------------------------------------------
    fun selectLibraryGenre(genre: MusicGenre) {
        _uiState.update { it.copy(selectedLibraryGenre = genre) }
    }

    fun setLibrarySearchQuery(query: String) {
        _uiState.update { it.copy(librarySearchQuery = query) }
    }

    fun toggleFavoritePack(packId: String) {
        val next = if (_uiState.value.favoritePackIds.contains(packId)) {
            _uiState.value.favoritePackIds - packId
        } else {
            _uiState.value.favoritePackIds + packId
        }
        storage.saveStringSet("favorite_packs", next)
        _uiState.update { it.copy(favoritePackIds = next) }
    }

    fun togglePreviewPack(pack: SoundPack) {
        if (_uiState.value.previewingPackId == pack.id) {
            stopPreviewAndMiniPlayer()
            return
        }
        stopPreviewAndMiniPlayer()
        audioEngine.setBpm(pack.bpm)
        _uiState.update {
            it.copy(
                previewingPackId = pack.id,
                miniPlayerTrack = "${pack.title} • ${pack.genre.label}",
                miniPlayerSubtitle = "${pack.artist} • ${pack.bpm} BPM",
                miniPlayerDuration = "${pack.bpm} BPM",
                isMiniPlayerPlaying = true
            )
        }
        previewJob = viewModelScope.launch(Dispatchers.IO) {
            val kit = GenreSampleSynthesizer.synthesizeKitForGenre(pack.genre, pack.id)
            for (i in 0 until 24) audioEngine.loadFloatSample(i, kit[i])
            val clips = ClipPatternPresets.buildDefaultClips(pack.genre, pack.id)
            val stepMs = (60_000L / pack.bpm.coerceIn(60, 200)) / 4L
            // Play 64 steps (4 bars): evolving intro -> bass+drums -> full melody -> climax fill
            for (step in 0 until 64) {
                if (!isActive) break
                val bar = step / 16
                val s = step % 16
                val activeIndices = when (bar) {
                    0 -> listOf(0, 2, 3)       // Kick + Hats + Bassline
                    1 -> listOf(0, 1, 2, 3, 4) // Full Drums + Bass + Melody Hook
                    2 -> listOf(0, 1, 2, 3, 4, 5) // Add Percs
                    else -> listOf(0, 1, 2, 3, 4, 6) // Climax + Vocals
                }
                for (cIdx in activeIndices) {
                    val clip = clips[cIdx]
                    for (hit in clip.hits) {
                        if (hit.step == s) audioEngine.triggerPad(hit.padIndex)
                    }
                }
                delay(stepMs)
            }
            withContext(Dispatchers.Main) {
                if (_uiState.value.previewingPackId == pack.id) {
                    _uiState.update { it.copy(previewingPackId = null, isMiniPlayerPlaying = false) }
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // COMMUNITY FEED, CREW LESSONS & MINI PLAYER
    // ------------------------------------------------------------------
    fun togglePlayPost(post: CommunityPost) {
        if (_uiState.value.playingPostId == post.id) {
            stopPreviewAndMiniPlayer()
            return
        }
        stopPreviewAndMiniPlayer()

        // If this post has a real recorded WAV file, play it via MediaPlayer!
        if (!post.recordingFilePath.isNullOrBlank() && File(post.recordingFilePath).exists()) {
            wavPlayer.play(post.id, post.recordingFilePath) {
                _uiState.update { it.copy(playingPostId = null, isMiniPlayerPlaying = false) }
            }
            _uiState.update {
                it.copy(
                    playingPostId = post.id,
                    miniPlayerTrack = post.trackTitle,
                    miniPlayerSubtitle = "${post.authorName} • ${post.bpm} BPM",
                    isMiniPlayerPlaying = true
                )
            }
            return
        }

        // Otherwise play a real live multi-layer beat performance of that post's pack & BPM!
        val pack = _uiState.value.allSoundPacks.firstOrNull { it.id == post.packId } ?: SoundPackCatalog.getPackById(post.packId)
        _uiState.update {
            it.copy(
                playingPostId = post.id,
                miniPlayerTrack = post.trackTitle,
                miniPlayerSubtitle = "${post.authorName} • ${post.bpm} BPM",
                miniPlayerDuration = "00:16",
                isMiniPlayerPlaying = true
            )
        }
        previewJob = viewModelScope.launch(Dispatchers.IO) {
            val kit = GenreSampleSynthesizer.synthesizeKitForGenre(pack.genre)
            for (i in 0 until 24) audioEngine.loadFloatSample(i, kit[i])
            val clips = ClipPatternPresets.buildDefaultClips(pack.genre)
            val stepMs = (60_000L / post.bpm.coerceIn(60, 200)) / 4L
            for (step in 0 until 48) {
                if (!isActive) break
                for (cIdx in 0..5) {
                    val clip = clips[cIdx]
                    for (hit in clip.hits) {
                        if (hit.step == step % 16) audioEngine.triggerPad(hit.padIndex)
                    }
                }
                delay(stepMs)
            }
            withContext(Dispatchers.Main) {
                if (_uiState.value.playingPostId == post.id) {
                    _uiState.update { it.copy(playingPostId = null, isMiniPlayerPlaying = false) }
                }
            }
        }
    }

    fun toggleMiniPlayer() {
        if (_uiState.value.isMiniPlayerPlaying) {
            stopPreviewAndMiniPlayer()
        } else {
            val firstRec = _uiState.value.recordings.firstOrNull()
            if (firstRec != null) {
                togglePlayRecording(firstRec)
            } else {
                val curId = _uiState.value.previewingPackId
                val packs = _uiState.value.allSoundPacks
                val targetPack = packs.firstOrNull { it.id == curId }
                    ?: packs.firstOrNull { _uiState.value.miniPlayerTrack?.startsWith(it.title) == true }
                    ?: packs.first()
                togglePreviewPack(targetPack)
            }
        }
    }

    fun nextMiniPlayerTrack() {
        val packs = _uiState.value.allSoundPacks
        if (packs.isEmpty()) return
        val curId = _uiState.value.previewingPackId
        val curIdx = packs.indexOfFirst { it.id == curId || _uiState.value.miniPlayerTrack?.startsWith(it.title) == true }
        val nextPack = packs[(curIdx + 1) % packs.size]
        togglePreviewPack(nextPack)
    }

    fun stopPreviewAndMiniPlayer() {
        previewJob?.cancel()
        previewJob = null
        playbackProgressJob?.cancel()
        wavPlayer.stop()
        _uiState.update {
            it.copy(
                previewingPackId = null,
                playingPostId = null,
                playingRecordingId = null,
                recordingPlaybackProgress = 0f,
                isMiniPlayerPlaying = false
            )
        }
    }

    fun toggleLikePost(postId: String) {
        val updated = _uiState.value.feedPosts.map { post ->
            if (post.id == postId) {
                val nextLiked = !post.isLiked
                post.copy(
                    isLiked = nextLiked,
                    likesCount = (post.likesCount + if (nextLiked) 1 else -1).coerceAtLeast(0)
                )
            } else post
        }
        _uiState.update { it.copy(feedPosts = updated) }
    }

    fun toggleFollowAuthor(authorName: String) {
        val nextFollowed = if (_uiState.value.followedAuthors.contains(authorName)) {
            _uiState.value.followedAuthors - authorName
        } else {
            _uiState.value.followedAuthors + authorName
        }
        storage.saveStringSet("followed_authors", nextFollowed)
        val updatedPosts = _uiState.value.feedPosts.map {
            it.copy(isFollowingAuthor = nextFollowed.contains(it.authorName))
        }
        _uiState.update { it.copy(followedAuthors = nextFollowed, feedPosts = updatedPosts) }
    }

    fun addCommentToPost(postId: String, commentText: String) {
        val clean = commentText.trim()
        if (clean.isBlank()) return
        val newComment = FeedComment(
            id = UUID.randomUUID().toString(),
            author = _uiState.value.userName,
            text = clean,
            timeAgo = "Just now"
        )
        val updated = _uiState.value.feedPosts.map { post ->
            if (post.id == postId) post.copy(comments = post.comments + newComment) else post
        }
        _uiState.update { it.copy(feedPosts = updated) }
    }

    fun sharePost(post: CommunityPost) {
        ExportService.shareText(
            context,
            post.trackTitle,
            "🔥 Listen to \"${post.trackTitle}\" by ${post.authorName} (${post.bpm} BPM, ${post.genre.label}) on Drum Pads 24!"
        )
    }

    fun publishRecordingToFeed(recording: RecordingItem, caption: String) {
        val name = _uiState.value.userName
        val initials = name.split(" ").mapNotNull { it.firstOrNull()?.uppercaseChar() }.take(2).joinToString("").ifBlank { "ME" }
        val pack = _uiState.value.allSoundPacks.firstOrNull { it.id == recording.packId }
        val newPost = CommunityPost(
            id = "user_post_${UUID.randomUUID().toString().take(8)}",
            authorName = name,
            authorInitials = initials,
            avatarStart = 0xFFFFD600,
            avatarEnd = 0xFFFF2A85,
            timeAgo = "Just now",
            caption = caption.trim().ifBlank { "Just cooked this beat on ${recording.packName} (${recording.bpm} BPM) 🔥🥁" },
            trackTitle = recording.title,
            packId = recording.packId,
            genre = pack?.genre ?: _uiState.value.activeGenre,
            bpm = recording.bpm,
            likesCount = 1,
            isLiked = true,
            isUserPost = true,
            recordingFilePath = recording.filePath
        )
        val updated = listOf(newPost) + _uiState.value.feedPosts
        storage.saveUserPosts(updated)
        _uiState.update { it.copy(feedPosts = updated) }
        showStatus("🚀 Published \"${recording.title}\" to Community Feed!")
    }

    fun startCrewTutorial(post: CommunityPost) {
        val pack = _uiState.value.allSoundPacks.firstOrNull { it.id == post.packId } ?: SoundPackCatalog.getPackById(post.packId)
        openSoundPack(pack, startInClips = false)
        val steps = post.tutorialSteps.ifEmpty {
            listOf(TutorialStep(0, "Kick"), TutorialStep(4, "Hat"), TutorialStep(2, "Snare"), TutorialStep(6, "Bass"))
        }
        _uiState.update {
            it.copy(
                selectedBank = PadBank.BANK_A,
                isClipsMode = false,
                activeTutorial = TutorialSessionState(
                    postId = post.id,
                    title = post.trackTitle,
                    steps = steps
                )
            )
        }
        showStatus("🎓 Tap the glowing cyan guide pad to play!")
    }

    fun exitTutorial() {
        _uiState.update { it.copy(activeTutorial = null) }
    }

    // ------------------------------------------------------------------
    // NAVIGATION, PROFILE & SETTINGS
    // ------------------------------------------------------------------
    fun selectTab(tab: NavTab) {
        _uiState.update { it.copy(selectedTab = tab, activeScreen = ActiveScreen.MAIN) }
    }

    fun navigateTo(screen: ActiveScreen) {
        if (_uiState.value.activeScreen == ActiveScreen.PAD_PLAYER && _uiState.value.isPadLocked && screen != ActiveScreen.PAD_PLAYER) {
            showStatus("🔒 Pads are locked — tap Lock icon to unlock")
            return
        }
        _uiState.update { it.copy(activeScreen = screen) }
    }

    fun selectBank(bank: PadBank) {
        _uiState.update { it.copy(selectedBank = bank) }
    }

    fun setPadLocked(locked: Boolean) {
        _uiState.update { it.copy(isPadLocked = locked) }
        showStatus(if (locked) "🔒 Performance Lock ON" else "🔓 Performance Lock OFF")
    }

    fun toggleHoldMode() {
        val next = !_uiState.value.isHoldMode
        _uiState.update { it.copy(isHoldMode = next) }
        showStatus(if (next) "HOLD Mode ON (release stops pad)" else "HOLD Mode OFF (One-Shot)")
    }

    fun setHapticEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isHapticEnabled = enabled) }
    }

    fun setLightShowEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isLightShowEnabled = enabled) }
    }

    fun setShowPadLabels(enabled: Boolean) {
        _uiState.update { it.copy(showPadLabels = enabled) }
    }

    fun toggleSettings(open: Boolean) {
        _uiState.update {
            if (open) it.copy(activeScreen = ActiveScreen.SETTINGS, isSettingsOpen = true)
            else it.copy(activeScreen = ActiveScreen.MAIN, isSettingsOpen = false)
        }
    }

    fun updateProfile(name: String, bio: String, link: String) {
        val cleanName = name.trim().ifBlank { "Beat Maker" }
        storage.saveProfile(cleanName, bio.trim(), link.trim())
        _uiState.update {
            it.copy(userName = cleanName, userBio = bio.trim(), userLink = link.trim())
        }
        showStatus("Profile updated")
    }

    fun setStressTest(enabled: Boolean) {
        stressJob?.cancel()
        audioEngine.setStressTest(enabled)
        if (enabled) {
            _uiState.update { it.copy(isStressTestActive = true, stressSecondsRemaining = 60) }
            stressJob = viewModelScope.launch(Dispatchers.Default) {
                var remaining = 60
                while (remaining > 0 && isActive) {
                    delay(1000)
                    remaining--
                    _uiState.update { it.copy(stressSecondsRemaining = remaining, xrunCount = audioEngine.getXRunCount()) }
                }
                audioEngine.setStressTest(false)
                _uiState.update { it.copy(isStressTestActive = false, stressSecondsRemaining = 0) }
            }
        } else {
            _uiState.update { it.copy(isStressTestActive = false, stressSecondsRemaining = 0) }
        }
    }

    fun showStatus(message: String) {
        toastJob?.cancel()
        _uiState.update { it.copy(statusMessage = message) }
        toastJob = viewModelScope.launch {
            delay(2800)
            _uiState.update { if (it.statusMessage == message) it.copy(statusMessage = null) else it }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stressJob?.cancel()
        clipLoopJob?.cancel()
        recordingTimerJob?.cancel()
        previewJob?.cancel()
        playbackProgressJob?.cancel()
        meterJob?.cancel()
        wavPlayer.stop()
        if (micRecorder.isRecording()) micRecorder.stop()
        audioEngine.stop()
    }
}
