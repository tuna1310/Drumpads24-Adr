package com.drumpads.app

import com.drumpads.app.domain.model.PadGroup
import com.drumpads.app.domain.repository.DefaultPadRepository
import org.junit.Assert.*
import org.junit.Test

class PadMappingTest {

    private val repository = DefaultPadRepository()

    @Test
    fun testExactly24PadsConfigured() {
        val pads = repository.getPads()
        assertEquals("Must configure exactly 24 pads in 6x4 layout", 24, pads.size)
    }

    @Test
    fun testPadIndicesAreSequentialZeroTo23() {
        val pads = repository.getPads()
        for (i in 0 until 24) {
            assertEquals("Pad index must match position", i, pads[i].index)
        }
    }

    @Test
    fun testFourPadGroupsEquallyDistributed() {
        val pads = repository.getPads()
        val drums = pads.filter { it.group == PadGroup.DRUM }
        val basses = pads.filter { it.group == PadGroup.BASS }
        val melodies = pads.filter { it.group == PadGroup.MELODY }
        val fxs = pads.filter { it.group == PadGroup.FX }

        assertEquals("Must have 6 Drum pads", 6, drums.size)
        assertEquals("Must have 6 Bass pads", 6, basses.size)
        assertEquals("Must have 6 Melody pads", 6, melodies.size)
        assertEquals("Must have 6 FX pads", 6, fxs.size)
    }

    @Test
    fun testSampleIdsAreUniqueAndNonEmpty() {
        val pads = repository.getPads()
        val sampleIds = pads.map { it.sampleId }
        val uniqueIds = sampleIds.toSet()

        assertEquals("All 24 sample IDs must be unique", 24, uniqueIds.size)
        for (pad in pads) {
            assertTrue("Sample ID must not be blank", pad.sampleId.isNotBlank())
            assertTrue("Sample Path must end with .wav", pad.samplePath.endsWith(".wav"))
        }
    }

    @Test
    fun testGetPadByIndex() {
        val pad0 = repository.getPad(0)
        assertNotNull(pad0)
        assertEquals("Kick Punch", pad0?.name)
        assertEquals(PadGroup.DRUM, pad0?.group)

        val invalid = repository.getPad(99)
        assertNull(invalid)
    }
}
