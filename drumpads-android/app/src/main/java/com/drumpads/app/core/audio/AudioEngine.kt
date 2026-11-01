package com.drumpads.app.core.audio

import android.content.Context

interface AudioEngine {
    fun init()
    /** Wire up SoundPool with Android Context. No-op in unit tests. */
    fun initWithContext(context: Context) {}
    fun start(): Boolean
    fun stop()
    fun triggerPad(padIndex: Int): Boolean
    fun loadSample(padIndex: Int, pcmData: ShortArray)
    /** Load a WAV file from assets into SoundPool. No-op in unit tests. */
    fun loadAssetSample(padIndex: Int, assetPath: String) {}
    fun setStressTest(enable: Boolean)
    fun isStressTestActive(): Boolean
    fun getXRunCount(): Int
    fun getActiveVoiceCount(): Int
}
