package com.drumpads.app.core.audio

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.exp
import kotlin.math.sin

class JvmVoicePool(val maxVoices: Int = 32) {
    data class Voice(
        var padId: Int = -1,
        var sampleData: FloatArray? = null,
        var totalFrames: Int = 0,
        var currentFrame: Int = 0,
        var active: Boolean = false,
        var triggerTimestamp: Long = 0L
    )

    val voices = Array(maxVoices) { Voice() }

    fun allocateVoice(padId: Int, sampleData: FloatArray, timestamp: Long): Int {
        if (sampleData.isEmpty()) return -1

        // 1. Find inactive voice
        var candidateIndex = -1
        for (i in 0 until maxVoices) {
            if (!voices[i].active) {
                candidateIndex = i
                break
            }
        }

        // 2. If all 32 active: oldest voice stealing
        if (candidateIndex == -1) {
            var oldestTimestamp = Long.MAX_VALUE
            for (i in 0 until maxVoices) {
                if (voices[i].triggerTimestamp < oldestTimestamp) {
                    oldestTimestamp = voices[i].triggerTimestamp
                    candidateIndex = i
                }
            }
        }

        if (candidateIndex in 0 until maxVoices) {
            val v = voices[candidateIndex]
            v.padId = padId
            v.sampleData = sampleData
            v.totalFrames = sampleData.size
            v.currentFrame = 0
            v.active = true
            v.triggerTimestamp = timestamp
            return candidateIndex
        }

        return -1
    }

    fun renderMixed(outputBuffer: FloatArray, numFrames: Int) {
        outputBuffer.fill(0f)
        for (v in 0 until maxVoices) {
            val voice = voices[v]
            if (!voice.active) continue

            val src = voice.sampleData ?: continue
            val remaining = voice.totalFrames - voice.currentFrame
            val framesToRender = minOf(numFrames, remaining)

            for (i in 0 until framesToRender) {
                outputBuffer[i] += src[voice.currentFrame + i]
            }

            voice.currentFrame += framesToRender
            if (voice.currentFrame >= voice.totalFrames) {
                voice.active = false
            }
        }
    }

    fun getActiveVoiceCount(): Int = voices.count { it.active }

    fun reset() {
        for (v in voices) {
            v.active = false
            v.currentFrame = 0
            v.padId = -1
        }
    }
}

class JvmAudioEngine : AudioEngine {
    private val voicePool = JvmVoicePool(32)
    private val samples = Array(24) { FloatArray(0) }
    private val isRunning = AtomicBoolean(false)
    private val stressTestActive = AtomicBoolean(false)
    private val xrunCount = AtomicInteger(0)
    private val timestampSequence = AtomicLong(0)

    init {
        generateDefaultSamples()
    }

    private fun generateDefaultSamples() {
        val sampleRate = 44100
        val length = (sampleRate * 0.3).toInt() // 300ms
        for (p in 0 until 24) {
            val buf = FloatArray(length)
            val freq = 100.0 + p * 25.0
            for (i in 0 until length) {
                val t = i.toDouble() / sampleRate
                buf[i] = (sin(2.0 * Math.PI * freq * t) * exp(-t * 8.0)).toFloat()
            }
            samples[p] = buf
        }
    }

    override fun init() {}

    override fun start(): Boolean {
        isRunning.set(true)
        return true
    }

    override fun stop() {
        isRunning.set(false)
        voicePool.reset()
    }

    override fun triggerPad(padIndex: Int): Boolean {
        if (padIndex !in 0 until 24) return false
        val sample = samples[padIndex]
        val ts = timestampSequence.incrementAndGet()
        val voiceIndex = voicePool.allocateVoice(padIndex, sample, ts)
        return voiceIndex >= 0
    }

    override fun loadSample(padIndex: Int, pcmData: ShortArray) {
        if (padIndex !in 0 until 24 || pcmData.isEmpty()) return
        val floatData = FloatArray(pcmData.size) { i -> pcmData[i] / 32768.0f }
        samples[padIndex] = floatData
    }

    override fun setStressTest(enable: Boolean) {
        stressTestActive.set(enable)
        if (enable) {
            // Trigger 32 voices
            for (v in 0 until 32) {
                triggerPad(v % 24)
            }
        }
    }

    override fun isStressTestActive(): Boolean = stressTestActive.get()

    override fun getXRunCount(): Int = xrunCount.get()

    override fun getActiveVoiceCount(): Int = voicePool.getActiveVoiceCount()

    // Test helper to render frames
    fun renderFrames(numFrames: Int): FloatArray {
        val out = FloatArray(numFrames)
        voicePool.renderMixed(out, numFrames)
        return out
    }

    fun getVoicePool(): JvmVoicePool = voicePool
}
