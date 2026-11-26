package com.drumpads.app.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import com.drumpads.app.domain.model.Pad
import com.drumpads.app.domain.model.PadPlayMode
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.*

/**
 * Pure JVM VoicePool preserved for VoicePoolTest unit tests and real-time voice allocation.
 */
class JvmVoicePool(val maxVoices: Int = 32) {
    data class Voice(
        var padId: Int = -1,
        var sampleData: FloatArray? = null,
        var totalFrames: Int = 0,
        var currentFrame: Int = 0,
        var active: Boolean = false,
        var triggerTimestamp: Long = 0L,
        // Extended real-time DSP fields
        var fractionalFrame: Double = 0.0,
        var startFrame: Int = 0,
        var endFrame: Int = 0,
        var playbackRate: Double = 1.0,
        var gainL: Float = 1.0f,
        var gainR: Float = 1.0f,
        var playMode: PadPlayMode = PadPlayMode.ONE_SHOT,
        var chokeGroup: Int = 0,
        var releasing: Boolean = false,
        var releaseGain: Float = 1.0f,
        var fadeInFrames: Int = 0,
        var fadeOutFrames: Int = 0,
        var padDistortion: Float = 0f,
        var padReverbSend: Float = 0f
    )

    val voices = Array(maxVoices) { Voice() }

    fun allocateVoice(padId: Int, sampleData: FloatArray, timestamp: Long): Int {
        return allocateVoiceWithConfig(
            padId = padId,
            sampleData = sampleData,
            timestamp = timestamp,
            pad = null
        )
    }

    @Synchronized
    fun allocateVoiceWithConfig(
        padId: Int,
        sampleData: FloatArray,
        timestamp: Long,
        pad: Pad?
    ): Int {
        if (sampleData.isEmpty()) return -1

        val choke = pad?.chokeGroup ?: 0
        if (choke > 0) {
            for (i in 0 until maxVoices) {
                if (voices[i].active && voices[i].chokeGroup == choke) {
                    voices[i].releasing = true
                }
            }
        }

        // If the pad is LOOP mode and already playing, toggle it off smoothly
        if (pad?.playMode == PadPlayMode.LOOP) {
            for (i in 0 until maxVoices) {
                if (voices[i].active && voices[i].padId == padId && !voices[i].releasing) {
                    voices[i].releasing = true
                    return i
                }
            }
        }

        var candidateIndex = -1
        for (i in 0 until maxVoices) {
            if (!voices[i].active) {
                candidateIndex = i
                break
            }
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
            val total = sampleData.size
            val sStart = ((pad?.trimStart ?: 0f).coerceIn(0f, 0.98f) * total).toInt()
            val sEnd = ((pad?.trimEnd ?: 1f).coerceIn(0.02f, 1f) * total).toInt().coerceAtLeast(sStart + 1)
            val regionLen = (sEnd - sStart).coerceAtLeast(1)

            val vol = (pad?.volume ?: 1.0f).coerceIn(0f, 1.5f)
            val pan = (pad?.pan ?: 0f).coerceIn(-1f, 1f)
            val panAngle = (pan + 1.0) * (PI / 4.0)
            val semitones = pad?.pitchSemitones ?: 0f
            val rate = 2.0.pow(semitones / 12.0)

            v.padId = padId
            v.sampleData = sampleData
            v.totalFrames = total
            v.currentFrame = sStart
            v.fractionalFrame = sStart.toDouble()
            v.startFrame = sStart
            v.endFrame = sEnd
            v.playbackRate = rate
            v.gainL = (vol * cos(panAngle)).toFloat()
            v.gainR = (vol * sin(panAngle)).toFloat()
            v.playMode = pad?.playMode ?: PadPlayMode.ONE_SHOT
            v.chokeGroup = choke
            v.releasing = false
            v.releaseGain = 1.0f
            v.fadeInFrames = ((pad?.fadeInRatio ?: 0f).coerceIn(0f, 0.5f) * regionLen).toInt()
            v.fadeOutFrames = ((pad?.fadeOutRatio ?: 0f).coerceIn(0f, 0.5f) * regionLen).toInt()
            v.padDistortion = pad?.padDistortion ?: 0f
            v.padReverbSend = pad?.padReverbSend ?: 0f
            v.active = true
            v.triggerTimestamp = timestamp
            return candidateIndex
        }
        return -1
    }

