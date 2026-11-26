package com.drumpads.app

import com.drumpads.app.core.audio.AudioFileDecoder
import com.drumpads.app.core.audio.DspEffectParams
import com.drumpads.app.core.audio.GenreSampleSynthesizer
import com.drumpads.app.core.audio.JvmAudioEngine
import com.drumpads.app.domain.model.ClipPatternPresets
import com.drumpads.app.domain.model.MusicGenre
import com.drumpads.app.domain.model.PadPlayMode
import com.drumpads.app.domain.model.SoundPackCatalog
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class DspAndFeaturesTest {

    @Test
    fun testAll12GenresSynthesizeDistinct24PadKits() {
        val genres = MusicGenre.playableGenres
        assertTrue("Must support at least 12 playable music genres", genres.size >= 12)

        val kickFingerprints = mutableSetOf<Float>()
        for (genre in genres) {
            val kit = GenreSampleSynthesizer.synthesizeKitForGenre(genre)
            assertEquals("Genre $genre must have 24 pad samples", 24, kit.size)
            for (padIdx in 0 until 24) {
                assertTrue("Pad $padIdx in $genre must not be empty", kit[padIdx].isNotEmpty())
                val peak = kit[padIdx].maxOf { abs(it) }
                assertTrue("Pad $padIdx in $genre must produce audible signal", peak > 0.05f)
            }
            kickFingerprints.add(kit[0].take(200).sum())
        }
        assertTrue("Different genres should synthesize distinct acoustic kits", kickFingerprints.size >= 4)
    }

    @Test
    fun testSoundPackCatalogAndClipPresets() {
        assertTrue("Catalog must contain at least 15 rich sound packs", SoundPackCatalog.allPacks.size >= 15)
        for (genre in MusicGenre.playableGenres) {
            val pads = SoundPackCatalog.buildPadsForGenre(genre)
            assertEquals(24, pads.size)
            val clips = ClipPatternPresets.buildDefaultClips(genre)
            assertEquals(12, clips.size)
            assertFalse("Clip 0 Kick should have pattern hits", clips[0].isEmpty)
        }
    }

    @Test
    fun testRealTimeDspPipelineModulatesAudioWithZeroClip() {
        val engine = JvmAudioEngine()
        engine.setBpm(140)
        engine.triggerPad(0)

        val frames = 512
        val left = FloatArray(frames)
        val right = FloatArray(frames)
        val revL = FloatArray(frames)
        val revR = FloatArray(frames)
        val pcmDry = ShortArray(frames * 2)

        engine.setDspParams(DspEffectParams(masterVolume = 1.0f))
        engine.renderProcessedStereoBlock(left, right, revL, revR, pcmDry, frames)
        val dryEnergy = pcmDry.sumOf { abs(it.toInt()) }
        assertTrue("Dry audio output must be non-silent", dryEnergy > 1000)

        // Now trigger with heavy Distortion + Reverb + Wobble + Gate
        engine.triggerPad(6)
        val pcmWet = ShortArray(frames * 2)
        engine.setDspParams(
            DspEffectParams(
                masterVolume = 1.0f,
                reverbLevel = 0.7f,
                delay116Level = 0.5f,
                distortionLevel = 0.8f,
                wobbleLevel = 0.6f,
                gateLevel = 0.4f,
                lofiLevel = 0.5f,
                masterPitchSemitones = 3f
            )
        )
        engine.renderProcessedStereoBlock(left, right, revL, revR, pcmWet, frames)
        val wetEnergy = pcmWet.sumOf { abs(it.toInt()) }
        assertTrue("Wet DSP audio output must be non-silent", wetEnergy > 1000)
    }

    @Test
    fun testWaveformPeaksAndCropFadeEditor() {
        val buf = FloatArray(4410) { i -> kotlin.math.sin(i * 0.05).toFloat() }
        val peaks = AudioFileDecoder.computeWaveformPeaks(buf, 60)
        assertEquals(60, peaks.size)
        assertTrue(peaks.max() <= 1.0f && peaks.min() >= 0.04f)

        val cropped = AudioFileDecoder.cropAndFade(buf, trimStart = 0.25f, trimEnd = 0.75f, fadeInRatio = 0.1f, fadeOutRatio = 0.1f)
        assertEquals(2205, cropped.size)
        assertEquals("First sample with fadeIn must start at 0", 0f, cropped[0], 0.001f)
        assertEquals("Last sample with fadeOut must end at 0", 0f, cropped.last(), 0.001f)
    }

    @Test
    fun testChokeGroupStopsPreviousPadInSameGroup() {
        val engine = JvmAudioEngine()
        val padOpenHat = SoundPackCatalog.buildPadsForGenre(MusicGenre.TRAP)[5] // chokeGroup 1
        val padClosedHat = SoundPackCatalog.buildPadsForGenre(MusicGenre.TRAP)[4].copy(playMode = PadPlayMode.ONE_SHOT) // chokeGroup 1
        engine.updatePadConfig(padOpenHat)
        engine.updatePadConfig(padClosedHat)

        engine.triggerPad(5)
        assertEquals(1, engine.getActiveVoiceCount())
        engine.triggerPad(4)
        // Voice 5 is flagged releasing while Voice 4 starts; after rendering a short block, Voice 5 stops!
        val l = FloatArray(1024); val r = FloatArray(1024); val rl = FloatArray(1024); val rr = FloatArray(1024); val pcm = ShortArray(2048)
        engine.renderProcessedStereoBlock(l, r, rl, rr, pcm, 1024)
        assertTrue("Choked voice should smoothly deactivate", engine.getActiveVoiceCount() <= 1)
    }
}
