package com.drumpads.app.core.audio

import android.content.Context
import com.drumpads.app.domain.model.Pad

data class DspEffectParams(
    val masterVolume: Float = 0.85f,
    val lofiLevel: Float = 0.0f,
    val filterCutoff: Float = 1.0f,       // 1.0 = open, 0.0 = dark LPF
    val masterPitchSemitones: Float = 0f, // -12..+12
    val reverbLevel: Float = 0.15f,
    val delay116Level: Float = 0.0f,
    val delay18Level: Float = 0.0f,
    val distortionLevel: Float = 0.0f,
    val gateLevel: Float = 0.0f,
    val wobbleLevel: Float = 0.0f
)

interface AudioEngine {
    fun init()
    fun initWithContext(context: Context) {}
    fun start(): Boolean
    fun stop()
    fun triggerPad(padIndex: Int): Boolean
    fun releasePad(padIndex: Int) {}
    fun stopAllVoices() {}
    fun loadSample(padIndex: Int, pcmData: ShortArray)
    fun loadFloatSample(padIndex: Int, floatData: FloatArray) {}
    fun getSampleBuffer(padIndex: Int): FloatArray = FloatArray(0)
    fun loadAssetSample(padIndex: Int, assetPath: String) {}
    fun updatePadConfig(pad: Pad) {}
    fun setDspParams(params: DspEffectParams) {}
    fun setBpm(bpm: Int) {}
    fun setAudioTapListener(listener: ((ShortArray, Int) -> Unit)?) {}
    fun getPeakLevel(): Float = 0f
    fun playMetronomeClick(accent: Boolean) {}
    fun setStressTest(enable: Boolean)
    fun isStressTestActive(): Boolean
    fun getXRunCount(): Int
    fun getActiveVoiceCount(): Int
}