    @Synchronized
    fun releaseHoldPad(padId: Int) {
        for (i in 0 until maxVoices) {
            if (voices[i].active && voices[i].padId == padId && voices[i].playMode == PadPlayMode.HOLD) {
                voices[i].releasing = true
            }
        }
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

    @Synchronized
    fun renderStereoBlock(
        left: FloatArray,
        right: FloatArray,
        reverbSendL: FloatArray,
        reverbSendR: FloatArray,
        numFrames: Int,
        masterPitchRate: Double
    ) {
        left.fill(0f, 0, numFrames)
        right.fill(0f, 0, numFrames)
        reverbSendL.fill(0f, 0, numFrames)
        reverbSendR.fill(0f, 0, numFrames)

        for (vIdx in 0 until maxVoices) {
            val v = voices[vIdx]
            if (!v.active) continue
            val src = v.sampleData ?: continue
            val effectiveRate = (v.playbackRate * masterPitchRate).coerceIn(0.25, 4.0)
            val startF = v.startFrame
            val endF = v.endFrame.coerceAtMost(src.size)
            val regionLen = (endF - startF).coerceAtLeast(1)

            for (i in 0 until numFrames) {
                val pos = v.fractionalFrame
                val idx0 = pos.toInt()
                if (idx0 >= endF) {
                    if (v.playMode == PadPlayMode.LOOP && !v.releasing) {
                        v.fractionalFrame = startF.toDouble()
                        continue
                    } else {
                        v.active = false
                        break
                    }
                }
                val idx1 = (idx0 + 1).coerceAtMost(endF - 1)
                val frac = (pos - idx0).toFloat()
                var sample = src[idx0] + (src[idx1] - src[idx0]) * frac

                // Trim Fade In / Fade Out envelope
                val relPos = idx0 - startF
                if (v.fadeInFrames > 0 && relPos < v.fadeInFrames) {
                    sample *= relPos.toFloat() / v.fadeInFrames.toFloat()
                }
                if (v.fadeOutFrames > 0 && relPos > regionLen - v.fadeOutFrames) {
                    val rem = (regionLen - relPos).coerceAtLeast(0)
                    sample *= rem.toFloat() / v.fadeOutFrames.toFloat()
                }

                // Per-pad distortion
                if (v.padDistortion > 0.01f) {
                    val drive = 1f + v.padDistortion * 4f
                    sample = tanh((sample * drive).toDouble()).toFloat()
                }

                // Release ramp for choke / HOLD release (smooth ~8ms fade)
                if (v.releasing) {
                    v.releaseGain -= 0.003f
                    if (v.releaseGain <= 0f) {
                        v.active = false
                        break
                    }
                    sample *= v.releaseGain
                }

                val sL = sample * v.gainL
                val sR = sample * v.gainR
                left[i] += sL
                right[i] += sR
                if (v.padReverbSend > 0.01f) {
                    reverbSendL[i] += sL * v.padReverbSend
                    reverbSendR[i] += sR * v.padReverbSend
                }

                v.fractionalFrame += effectiveRate
                v.currentFrame = v.fractionalFrame.toInt()
            }
        }
    }

    fun getActiveVoiceCount(): Int = voices.count { it.active }

    @Synchronized
    fun reset() {
        for (v in voices) {
            v.active = false
            v.currentFrame = 0
            v.fractionalFrame = 0.0
            v.padId = -1
        }
    }
}

/**
 * Real-time low-latency Android AudioTrack DSP Engine (44.1kHz Stereo, 32 Voices).
 * Supports ALL 10 master DSP FX (Volume, Lo-Fi, Filter, Pitch, Reverb, Delay 1/16,
 * Delay 1/8, Distortion, Gate, Wobble), Per-Pad Pitch/Trim/Fade/Pan/Choke/Loop/Hold,
 * and live post-FX Audio Tap for real WAV recording!
 */
class JvmAudioEngine : AudioEngine {

    companion object {
        const val SAMPLE_RATE = 44100
        private const val BLOCK_FRAMES = 256 // ~5.8ms per DSP render block
        private const val MAX_SLOTS = 32 // 0..23 pads, 24..29 quick FX, 30..31 metronome
    }

