package com.drumpads.app.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.util.concurrent.ConcurrentHashMap
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
        var candidateIndex = -1
        for (i in 0 until maxVoices) {
            if (!voices[i].active) { candidateIndex = i; break }
        }
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
            v.padId = padId; v.sampleData = sampleData; v.totalFrames = sampleData.size
            v.currentFrame = 0; v.active = true; v.triggerTimestamp = timestamp
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
            for (i in 0 until framesToRender) outputBuffer[i] += src[voice.currentFrame + i]
            voice.currentFrame += framesToRender
            if (voice.currentFrame >= voice.totalFrames) voice.active = false
        }
    }

    fun getActiveVoiceCount(): Int = voices.count { it.active }

    fun reset() {
        for (v in voices) { v.active = false; v.currentFrame = 0; v.padId = -1 }
    }
}

/**
 * JvmAudioEngine — Uses Android SoundPool for low-latency WAV sample playback.
 * Falls back to synthesized tones when SoundPool is unavailable (unit tests).
 */
class JvmAudioEngine : AudioEngine {

    private val voicePool = JvmVoicePool(32)
    private val synthSamples = Array(32) { FloatArray(0) }
    private val isRunning = AtomicBoolean(false)
    private val stressTestActive = AtomicBoolean(false)
    private val xrunCount = AtomicInteger(0)
    private val timestampSequence = AtomicLong(0)

    // SoundPool for real device playback
    private var soundPool: SoundPool? = null
    private val soundPoolIds = ConcurrentHashMap<Int, Int>() // padIndex -> soundId
    private val soundPoolLoaded = ConcurrentHashMap<Int, Boolean>() // padIndex -> loaded?
    private var appContext: Context? = null
    private var volumeLevel: Float = 1.0f
    private var fxEchoLevel: Float = 0.0f
    private var fxReverbLevel: Float = 0.0f

    // Effect state for synthesis fallback
    private val echoBuffer = FloatArray(44100) // 1-second echo buffer
    private var echoWritePos = 0

    init {
        generateSynthSamples()
    }

    /**
     * Initialize with Android Context so SoundPool can be created.
     * Called from NativeAudioEngineBridge or directly from ViewModel on Android.
     */
    override fun initWithContext(context: Context) {
        appContext = context
        buildSoundPool(context)
    }

