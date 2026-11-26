package com.drumpads.app.core.audio

import com.drumpads.app.domain.model.MusicGenre
import java.util.Random
import kotlin.math.*

/**
 * Procedurally synthesizes genuinely distinct, musical 44.1kHz FloatArray samples
 * tailored to each of the 12 music genres so every Sound Pack has an authentic sonic identity.
 */
object GenreSampleSynthesizer {

    private const val SR = 44100

    fun synthesizeKitForGenre(genre: MusicGenre, packId: String = ""): Array<FloatArray> {
        val kit = Array(24) { FloatArray(0) }
        val packHash = if (packId.isBlank()) genre.ordinal * 9973L else (packId.hashCode().toLong() * 31337L)
        val rng = Random(packHash + 1337L)
        // Unique musical root transposition (-5 to +7 semitones) per Sound Pack
        val semitoneShift = if (packId.isBlank()) 0 else {
            val table = intArrayOf(0, -3, 2, -5, 4, 7, -2, 5, 1, -4, 3, 6)
            table[abs(packId.hashCode()) % table.size]
        }
        val keyRatio = 2.0.pow(semitoneShift / 12.0)

        // 0..5: DRUMS
        kit[0] = makeKick(genre, deep = false)
        kit[1] = makeKick(genre, deep = true)
        kit[2] = makeSnare(genre, electronic = false, rng = rng)
        kit[3] = makeSnare(genre, electronic = true, rng = rng)
        kit[4] = makeHiHat(genre, open = false, rng = rng)
        kit[5] = makeHiHat(genre, open = true, rng = rng)

        // 6..11: BASS (or Phonk Cowbells!)
        val bassFreqs = when (genre) {
            MusicGenre.PHONK -> doubleArrayOf(523.25, 622.25, 698.46, 783.99, 932.33, 55.0)
            MusicGenre.DUBSTEP -> doubleArrayOf(49.0, 58.27, 65.41, 73.42, 87.31, 41.2)
            MusicGenre.DNB -> doubleArrayOf(55.0, 61.74, 65.41, 73.42, 82.41, 98.0)
            MusicGenre.LOFI -> doubleArrayOf(65.41, 73.42, 82.41, 98.0, 110.0, 130.81)
            else -> doubleArrayOf(55.0, 65.41, 73.42, 82.41, 98.0, 110.0)
        }
        for (i in 0 until 6) {
            kit[6 + i] = makeBass(genre, bassFreqs[i] * keyRatio, i)
        }

        // 12..17: MELODY / CHORDS / LEADS
        val melodyFreqs = when (genre) {
            MusicGenre.PHONK, MusicGenre.TRAP, MusicGenre.DUBSTEP ->
                doubleArrayOf(261.63, 311.13, 349.23, 392.00, 466.16, 523.25) // Minor pentatonic
            MusicGenre.LOFI, MusicGenre.RNB ->
                doubleArrayOf(261.63, 293.66, 329.63, 392.00, 440.00, 493.88) // Maj9 jazzy
            MusicGenre.SYNTHWAVE, MusicGenre.HOUSE, MusicGenre.POP ->
                doubleArrayOf(220.00, 261.63, 293.66, 329.63, 392.00, 440.00) // Warm progression
            else ->
                doubleArrayOf(261.63, 293.66, 329.63, 392.00, 440.00, 523.25)
        }
        for (i in 0 until 6) {
            kit[12 + i] = makeMelody(genre, melodyFreqs[i] * keyRatio, i)
        }

        // 18..23: PERC / FX / VOCAL CHOPS
        kit[18] = makeTom(115.0, 68.0, genre)
        kit[19] = makeTom(195.0, 125.0, genre)
        kit[20] = makeCrash(rng)
        kit[21] = makeFxSweep(genre)
        kit[22] = makeVocalFormant(220.0, 740.0, 1320.0, genre)
        kit[23] = makeVocalFormant(265.0, 510.0, 960.0, genre)

        return kit
    }