    private val voicePool = JvmVoicePool(32)
    private val sampleBuffers = Array(MAX_SLOTS) { FloatArray(0) }
    private val padConfigs = Array<Pad?>(MAX_SLOTS) { null }
    private val isRunning = AtomicBoolean(false)
    private val stressTestActive = AtomicBoolean(false)
    private val xrunCount = AtomicInteger(0)
    private val timestampSequence = AtomicLong(0)

    @Volatile
    private var dspParams = DspEffectParams()

    @Volatile
    private var currentBpm: Int = 120

    @Volatile
    private var audioTapListener: ((ShortArray, Int) -> Unit)? = null

    @Volatile
    private var latestPeakLevel: Float = 0f

    private var appContext: Context? = null
    private var audioTrack: AudioTrack? = null
    private var renderThread: Thread? = null

    // DSP State buffers (pre-allocated once, zero GC allocations in render loop!)
    private val delayBufL = FloatArray(SAMPLE_RATE * 2) // 2 seconds stereo delay
    private val delayBufR = FloatArray(SAMPLE_RATE * 2)
    private var delayWritePos = 0

    // Schroeder 4-comb Reverb buffers (prime lengths for lush stereo room)
    private val combL1 = FloatArray(1557)
    private val combL2 = FloatArray(1617)
    private val combL3 = FloatArray(1491)
    private val combL4 = FloatArray(1422)
    private val combR1 = FloatArray(1580)
    private val combR2 = FloatArray(1640)
    private val combR3 = FloatArray(1514)
    private val combR4 = FloatArray(1445)
    private var cIdx1 = 0; private var cIdx2 = 0; private var cIdx3 = 0; private var cIdx4 = 0
    private val allpassL = FloatArray(556)
    private val allpassR = FloatArray(579)
    private var apIdx = 0

    // State-variable low-pass filter state (Filter + Wobble)
    private var lpL = 0f; private var bpL = 0f
    private var lpR = 0f; private var bpR = 0f

    // Lo-Fi decimation state
    private var lofiHoldL = 0f; private var lofiHoldR = 0f
    private var lofiCounter = 0
    private var lofiNoiseSeed = 123456789

    // Gate & Wobble phase counter (in samples)
    private var dspSampleClock: Long = 0L

    // Metronome clicks (slots 30 = accent, 31 = normal)
    init {
        generateDefaultSamples()
        buildMetronomeClicks()
    }

    private fun buildMetronomeClicks() {
        val accentLen = (SAMPLE_RATE * 0.035).toInt()
        val accent = FloatArray(accentLen)
        val normal = FloatArray(accentLen)
        for (i in 0 until accentLen) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = exp(-t * 110.0)
            accent[i] = (sin(2.0 * PI * 1650.0 * t) * env * 0.65).toFloat()
            normal[i] = (sin(2.0 * PI * 1100.0 * t) * env * 0.45).toFloat()
        }
        sampleBuffers[30] = accent
        sampleBuffers[31] = normal
    }

    private fun generateDefaultSamples() {
        val kit = GenreSampleSynthesizer.synthesizeKitForGenre(com.drumpads.app.domain.model.MusicGenre.EDM)
        for (i in 0 until 24) {
            sampleBuffers[i] = kit[i]
        }
    }

    override fun init() {}

    override fun initWithContext(context: Context) {
        appContext = context.applicationContext
    }

    override fun loadAssetSample(padIndex: Int, assetPath: String) {
        // Handled via parseWavPcm -> loadSample / loadFloatSample so PCM is in AudioTrack engine!
        val ctx = appContext ?: return
        if (padIndex !in 0 until 30) return
        try {
            ctx.assets.open(assetPath).use { stream ->
                val pcm = AudioFileDecoder.parseWavToFloat(stream.readBytes())
                if (pcm != null && pcm.isNotEmpty()) {
                    sampleBuffers[padIndex] = pcm
                }
            }
        } catch (_: Throwable) {
            // Keep synthesized fallback
        }
    }

    override fun start(): Boolean {
        if (isRunning.getAndSet(true)) return true
        // Only start AudioTrack when running on Android with a Context
        if (appContext != null) {
            startAudioTrackLoop()
        }
        return true
    }

