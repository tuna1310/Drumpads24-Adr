package com.drumpads.app.core.audio

interface AudioEngine {
    fun init()
    fun start(): Boolean
    fun stop()
    fun triggerPad(padIndex: Int): Boolean
    fun loadSample(padIndex: Int, pcmData: ShortArray)
    fun setStressTest(enable: Boolean)
    fun isStressTestActive(): Boolean
    fun getXRunCount(): Int
    fun getActiveVoiceCount(): Int
}