    private fun makeKick(genre: MusicGenre, deep: Boolean): FloatArray {
        val dur = when (genre) {
            MusicGenre.TRAP, MusicGenre.PHONK -> if (deep) 0.72 else 0.45
            MusicGenre.TECHNO, MusicGenre.HOUSE -> 0.40
            MusicGenre.LOFI, MusicGenre.HIP_HOP -> 0.36
            else -> if (deep) 0.55 else 0.38
        }
        val len = (SR * dur).toInt()
        val buf = FloatArray(len)
        val fStart = when (genre) {
            MusicGenre.TECHNO -> 220.0
            MusicGenre.DUBSTEP -> 200.0
            MusicGenre.LOFI -> 125.0
            else -> if (deep) 140.0 else 175.0
        }
        val fEnd = if (deep) 38.0 else 48.0
        val pitchDecay = if (genre == MusicGenre.TRAP || genre == MusicGenre.PHONK) 24.0 else 32.0
        val ampDecay = when {
            deep && (genre == MusicGenre.TRAP || genre == MusicGenre.PHONK) -> 3.2
            deep -> 5.5
            else -> 9.5
        }
        val drive = when (genre) {
            MusicGenre.PHONK, MusicGenre.TECHNO, MusicGenre.DUBSTEP -> 2.4
            MusicGenre.TRAP -> 1.8
            else -> 1.3
        }
        var phase = 0.0
        for (i in 0 until len) {
            val t = i.toDouble() / SR
            val f = fEnd + (fStart - fEnd) * exp(-t * pitchDecay)
            phase += 2.0 * PI * f / SR
            val click = if (t < 0.004) sin(2.0 * PI * 1800.0 * t) * (1.0 - t / 0.004) * 0.35 else 0.0
            val raw = (sin(phase) + click) * exp(-t * ampDecay)
            buf[i] = tanh(raw * drive).toFloat().coerceIn(-1f, 1f)
        }
        return buf
    }

    private fun makeSnare(genre: MusicGenre, electronic: Boolean, rng: Random): FloatArray {
        val dur = if (genre == MusicGenre.SYNTHWAVE || genre == MusicGenre.DUBSTEP) 0.38 else 0.26
        val len = (SR * dur).toInt()
        val buf = FloatArray(len)
        val toneFreq = when (genre) {
            MusicGenre.DNB -> 245.0
            MusicGenre.LOFI -> 165.0
            MusicGenre.RNB -> 310.0
            else -> if (electronic) 225.0 else 190.0
        }
        var lpState = 0.0
        val lpAlpha = if (genre == MusicGenre.LOFI) 0.22 else 0.65
        for (i in 0 until len) {
            val t = i.toDouble() / SR
            val noiseRaw = rng.nextDouble() * 2.0 - 1.0
            lpState += lpAlpha * (noiseRaw - lpState)
            val noiseEnv = if (genre == MusicGenre.SYNTHWAVE && t < 0.14) 0.85 else exp(-t * if (electronic) 16.0 else 21.0)
            // Clap micro-flam burst if electronic
            val clapBurst = if (electronic && (t in 0.0..0.008 || t in 0.012..0.020 || t in 0.024..0.032)) 1.3 else 1.0
            val body = sin(2.0 * PI * (toneFreq * exp(-t * 8.0) + 90.0) * t) * exp(-t * 24.0) * (if (electronic) 0.45 else 0.65)
            val sample = body + lpState * noiseEnv * clapBurst * 0.7
            buf[i] = tanh(sample * 1.4).toFloat().coerceIn(-1f, 1f)
        }
        return buf
    }

    private fun makeHiHat(genre: MusicGenre, open: Boolean, rng: Random): FloatArray {
        val dur = if (open) 0.38 else 0.09
        val len = (SR * dur).toInt()
        val buf = FloatArray(len)
        val decay = if (open) 8.5 else if (genre == MusicGenre.TRAP || genre == MusicGenre.PHONK) 58.0 else 42.0
        var prev = 0.0
        for (i in 0 until len) {
            val t = i.toDouble() / SR
            val white = rng.nextDouble() * 2.0 - 1.0
            val hp = white - prev * 0.85 // crisp high-pass metallic sheen
            prev = white
            val metallic = sin(2.0 * PI * 3400.0 * t) * 0.18 + sin(2.0 * PI * 5800.0 * t) * 0.15
            val s = (hp * 0.72 + metallic) * exp(-t * decay)
            buf[i] = (s * 0.85).toFloat().coerceIn(-1f, 1f)
        }
        return buf
    }

