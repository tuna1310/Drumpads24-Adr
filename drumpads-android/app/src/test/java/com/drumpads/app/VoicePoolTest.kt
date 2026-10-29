package com.drumpads.app

import com.drumpads.app.core.audio.JvmVoicePool
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VoicePoolTest {

    private lateinit var pool: JvmVoicePool
    private val dummySample = FloatArray(1000) { 0.5f }

    @Before
    fun setUp() {
        pool = JvmVoicePool(32)
    }

    @Test
    fun testVoicePoolAllocation() {
        assertEquals(0, pool.getActiveVoiceCount())

        val index0 = pool.allocateVoice(padId = 0, sampleData = dummySample, timestamp = 100L)
        assertTrue("Index should be between 0 and 31", index0 in 0..31)
        assertEquals(1, pool.getActiveVoiceCount())
        assertEquals(0, pool.voices[index0].padId)
        assertTrue(pool.voices[index0].active)
    }

    @Test
    fun testVoicePoolAllocationUpTo32() {
        for (i in 0 until 32) {
            val idx = pool.allocateVoice(padId = i % 24, sampleData = dummySample, timestamp = 100L + i)
            assertTrue("Allocated index must be valid", idx in 0..31)
        }
        assertEquals("Pool must have 32 active voices", 32, pool.getActiveVoiceCount())
    }

    @Test
    fun testVoiceStealingWhenFullAt32() {
        // Fill pool with 32 voices, timestamps 1000 to 1031
        for (i in 0 until 32) {
            pool.allocateVoice(padId = i % 24, sampleData = dummySample, timestamp = 1000L + i)
        }
        assertEquals(32, pool.getActiveVoiceCount())

        // The oldest voice has timestamp 1000L (which was voice index 0)
        val oldestIndex = 0
        assertEquals(1000L, pool.voices[oldestIndex].triggerTimestamp)

        // Allocate 33rd voice with new timestamp 2000L and new padId 99
        val reusedIndex = pool.allocateVoice(padId = 99, sampleData = dummySample, timestamp = 2000L)

        // Must steal and reuse the oldest voice
        assertEquals("Oldest voice must be reused", oldestIndex, reusedIndex)
        assertEquals("Pool size remains strictly capped at 32", 32, pool.getActiveVoiceCount())
        assertEquals("Voice must now have the new padId", 99, pool.voices[reusedIndex].padId)
        assertEquals("Voice must now have the new timestamp", 2000L, pool.voices[reusedIndex].triggerTimestamp)
    }

    @Test
    fun testVoiceDeactivationAfterRender() {
        val shortSample = FloatArray(128) { 0.2f }
        val idx = pool.allocateVoice(padId = 5, sampleData = shortSample, timestamp = 500L)
        assertEquals(1, pool.getActiveVoiceCount())

        val buffer = FloatArray(256)
        pool.renderMixed(buffer, 256)

        // Voice had 128 frames, rendered 256 frames -> voice finished and deactivated
        assertEquals(0, pool.getActiveVoiceCount())
        assertFalse(pool.voices[idx].active)
    }

    @Test
    fun testVoicePoolReset() {
        for (i in 0 until 10) {
            pool.allocateVoice(padId = i, sampleData = dummySample, timestamp = 10L + i)
        }
        assertEquals(10, pool.getActiveVoiceCount())

        pool.reset()
        assertEquals(0, pool.getActiveVoiceCount())
    }
}
