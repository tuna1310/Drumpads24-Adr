package com.drumpads.app.core.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaRecorder
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Decodes WAV/MP3/M4A/OGG audio files into 44.1kHz FloatArray PCM, records microphone samples
 * via Android AudioRecord, computes real waveform peaks, and performs real crop/fade editing.
 */
object AudioFileDecoder {

    private const val TARGET_SR = 44100
    private const val MAX_SAMPLE_SECONDS = 12
    private const val MAX_FRAMES = TARGET_SR * MAX_SAMPLE_SECONDS

    fun parseWavToFloat(bytes: ByteArray): FloatArray? {
        if (bytes.size < 44) return null
        if (String(bytes, 0, 4) != "RIFF" || String(bytes, 8, 4) != "WAVE") return null

        var channels = 1
        var sampleRate = TARGET_SR
        var bitsPerSample = 16
        var offset = 12

        while (offset + 8 <= bytes.size) {
            val chunkId = String(bytes, offset, 4)
            val chunkSize = ByteBuffer.wrap(bytes, offset + 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
            val dataOffset = offset + 8
            if (chunkSize < 0 || dataOffset > bytes.size) break

            when (chunkId) {
                "fmt " -> {
                    if (dataOffset + 16 <= bytes.size) {
                        val bb = ByteBuffer.wrap(bytes, dataOffset, 16).order(ByteOrder.LITTLE_ENDIAN)
                        bb.short // audioFormat
                        channels = bb.short.toInt().coerceIn(1, 2)
                        sampleRate = bb.int.coerceIn(8000, 96000)
                        bb.int // byteRate
                        bb.short // blockAlign
                        bitsPerSample = bb.short.toInt()
                    }
                }
                "data" -> {
                    val safeBytes = min(chunkSize, bytes.size - dataOffset)
                    if (bitsPerSample == 16 && safeBytes >= 2) {
                        val totalShorts = safeBytes / 2
                        val frameCount = (totalShorts / channels).coerceAtMost(MAX_FRAMES * 2)
                        val shortBuf = ByteBuffer.wrap(bytes, dataOffset, totalShorts * 2)
                            .order(ByteOrder.LITTLE_ENDIAN)
                            .asShortBuffer()
                        val mono = FloatArray(frameCount)
                        if (channels == 1) {
                            for (i in 0 until frameCount) {
                                mono[i] = shortBuf.get(i) / 32768.0f
                            }
                        } else {
                            for (i in 0 until frameCount) {
                                val l = shortBuf.get(i * 2) / 32768.0f
                                val r = shortBuf.get(i * 2 + 1) / 32768.0f
                                mono[i] = (l + r) * 0.5f
                            }
                        }
                        return resampleIfNeeded(mono, sampleRate, TARGET_SR)
                    }
                }
            }
            offset += 8 + chunkSize + (chunkSize and 1)
        }
        return null
    }

    /**
     * Decodes any Android audio URI (WAV, MP3, M4A, AAC, OGG, FLAC) into 44.1kHz FloatArray.
     */
    fun decodeUriToFloat(context: Context, uri: Uri): FloatArray? {
        // 1. Fast path for raw WAV files
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val head = ByteArray(12)
                if (stream.read(head) == 12 && String(head, 0, 4) == "RIFF" && String(head, 8, 4) == "WAVE") {
                    val rest = stream.readBytes()
                    val all = ByteArray(12 + rest.size)
                    System.arraycopy(head, 0, all, 0, 12)
                    System.arraycopy(rest, 0, all, 12, rest.size)
                    val parsed = parseWavToFloat(all)
                    if (parsed != null && parsed.isNotEmpty()) return parsed
                }
            }
        } catch (_: Throwable) {}