    private fun makeBass(genre: MusicGenre, freq: Double, slot: Int): FloatArray {
        val dur = if (genre == MusicGenre.DUBSTEP || genre == MusicGenre.DNB) 0.65 else 0.52
        val len = (SR * dur).toInt()
        val buf = FloatArray(len)
        var filterL = 0.0
        for (i in 0 until len) {
            val t = i.toDouble() / SR
            val env = min(1.0, t / 0.006) * exp(-t * 3.8)
            val s = when (genre) {
                MusicGenre.PHONK -> {
                    if (slot < 5) {
                        // Iconic 808 Memphis Cowbell (dual square/triangle partials 540/800 ratio)
                        val f1 = freq
                        val f2 = freq * 1.482
                        val sq1 = if (sin(2.0 * PI * f1 * t) >= 0) 1.0 else -1.0
                        val sq2 = if (sin(2.0 * PI * f2 * t) >= 0) 1.0 else -1.0
                        val raw = (sq1 * 0.55 + sq2 * 0.45) * exp(-t * 9.5)
                        filterL += 0.25 * (raw - filterL)
                        tanh(filterL * 2.1)
                    } else {
                        // Distorted Phonk 808 sub
                        tanh(sin(2.0 * PI * freq * t) * 2.8) * env
                    }
                }
                MusicGenre.DUBSTEP -> {
                    // FM Wobble growl bass
                    val lfo = 0.5 + 0.5 * sin(2.0 * PI * (4.0 + slot) * t)
                    val mod = sin(2.0 * PI * freq * 2.0 * t) * (1.5 + lfo * 4.5)
                    val carrier = sin(2.0 * PI * freq * t + mod)
                    val sub = sin(2.0 * PI * (freq * 0.5) * t) * 0.5
                    tanh((carrier * 0.7 + sub) * 1.8) * env
                }
                MusicGenre.TECHNO -> {
                    // Acid 303 resonant saw bass
                    val saw = 2.0 * ((freq * t) - floor(0.5 + freq * t))
                    val cutoff = 0.06 + 0.45 * exp(-t * 9.0)
                    filterL += cutoff * (saw - filterL)
                    tanh(filterL * 2.2) * env
                }
                MusicGenre.DNB -> {
                    // Detuned Reese bass
                    val s1 = 2.0 * ((freq * 0.994 * t) - floor(0.5 + freq * 0.994 * t))
                    val s2 = 2.0 * ((freq * 1.006 * t) - floor(0.5 + freq * 1.006 * t))
                    filterL += 0.14 * (((s1 + s2) * 0.5) - filterL)
                    tanh(filterL * 2.0) * env
                }
                else -> {
                    // Warm punchy electronic bass
                    val fund = sin(2.0 * PI * freq * t)
                    val harm = 0.35 * sin(4.0 * PI * freq * t) * exp(-t * 8.0)
                    val harm3 = 0.15 * sin(6.0 * PI * freq * t) * exp(-t * 14.0)
                    tanh((fund + harm + harm3) * 1.35) * env
                }
            }
            buf[i] = s.toFloat().coerceIn(-1f, 1f)
        }
        return buf
    }