    private fun startAudioTrackLoop() {
        if (renderThread?.isAlive == true) return
        try {
            val minBufBytes = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(BLOCK_FRAMES * 4)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(minBufBytes)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .build()

            audioTrack = track
            track.play()

            val t = Thread({
                try {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
                } catch (_: Throwable) {}

                val left = FloatArray(BLOCK_FRAMES)
                val right = FloatArray(BLOCK_FRAMES)
                val revSendL = FloatArray(BLOCK_FRAMES)
                val revSendR = FloatArray(BLOCK_FRAMES)
                val pcmOut = ShortArray(BLOCK_FRAMES * 2)

                while (isRunning.get()) {
                    renderProcessedStereoBlock(left, right, revSendL, revSendR, pcmOut, BLOCK_FRAMES)
                    val written = track.write(pcmOut, 0, pcmOut.size, AudioTrack.WRITE_BLOCKING)
                    if (written < 0) {
                        xrunCount.incrementAndGet()
                    }
                    audioTapListener?.invoke(pcmOut, pcmOut.size)
                }
            }, "DrumPads-AudioDSP")
            t.isDaemon = true
            t.start()
            renderThread = t
        } catch (e: Throwable) {
            android.util.Log.e("JvmAudioEngine", "Failed to start AudioTrack: ${e.message}")
        }
    }