        // 2. General path via MediaExtractor + MediaCodec (MP3, M4A, OGG, FLAC, etc.)
        var extractor: MediaExtractor? = null
        var codec: MediaCodec? = null
        try {
            extractor = MediaExtractor()
            extractor.setDataSource(context, uri, null)

            var audioTrackIndex = -1
            var format: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    format = f
                    break
                }
            }
            if (audioTrackIndex < 0 || format == null) return null

            extractor.selectTrack(audioTrackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
            val srcRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) format.getInteger(MediaFormat.KEY_SAMPLE_RATE) else TARGET_SR
            val srcChannels = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) else 1

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val pcmBytes = ByteArrayOutputStream()
            val maxBytes = MAX_FRAMES * srcChannels * 2
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false

            while (!outputDone && pcmBytes.size() < maxBytes) {
                if (!inputDone) {
                    val inIdx = codec.dequeueInputBuffer(10_000)
                    if (inIdx >= 0) {
                        val inBuf = codec.getInputBuffer(inIdx)
                        val sampleSize = if (inBuf != null) extractor.readSampleData(inBuf, 0) else -1
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inIdx, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outIdx = codec.dequeueOutputBuffer(info, 10_000)
                if (outIdx >= 0) {
                    val outBuf = codec.getOutputBuffer(outIdx)
                    if (outBuf != null && info.size > 0) {
                        val chunk = ByteArray(info.size)
                        outBuf.position(info.offset)
                        outBuf.get(chunk)
                        pcmBytes.write(chunk)
                    }
                    codec.releaseOutputBuffer(outIdx, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputDone = true
                    }
                }
            }

            val raw = pcmBytes.toByteArray()
            if (raw.size < 4) return null
            val totalShorts = raw.size / 2
            val shortBuf = ByteBuffer.wrap(raw, 0, totalShorts * 2).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
            val ch = srcChannels.coerceIn(1, 2)
            val frameCount = totalShorts / ch
            val mono = FloatArray(frameCount)
            if (ch == 1) {
                for (i in 0 until frameCount) mono[i] = shortBuf.get(i) / 32768f
            } else {
                for (i in 0 until frameCount) {
                    mono[i] = (shortBuf.get(i * 2) + shortBuf.get(i * 2 + 1)) / 65536f
                }
            }
            return resampleIfNeeded(mono, srcRate, TARGET_SR)
        } catch (e: Throwable) {
            android.util.Log.e("AudioFileDecoder", "Decode failed: ${e.message}")
            return null
        } finally {
            try { codec?.stop(); codec?.release() } catch (_: Throwable) {}
            try { extractor?.release() } catch (_: Throwable) {}
        }
    }

    private fun resampleIfNeeded(input: FloatArray, srcRate: Int, dstRate: Int): FloatArray {
        if (input.isEmpty() || srcRate == dstRate) {
            return if (input.size > MAX_FRAMES) input.copyOf(MAX_FRAMES) else input
        }
        val ratio = srcRate.toDouble() / dstRate.toDouble()
        val outLen = ((input.size / ratio).toInt()).coerceIn(1, MAX_FRAMES)
        val out = FloatArray(outLen)
        for (i in 0 until outLen) {
            val pos = i * ratio
            val i0 = pos.toInt().coerceIn(0, input.lastIndex)
            val i1 = (i0 + 1).coerceAtMost(input.lastIndex)
            val frac = (pos - i0).toFloat()
            out[i] = input[i0] + (input[i1] - input[i0]) * frac
        }
        return out
    }

    /**
     * Computes real normalized peak amplitudes (0f..1f) from an actual PCM sample buffer for waveform rendering.
     */
    fun computeWaveformPeaks(pcm: FloatArray, buckets: Int = 64): FloatArray {
        if (pcm.isEmpty() || buckets <= 0) return FloatArray(buckets) { 0.08f }
        val peaks = FloatArray(buckets)
        val step = pcm.size.toFloat() / buckets.toFloat()
        var maxPeak = 0.001f
        for (b in 0 until buckets) {
            val start = (b * step).toInt().coerceIn(0, pcm.lastIndex)
            val end = ((b + 1) * step).toInt().coerceIn(start + 1, pcm.size)
            var p = 0f
            for (i in start until end) {
                val a = abs(pcm[i])
                if (a > p) p = a
            }
            peaks[b] = p
            if (p > maxPeak) maxPeak = p
        }
        for (b in 0 until buckets) {
            peaks[b] = (peaks[b] / maxPeak).coerceIn(0.04f, 1.0f)
        }
        return peaks
    }

    /**
     * Destructively crops a sample buffer between [trimStart, trimEnd] and applies linear Fade In / Fade Out.
     */
    fun cropAndFade(
        pcm: FloatArray,
        trimStart: Float,
        trimEnd: Float,
        fadeInRatio: Float,
        fadeOutRatio: Float
    ): FloatArray {
        if (pcm.isEmpty()) return pcm
        val s0 = (trimStart.coerceIn(0f, 0.96f) * pcm.size).toInt()
        val s1 = (trimEnd.coerceIn(0.04f, 1f) * pcm.size).toInt().coerceAtLeast(s0 + 16).coerceAtMost(pcm.size)
        val len = s1 - s0
        val out = FloatArray(len)
        val fadeInFrames = (fadeInRatio.coerceIn(0f, 0.5f) * len).toInt()
        val fadeOutFrames = (fadeOutRatio.coerceIn(0f, 0.5f) * len).toInt()

        for (i in 0 until len) {
            var v = pcm[s0 + i]
            if (fadeInFrames > 0 && i < fadeInFrames) {
                v *= i.toFloat() / fadeInFrames.toFloat()
            }
            if (fadeOutFrames > 0 && i >= len - fadeOutFrames) {
                val rem = len - 1 - i
                v *= rem.toFloat() / fadeOutFrames.toFloat()
            }
            out[i] = v
        }
        return out
    }

    fun writeMonoWav(file: File, pcm: FloatArray, sampleRate: Int = TARGET_SR): Boolean {
        return try {
            file.parentFile?.mkdirs()
            val numSamples = pcm.size
            val dataBytes = numSamples * 2
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
                put("RIFF".toByteArray())
                putInt(36 + dataBytes)
                put("WAVE".toByteArray())
                put("fmt ".toByteArray())
                putInt(16)
                putShort(1) // PCM
                putShort(1) // Mono
                putInt(sampleRate)
                putInt(sampleRate * 2)
                putShort(2)
                putShort(16)
                put("data".toByteArray())
                putInt(dataBytes)
            }.array()

            val body = ByteBuffer.allocate(dataBytes).order(ByteOrder.LITTLE_ENDIAN)
            for (s in pcm) {
                body.putShort((s.coerceIn(-1f, 1f) * 32767f).toInt().toShort())
            }
            FileOutputStream(file).use { out ->
                out.write(header)
                out.write(body.array())
            }
            true
        } catch (e: Throwable) {
            android.util.Log.e("AudioFileDecoder", "writeMonoWav failed: ${e.message}")
            false
        }
    }
}

