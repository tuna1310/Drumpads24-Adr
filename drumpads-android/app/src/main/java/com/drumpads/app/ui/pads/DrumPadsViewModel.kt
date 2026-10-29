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
                    // Fallback synthesis in AudioEngine is active
                }
            }
        }
    }

    private fun parseWavPcm(stream: InputStream): ShortArray? {
        val bytes = stream.readBytes()
        if (bytes.size < 44) return null
        // Locate 'data' chunk
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

    fun setHapticEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isHapticEnabled = enabled) }
    }

    fun toggleSettings(open: Boolean) {
        _uiState.update { it.copy(isSettingsOpen = open) }
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
        audioEngine.stop()
    }
}