    private fun buildSoundPool(context: Context) {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(32)
            .setAudioAttributes(attrs)
            .build()
        soundPool?.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                // Find which padIndex this sampleId belongs to
                soundPoolIds.forEach { (padIndex, id) ->
                    if (id == sampleId) soundPoolLoaded[padIndex] = true
                }
            }
        }
    }

    /**
     * Load a WAV file from assets into SoundPool for the given pad index.
     * @param padIndex 0-23
     * @param assetPath relative path in assets (e.g. "starter/samples/kick_punch.wav")
     */
    override fun loadAssetSample(padIndex: Int, assetPath: String) {
        val context = appContext ?: return
        val pool = soundPool ?: return
        try {
            context.assets.openFd(assetPath).use { afd ->
                val soundId = pool.load(afd, 1)
                if (soundId > 0) {
                    soundPoolIds[padIndex] = soundId
                    soundPoolLoaded[padIndex] = false
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("JvmAudioEngine", "Failed to load asset $assetPath: ${e.message}")
        }
    }

    private fun generateSynthSamples() {
        val sampleRate = 44100
        val random = java.util.Random(42)
        for (p in 0 until 32) {
            val length = (sampleRate * 0.35).toInt()
            val buf = FloatArray(length)
            when {
                p < 6 -> {
                    val startFreq = 160.0 - p * 15.0; val endFreq = 45.0; var phase = 0.0
                    for (i in 0 until length) {
                        val t = i.toDouble() / sampleRate
                        val freq = startFreq * exp(-t * 22.0) + endFreq
                        phase += 2.0 * Math.PI * freq / sampleRate
                        val body = sin(phase) * exp(-t * 12.0)
                        val noise = if (p in 2..5) (random.nextDouble() * 2.0 - 1.0) * exp(-t * if (p == 4) 40.0 else 18.0) else 0.0
                        buf[i] = ((body * 0.7 + noise * 0.4)).toFloat().coerceIn(-1.0f, 1.0f)
                    }
                }
                p < 12 -> {
                    val freq = 55.0 + (p - 6) * 12.0
                    for (i in 0 until length) {
                        val t = i.toDouble() / sampleRate
                        buf[i] = ((sin(2.0 * Math.PI * freq * t) + 0.3 * sin(4.0 * Math.PI * freq * t)) * exp(-t * 5.0)).toFloat().coerceIn(-1f, 1f)
                    }
                }
                p < 18 -> {
                    val freqs = doubleArrayOf(261.63, 293.66, 329.63, 392.00, 440.00, 523.25)
                    val freq = freqs[(p - 12) % freqs.size]
                    for (i in 0 until length) {
                        val t = i.toDouble() / sampleRate
                        buf[i] = ((sin(2.0 * Math.PI * freq * t) + 0.2 * sin(6.0 * Math.PI * freq * t)) * exp(-t * 8.0)).toFloat().coerceIn(-1f, 1f)
                    }
                }
                else -> {
                    for (i in 0 until length) {
                        val t = i.toDouble() / sampleRate
                        val freq = 900.0 * exp(-t * 14.0) + 120.0
                        buf[i] = (sin(2.0 * Math.PI * freq * t) * exp(-t * 7.0)).toFloat().coerceIn(-1f, 1f)
                    }
                }
            }
            synthSamples[p] = buf
        }
    }

    override fun init() {}

    override fun start(): Boolean {
        isRunning.set(true)
        return true
    }

    override fun stop() {
        isRunning.set(false)
        soundPool?.release()
        soundPool = null
        voicePool.reset()
    }

    override fun triggerPad(padIndex: Int): Boolean {
        if (padIndex !in 0 until 32) return false

        // Prefer SoundPool on device (low-latency)
        val pool = soundPool
        val soundId = soundPoolIds[padIndex]
        if (pool != null && soundId != null && soundPoolLoaded[padIndex] == true) {
            pool.play(soundId, volumeLevel, volumeLevel, 1, 0, 1.0f)
            return true
        }

        // Fallback to synth voice pool (unit tests / no context)
        val sample = synthSamples[padIndex]
        val ts = timestampSequence.incrementAndGet()
        return voicePool.allocateVoice(padIndex, sample, ts) >= 0
    }

    override fun loadSample(padIndex: Int, pcmData: ShortArray) {
        if (padIndex !in 0 until 32 || pcmData.isEmpty()) return
        val floatData = FloatArray(pcmData.size) { i -> pcmData[i] / 32768.0f }
        synthSamples[padIndex] = floatData
    }

    fun setVolume(level: Float) {
        volumeLevel = level.coerceIn(0f, 1f)
    }

    fun setEchoLevel(level: Float) { fxEchoLevel = level.coerceIn(0f, 1f) }
    fun setReverbLevel(level: Float) { fxReverbLevel = level.coerceIn(0f, 1f) }

    override fun setStressTest(enable: Boolean) {
        stressTestActive.set(enable)
        if (enable) { for (v in 0 until 32) triggerPad(v % 24) }
    }

    override fun isStressTestActive(): Boolean = stressTestActive.get()
    override fun getXRunCount(): Int = xrunCount.get()
    override fun getActiveVoiceCount(): Int = voicePool.getActiveVoiceCount()

    fun renderFrames(numFrames: Int): FloatArray {
        val out = FloatArray(numFrames)
        voicePool.renderMixed(out, numFrames)
        return out
    }

    fun getVoicePool(): JvmVoicePool = voicePool
}