    private fun makeMelody(genre: MusicGenre, freq: Double, slot: Int): FloatArray {
        val dur = if (genre == MusicGenre.LOFI || genre == MusicGenre.RNB) 0.75 else 0.48
        val len = (SR * dur).toInt()
        val buf = FloatArray(len)
        var lp = 0.0
        for (i in 0 until len) {
            val t = i.toDouble() / SR
            val attack = min(1.0, t / 0.008)
            val s = when (genre) {
                MusicGenre.LOFI, MusicGenre.RNB -> {
                    // Warm Rhodes electric piano triad chord!
                    val n1 = sin(2.0 * PI * freq * t)
                    val n2 = sin(2.0 * PI * freq * 1.1892 * t) // minor 3rd
                    val n3 = sin(2.0 * PI * freq * 1.4983 * t) // 5th
                    val bell = sin(2.0 * PI * freq * 4.0 * t) * exp(-t * 25.0) * 0.25
                    val vibrato = 1.0 + 0.004 * sin(2.0 * PI * 4.5 * t)
                    ((n1 + n2 * 0.75 + n3 * 0.7 + bell) * 0.38 * vibrato) * attack * exp(-t * 3.2)
                }
                MusicGenre.SYNTHWAVE, MusicGenre.EDM -> {
                    // Supersaw synth lead / stab
                    val d1 = 2.0 * ((freq * 0.995 * t) - floor(0.5 + freq * 0.995 * t))
                    val d2 = 2.0 * ((freq * t) - floor(0.5 + freq * t))
                    val d3 = 2.0 * ((freq * 1.005 * t) - floor(0.5 + freq * 1.005 * t))
                    val mix = (d1 + d2 + d3) / 3.0
                    val alpha = 0.12 + 0.38 * exp(-t * 7.0)
                    lp += alpha * (mix - lp)
                    lp * 0.85 * attack * exp(-t * 4.5)
                }
                MusicGenre.HOUSE -> {
                    // Classic M1 House piano chord stab
                    val c1 = sin(2.0 * PI * freq * t) + 0.3 * sin(4.0 * PI * freq * t)
                    val c2 = sin(2.0 * PI * freq * 1.2599 * t)
                    val c3 = sin(2.0 * PI * freq * 1.4983 * t)
                    ((c1 + c2 + c3) * 0.33) * attack * exp(-t * 7.5)
                }
                else -> {
                    // Bell / Pluck lead
                    val carrier = sin(2.0 * PI * freq * t + 1.2 * exp(-t * 14.0) * sin(2.0 * PI * freq * 2.0 * t))
                    carrier * 0.8 * attack * exp(-t * (5.5 + (slot % 3)))
                }
            }
            buf[i] = s.toFloat().coerceIn(-1f, 1f)
        }
        return buf
    }

    private fun makeTom(fStart: Double, fEnd: Double, genre: MusicGenre): FloatArray {
        val len = (SR * 0.32).toInt()
        val buf = FloatArray(len)
        var phase = 0.0
        for (i in 0 until len) {
            val t = i.toDouble() / SR
            val f = fEnd + (fStart - fEnd) * exp(-t * 16.0)
            phase += 2.0 * PI * f / SR
            val boost = if (genre == MusicGenre.SYNTHWAVE) 1.5 else 1.15
            buf[i] = tanh(sin(phase) * exp(-t * 9.5) * boost).toFloat().coerceIn(-1f, 1f)
        }
        return buf
    }

    private fun makeCrash(rng: Random): FloatArray {
        val len = (SR * 0.70).toInt()
        val buf = FloatArray(len)
        var prev = 0.0
        for (i in 0 until len) {
            val t = i.toDouble() / SR
            val w = rng.nextDouble() * 2.0 - 1.0
            val hp = w - prev * 0.78
            prev = w
            val shimmer = sin(2.0 * PI * 4200.0 * t) * 0.12 + sin(2.0 * PI * 6700.0 * t) * 0.10
            buf[i] = ((hp * 0.65 + shimmer) * exp(-t * 4.5)).toFloat().coerceIn(-1f, 1f)
        }
        return buf
    }

    private fun makeFxSweep(genre: MusicGenre): FloatArray {
        val len = (SR * 0.42).toInt()
        val buf = FloatArray(len)
        var phase = 0.0
        val rising = genre == MusicGenre.HOUSE || genre == MusicGenre.POP || genre == MusicGenre.EDM
        for (i in 0 until len) {
            val t = i.toDouble() / SR
            val f = if (rising) 220.0 + 1800.0 * (t / 0.42).pow(2.0) else 1900.0 * exp(-t * 9.0) + 120.0
            phase += 2.0 * PI * f / SR
            val env = sin(PI * (t / 0.42))
            buf[i] = (sin(phase) * env * 0.75).toFloat().coerceIn(-1f, 1f)
        }
        return buf
    }

    private fun makeVocalFormant(pitch: Double, f1: Double, f2: Double, genre: MusicGenre): FloatArray {
        val len = (SR * 0.30).toInt()
        val buf = FloatArray(len)
        val pitchShift = if (genre == MusicGenre.PHONK) 0.78 else if (genre == MusicGenre.POP) 1.2 else 1.0
        val p = pitch * pitchShift
        for (i in 0 until len) {
            val t = i.toDouble() / SR
            val env = min(1.0, t / 0.015) * exp(-t * 6.5)
            val glottal = sin(2.0 * PI * p * t) * 0.4 + sin(2.0 * PI * f1 * t) * 0.35 + sin(2.0 * PI * f2 * t) * 0.25
            buf[i] = tanh(glottal * env * 1.4).toFloat().coerceIn(-1f, 1f)
        }
        return buf
    }
}
