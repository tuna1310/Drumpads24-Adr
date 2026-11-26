package com.drumpads.app.core.audio

import android.content.Context
import com.drumpads.app.domain.model.Pad
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AudioEngine bridge routing directly through the real-time low-latency AudioTrack DSP engine
 * so all 10 live DSP FX, per-pad Pitch/Trim/Fade/Choke, and post-FX WAV recording work on every Android device.
 */
@Singleton
class NativeAudioEngineBridge @Inject constructor() : AudioEngine {

    private val dspEngine = JvmAudioEngine()

    override fun init() = dspEngine.init()
    override fun initWithContext(context: Context) = dspEngine.initWithContext(context)
    override fun start(): Boolean = dspEngine.start()
    override fun stop() = dspEngine.stop()
    override fun triggerPad(padIndex: Int): Boolean = dspEngine.triggerPad(padIndex)
    override fun releasePad(padIndex: Int) = dspEngine.releasePad(padIndex)
    override fun stopAllVoices() = dspEngine.stopAllVoices()
    override fun loadSample(padIndex: Int, pcmData: ShortArray) = dspEngine.loadSample(padIndex, pcmData)
    override fun loadFloatSample(padIndex: Int, floatData: FloatArray) = dspEngine.loadFloatSample(padIndex, floatData)
    override fun getSampleBuffer(padIndex: Int): FloatArray = dspEngine.getSampleBuffer(padIndex)
    override fun loadAssetSample(padIndex: Int, assetPath: String) = dspEngine.loadAssetSample(padIndex, assetPath)
    override fun updatePadConfig(pad: Pad) = dspEngine.updatePadConfig(pad)
    override fun setDspParams(params: DspEffectParams) = dspEngine.setDspParams(params)
    override fun setBpm(bpm: Int) = dspEngine.setBpm(bpm)
    override fun setAudioTapListener(listener: ((ShortArray, Int) -> Unit)?) = dspEngine.setAudioTapListener(listener)
    override fun getPeakLevel(): Float = dspEngine.getPeakLevel()
    override fun playMetronomeClick(accent: Boolean) = dspEngine.playMetronomeClick(accent)
    override fun setStressTest(enable: Boolean) = dspEngine.setStressTest(enable)
    override fun isStressTestActive(): Boolean = dspEngine.isStressTestActive()
    override fun getXRunCount(): Int = dspEngine.getXRunCount()
    override fun getActiveVoiceCount(): Int = dspEngine.getActiveVoiceCount()
}