    /**
     * Renders one stereo block through voices + full 10-FX DSP chain into ShortArray PCM16.
     * Public for deterministic DSP unit testing!
     */
    fun renderProcessedStereoBlock(
        left: FloatArray,
        right: FloatArray,
        revSendL: FloatArray,
        revSendR: FloatArray,
        pcmOut: ShortArray,
        numFrames: Int
    ) {
        val fx = dspParams
        val bpm = currentBpm.coerceIn(60, 200)
        val masterPitchRate = 2.0.pow(fx.masterPitchSemitones / 12.0)

        // 1. Mix active voices
        voicePool.renderStereoBlock(left, right, revSendL, revSendR, numFrames, masterPitchRate)

        // Precompute BPM-synced delay tap lengths
        val sixteenthSamples = ((60.0 / bpm / 4.0) * SAMPLE_RATE).toInt().coerceIn(512, SAMPLE_RATE - 1)
        val eighthSamples = (sixteenthSamples * 2).coerceIn(1024, SAMPLE_RATE * 2 - 1)
        val delaySize = delayBufL.size

        // Filter / Wobble setup
        val baseCutoffHz = 180.0 * (100.0).pow(fx.filterCutoff.coerceIn(0f, 1f).toDouble()) // 180Hz..18kHz
        val wobbleFreqHz = (bpm / 60.0) * 2.0 // 8th-note wobble LFO
        val gateStepSamples = sixteenthSamples.coerceAtLeast(1)

        // Lo-Fi setup
        val decimationStep = if (fx.lofiLevel > 0.01f) (1 + (fx.lofiLevel * 10f).toInt()) else 1
        val bitLevels = if (fx.lofiLevel > 0.01f) (32f - fx.lofiLevel * 26f).coerceAtLeast(4f) else 0f

        var blockPeak = 0f

        for (i in 0 until numFrames) {
            var l = left[i]
            var r = right[i]

            // 2. Stereo BPM-Synced Delay (1/16 & 1/8)
            val dPos = delayWritePos
            val r16 = (dPos - sixteenthSamples + delaySize) % delaySize
            val r8 = (dPos - eighthSamples + delaySize) % delaySize
            val delOutL = delayBufL[r16] * fx.delay116Level + delayBufR[r8] * fx.delay18Level
            val delOutR = delayBufR[r16] * fx.delay116Level + delayBufL[r8] * fx.delay18Level
            delayBufL[dPos] = (l + delOutL * 0.42f).coerceIn(-1.5f, 1.5f)
            delayBufR[dPos] = (r + delOutR * 0.42f).coerceIn(-1.5f, 1.5f)
            delayWritePos = (dPos + 1) % delaySize
            l += delOutL * 0.75f
            r += delOutR * 0.75f

            // 3. Real Algorithmic Schroeder Stereo Reverb (Master + Per-Pad sends)
            val revMix = fx.reverbLevel
            val rInL = l * revMix + revSendL[i]
            val rInR = r * revMix + revSendR[i]
            if (revMix > 0.005f || revSendL[i] != 0f || revSendR[i] != 0f ||
                combL1[cIdx1] != 0f || combR1[cIdx1] != 0f
            ) {
                val fb = 0.72f + revMix * 0.14f
                val oL1 = combL1[cIdx1]; combL1[cIdx1] = rInL + oL1 * fb
                val oL2 = combL2[cIdx2]; combL2[cIdx2] = rInL + oL2 * fb
                val oL3 = combL3[cIdx3]; combL3[cIdx3] = rInL + oL3 * fb
                val oL4 = combL4[cIdx4]; combL4[cIdx4] = rInL + oL4 * fb
                val sumL = (oL1 + oL2 + oL3 + oL4) * 0.25f

                val oR1 = combR1[cIdx1]; combR1[cIdx1] = rInR + oR1 * fb
                val oR2 = combR2[cIdx2]; combR2[cIdx2] = rInR + oR2 * fb
                val oR3 = combR3[cIdx3]; combR3[cIdx3] = rInR + oR3 * fb
                val oR4 = combR4[cIdx4]; combR4[cIdx4] = rInR + oR4 * fb
                val sumR = (oR1 + oR2 + oR3 + oR4) * 0.25f

                // Allpass diffusion
                val apOutL = allpassL[apIdx] - 0.5f * sumL
                allpassL[apIdx] = sumL + 0.5f * apOutL
                val apOutR = allpassR[apIdx] - 0.5f * sumR
                allpassR[apIdx] = sumR + 0.5f * apOutR

                l += apOutL * 0.55f
                r += apOutR * 0.55f
            }
            cIdx1 = (cIdx1 + 1) % combL1.size
            cIdx2 = (cIdx2 + 1) % combL2.size
            cIdx3 = (cIdx3 + 1) % combL3.size
            cIdx4 = (cIdx4 + 1) % combL4.size
            apIdx = (apIdx + 1) % allpassL.size

            // 4. Analog Overdrive Distortion
            if (fx.distortionLevel > 0.01f) {
                val drive = 1f + fx.distortionLevel * 5.5f
                l = tanh((l * drive).toDouble()).toFloat()
                r = tanh((r * drive).toDouble()).toFloat()
            }

            // 5. Lo-Fi (Bitcrush + Sample-Rate Decimation + Cassette Tape Dust)
            if (fx.lofiLevel > 0.01f) {
                if (lofiCounter % decimationStep == 0) {
                    lofiHoldL = round(l * bitLevels) / bitLevels
                    lofiHoldR = round(r * bitLevels) / bitLevels
                }
                lofiCounter++
                // Subtle warm vinyl/cassette hiss
                lofiNoiseSeed = lofiNoiseSeed * 1664525 + 1013904223
                val hiss = ((lofiNoiseSeed shr 16) and 0x7FFF) / 32768f - 0.5f
                val tapeNoise = hiss * fx.lofiLevel * 0.018f
                l = l * (1f - fx.lofiLevel * 0.85f) + (lofiHoldL + tapeNoise) * (fx.lofiLevel * 0.85f)
                r = r * (1f - fx.lofiLevel * 0.85f) + (lofiHoldR + tapeNoise) * (fx.lofiLevel * 0.85f)
            }

            // 6. Resonant Low-Pass Filter + Dubstep Wobble LFO
            val useFilter = fx.filterCutoff < 0.98f || fx.wobbleLevel > 0.01f
            if (useFilter) {
                var cutHz = baseCutoffHz
                if (fx.wobbleLevel > 0.01f) {
                    val tSec = dspSampleClock.toDouble() / SAMPLE_RATE
                    val lfo = 0.5 + 0.5 * sin(2.0 * PI * wobbleFreqHz * tSec)
                    val wobbleHz = 160.0 + lfo * 4800.0
                    cutHz = cutHz * (1.0 - fx.wobbleLevel) + wobbleHz * fx.wobbleLevel
                }
                val f = (2.0 * sin(PI * (cutHz.coerceIn(120.0, 14000.0) / SAMPLE_RATE))).toFloat().coerceIn(0.01f, 0.85f)
                val q = if (fx.wobbleLevel > 0.05f) 0.45f else 0.72f // higher resonance during wobble!
                val hpL = l - lpL - q * bpL
                bpL = (bpL + f * hpL).coerceIn(-2f, 2f)
                lpL = (lpL + f * bpL).coerceIn(-2f, 2f)

                val hpR = r - lpR - q * bpR
                bpR = (bpR + f * hpR).coerceIn(-2f, 2f)
                lpR = (lpR + f * bpR).coerceIn(-2f, 2f)

                l = lpL
                r = lpR
            }

            // 7. BPM-Synced Rhythmic Gate (Trance Chopper)
            if (fx.gateLevel > 0.01f) {
                val posInStep = (dspSampleClock % gateStepSamples).toFloat() / gateStepSamples.toFloat()
                val gateOpen = if (posInStep < 0.55f) 1f else if (posInStep < 0.65f) (0.65f - posInStep) * 10f else 0f
                val gateGain = 1f - fx.gateLevel * (1f - gateOpen)
                l *= gateGain
                r *= gateGain
            }

            // 8. Master Volume & Soft Limiter
            l *= fx.masterVolume
            r *= fx.masterVolume
            l = softClip(l)
            r = softClip(r)

            val absMax = max(abs(l), abs(r))
            if (absMax > blockPeak) blockPeak = absMax

            pcmOut[i * 2] = (l * 32767f).toInt().coerceIn(-32768, 32767).toShort()
            pcmOut[i * 2 + 1] = (r * 32767f).toInt().coerceIn(-32768, 32767).toShort()
            dspSampleClock++
        }

        latestPeakLevel = latestPeakLevel * 0.7f + blockPeak * 0.3f
    }

