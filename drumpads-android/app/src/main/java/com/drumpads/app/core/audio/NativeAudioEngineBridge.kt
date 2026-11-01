package com.drumpads.app.core.audio

import android.content.Context
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NativeAudioEngineBridge @Inject constructor() : AudioEngine {

    private val jvmFallback = JvmAudioEngine()
    private var isNativeAvailable = false

    init {
        try {
            System.loadLibrary("drumpads_audio")
            isNativeAvailable = true
            Log.i("NativeAudioEngineBridge", "Loaded native drumpads_audio library successfully")
        } catch (t: Throwable) {
            isNativeAvailable = false
            Log.w("NativeAudioEngineBridge", "Native audio library not loaded, using JVM audio engine fallback: ${t.message}")
        }
    }

    override fun init() {
        if (isNativeAvailable) {
            nativeInit()
        } else {
            jvmFallback.init()
        }
    }

    /** Pass Android Context to JVM fallback so SoundPool can be initialized. */
    override fun initWithContext(context: Context) {
        jvmFallback.initWithContext(context)
    }

    override fun start(): Boolean {
        return if (isNativeAvailable) {
            nativeStart()
        } else {
            jvmFallback.start()
        }
    }

    override fun stop() {
        if (isNativeAvailable) {
            nativeStop()
        } else {
            jvmFallback.stop()
        }
    }

    override fun triggerPad(padIndex: Int): Boolean {
        return if (isNativeAvailable) {
            nativeTriggerPad(padIndex)
        } else {
            jvmFallback.triggerPad(padIndex)
        }
    }

    override fun loadSample(padIndex: Int, pcmData: ShortArray) {
        if (isNativeAvailable) {
            nativeLoadSample(padIndex, pcmData, pcmData.size)
        } else {
            jvmFallback.loadSample(padIndex, pcmData)
        }
    }

    /** Delegate asset loading to JVM fallback's SoundPool. */
    override fun loadAssetSample(padIndex: Int, assetPath: String) {
        jvmFallback.loadAssetSample(padIndex, assetPath)
    }

    override fun setStressTest(enable: Boolean) {
        if (isNativeAvailable) {
            nativeSetStressTest(enable)
        } else {
            jvmFallback.setStressTest(enable)
        }
    }

    override fun isStressTestActive(): Boolean {
        return jvmFallback.isStressTestActive()
    }

    override fun getXRunCount(): Int {
        return if (isNativeAvailable) {
            nativeGetXRunCount()
        } else {
            jvmFallback.getXRunCount()
        }
    }

    override fun getActiveVoiceCount(): Int {
        return jvmFallback.getActiveVoiceCount()
    }

    // JNI Native methods
    private external fun nativeInit()
    private external fun nativeStart(): Boolean
    private external fun nativeStop()
    private external fun nativeTriggerPad(padIndex: Int): Boolean
    private external fun nativeLoadSample(padIndex: Int, pcmData: ShortArray, length: Int)
    private external fun nativeSetStressTest(enable: Boolean)
    private external fun nativeGetXRunCount(): Int
}
