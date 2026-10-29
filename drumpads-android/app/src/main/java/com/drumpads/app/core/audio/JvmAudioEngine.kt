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
    private var playbackThread: Thread? = null

    init {
        generateDefaultSamples()
    }

    private fun generateDefaultSamples() {
        val sampleRate = 44100
        val length = (sampleRate * 0.35).toInt()
        val random = java.util.Random(42)

        for (p in 0 until 24) {
            val buf = FloatArray(length)
            when {
                p < 6 -> {
                    // DRUMS: Punchy kick, snappy snare, crisp hi-hat
                    val startFreq = 160.0 - p * 15.0
                    val endFreq = 45.0
                    var phase = 0.0
                    for (i in 0 until length) {
                        val t = i.toDouble() / sampleRate
                        val freq = startFreq * exp(-t * 22.0) + endFreq
                        phase += 2.0 * Math.PI * freq / sampleRate
                        val body = sin(phase) * exp(-t * 12.0)
                        val noise = if (p in 2..5) {
                            (random.nextDouble() * 2.0 - 1.0) * exp(-t * (if (p == 4) 40.0 else 18.0))
                        } else 0.0
                        buf[i] = ((body * 0.7 + noise * 0.4)).toFloat().coerceIn(-1.0f, 1.0f)
                    }
                }
                p < 12 -> {
                    // BASS: Deep warm sub and synth bass
                    val freq = 55.0 + (p - 6) * 12.0
                    for (i in 0 until length) {
                        val t = i.toDouble() / sampleRate
                        val s = (sin(2.0 * Math.PI * freq * t) + 0.3 * sin(4.0 * Math.PI * freq * t)) * exp(-t * 5.0)
                        buf[i] = s.toFloat().coerceIn(-1.0f, 1.0f)
                    }
                }
                p < 18 -> {
                    // MELODY: Pluck leads
                    val freqs = doubleArrayOf(261.63, 293.66, 329.63, 392.00, 440.00, 523.25)
                    val freq = freqs[(p - 12) % freqs.size]
                    for (i in 0 until length) {
                        val t = i.toDouble() / sampleRate
                        val s = (sin(2.0 * Math.PI * freq * t) + 0.2 * sin(6.0 * Math.PI * freq * t)) * exp(-t * 8.0)
                        buf[i] = s.toFloat().coerceIn(-1.0f, 1.0f)
                    }
                }
                else -> {
                    // FX: Laser, crash, toms
                    for (i in 0 until length) {
                        val t = i.toDouble() / sampleRate
                        val freq = 900.0 * exp(-t * 14.0) + 120.0
                        val s = sin(2.0 * Math.PI * freq * t) * exp(-t * 7.0)
                        buf[i] = s.toFloat().coerceIn(-1.0f, 1.0f)
                    }
                }
            }
            samples[p] = buf
        }
    }

    override fun init() {}

    override fun start(): Boolean {
        isRunning.set(true)
        startPlaybackThread()
        return true
    }

    private fun startPlaybackThread() {
        try {
            val sampleRate = 44100
            val minBufferSize = android.media.AudioTrack.getMinBufferSize(
                sampleRate,
                android.media.AudioFormat.CHANNEL_OUT_MONO,
                android.media.AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBufferSize > 0) {
                val audioTrack = android.media.AudioTrack(
                    android.media.AudioManager.STREAM_MUSIC,
                    sampleRate,
                    android.media.AudioFormat.CHANNEL_OUT_MONO,
                    android.media.AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize * 2,
                    android.media.AudioTrack.MODE_STREAM
                )
                audioTrack.play()
                playbackThread = Thread {
                    val chunkSize = 512
                    val floatBuf = FloatArray(chunkSize)
                    val shortBuf = ShortArray(chunkSize)
                    while (isRunning.get()) {
                        if (voicePool.getActiveVoiceCount() > 0) {
                            voicePool.renderMixed(floatBuf, chunkSize)
                            for (i in 0 until chunkSize) {
                                val clamped = floatBuf[i].coerceIn(-1.0f, 1.0f)
                                shortBuf[i] = (clamped * 32767.0f).toInt().toShort()
                            }
                            audioTrack.write(shortBuf, 0, chunkSize)
                        } else {
                            try {
                                Thread.sleep(8)
                            } catch (_: InterruptedException) {
                                break
                            }
                        }
                    }
                    try {
                        audioTrack.stop()
                        audioTrack.release()
                    } catch (_: Throwable) {}
                }.apply {
                    isDaemon = true
                    priority = Thread.MAX_PRIORITY
                    start()
                }
            }
        } catch (_: Throwable) {
            // Running in JVM unit test environment without Android framework
        }
    }

    override fun stop() {
        isRunning.set(false)
        playbackThread?.interrupt()
        playbackThread = null
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