/**
 * Real Android Microphone sampler using AudioRecord (44.1kHz Mono PCM_16BIT)
 * so users can record live voice, beatbox, or acoustic instruments straight onto any pad!
 */
class MicSampleRecorder {
    private var audioRecord: AudioRecord? = null
    @Volatile private var recording = false
    private var captureThread: Thread? = null
    private val captured = ArrayList<Short>(44100 * 5)

    @SuppressLint("MissingPermission")
    fun start(onPeakLevel: (Float) -> Unit = {}): Boolean {
        if (recording) return true
        return try {
            val sr = 44100
            val minBuf = AudioRecord.getMinBufferSize(
                sr,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(4096)

            val rec = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sr,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuf * 2
            )
            if (rec.state != AudioRecord.STATE_INITIALIZED) {
                rec.release()
                return false
            }

            synchronized(captured) { captured.clear() }
            audioRecord = rec
            recording = true
            rec.startRecording()

            captureThread = Thread({
                val chunk = ShortArray(1024)
                val maxSamples = 44100 * 10 // up to 10s pad sample
                while (recording) {
                    val read = rec.read(chunk, 0, chunk.size)
                    if (read > 0) {
                        var peak = 0f
                        synchronized(captured) {
                            for (i in 0 until read) {
                                if (captured.size < maxSamples) {
                                    captured.add(chunk[i])
                                }
                                val a = abs(chunk[i] / 32768f)
                                if (a > peak) peak = a
                            }
                        }
                        onPeakLevel(peak)
                        if (captured.size >= maxSamples) {
                            recording = false
                        }
                    }
                }
            }, "DrumPads-MicSample")
            captureThread?.start()
            true
        } catch (e: Throwable) {
            android.util.Log.e("MicSampleRecorder", "Mic start error: ${e.message}")
            recording = false
            false
        }
    }

    fun stop(): FloatArray {
        recording = false
        try { captureThread?.join(300) } catch (_: Throwable) {}
        captureThread = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Throwable) {}
        audioRecord = null

        val raw = synchronized(captured) {
            FloatArray(captured.size) { i -> captured[i] / 32768f }
        }
        // Normalize recorded mic level gently so it's loud and punchy on the pad
        if (raw.isEmpty()) return raw
        var peak = 0.001f
        for (v in raw) peak = max(peak, abs(v))
        val gain = (0.92f / peak).coerceAtMost(6.0f)
        for (i in raw.indices) raw[i] = (raw[i] * gain).coerceIn(-1f, 1f)
        return raw
    }

    fun isRecording(): Boolean = recording
}