    private fun softClip(x: Float): Float {
        return when {
            x > 0.92f -> 0.92f + 0.08f * tanh(((x - 0.92f) * 8f).toDouble()).toFloat()
            x < -0.92f -> -0.92f + 0.08f * tanh(((x + 0.92f) * 8f).toDouble()).toFloat()
            else -> x
        }
    }

    override fun stop() {
        isRunning.set(false)
        try {
            renderThread?.join(250)
        } catch (_: Throwable) {}
        renderThread = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.release()
        } catch (_: Throwable) {}
        audioTrack = null
        voicePool.reset()
    }

    override fun triggerPad(padIndex: Int): Boolean {
        if (padIndex !in 0 until MAX_SLOTS) return false
        val sample = sampleBuffers[padIndex]
        if (sample.isEmpty()) return false
        val ts = timestampSequence.incrementAndGet()
        return voicePool.allocateVoiceWithConfig(
            padId = padIndex,
            sampleData = sample,
            timestamp = ts,
            pad = padConfigs[padIndex]
        ) >= 0
    }

    override fun releasePad(padIndex: Int) {
        if (padIndex !in 0 until MAX_SLOTS) return
        voicePool.releaseHoldPad(padIndex)
    }

    override fun stopAllVoices() {
        voicePool.reset()
    }

    override fun loadSample(padIndex: Int, pcmData: ShortArray) {
        if (padIndex !in 0 until MAX_SLOTS || pcmData.isEmpty()) return
        val floatData = FloatArray(pcmData.size) { i -> pcmData[i] / 32768.0f }
        sampleBuffers[padIndex] = floatData
    }

    override fun loadFloatSample(padIndex: Int, floatData: FloatArray) {
        if (padIndex !in 0 until MAX_SLOTS || floatData.isEmpty()) return
        sampleBuffers[padIndex] = floatData
    }

    override fun getSampleBuffer(padIndex: Int): FloatArray {
        if (padIndex !in 0 until MAX_SLOTS) return FloatArray(0)
        return sampleBuffers[padIndex]
    }

    override fun updatePadConfig(pad: Pad) {
        if (pad.index in 0 until 24) {
            padConfigs[pad.index] = pad
        }
    }

    override fun setDspParams(params: DspEffectParams) {
        dspParams = params
    }

    override fun setBpm(bpm: Int) {
        currentBpm = bpm.coerceIn(60, 200)
    }

    override fun setAudioTapListener(listener: ((ShortArray, Int) -> Unit)?) {
        audioTapListener = listener
    }

    override fun getPeakLevel(): Float = latestPeakLevel

    override fun playMetronomeClick(accent: Boolean) {
        val slot = if (accent) 30 else 31
        val sample = sampleBuffers[slot]
        if (sample.isNotEmpty()) {
            voicePool.allocateVoiceWithConfig(slot, sample, timestampSequence.incrementAndGet(), null)
        }
    }

    override fun setStressTest(enable: Boolean) {
        stressTestActive.set(enable)
        if (enable) {
            for (v in 0 until 32) triggerPad(v % 24)
        }
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
