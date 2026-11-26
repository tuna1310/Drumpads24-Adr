package com.drumpads.app.domain.model

import kotlin.math.abs

enum class RecordingMode(val label: String) {
    FREE("Free Mode"),
    LOOP("Loop Mode")
}

data class ClipHit(
    val step: Int,      // 0..lengthSteps-1 (16th-note grid)
    val padIndex: Int,  // 0..23
    val velocity: Float = 1.0f
)

data class ClipSlot(
    val index: Int,
    val name: String,
    val lengthSteps: Int = 16,
    val hits: List<ClipHit> = emptyList(),
    val colorHex: String = "#8B5CF6"
) {
    val isEmpty: Boolean get() = hits.isEmpty()
}

object ClipPatternPresets {

    fun buildDefaultClips(genre: MusicGenre, packId: String = ""): List<ClipSlot> {
        val slots = mutableListOf<ClipSlot>()
        val variant = abs(packId.hashCode()) % 6

        // Clip 0: Kick pattern tailored by genre + pack variant
        val kickSteps = when (genre) {
            MusicGenre.HOUSE, MusicGenre.TECHNO, MusicGenre.EDM, MusicGenre.POP ->
                if (variant % 2 == 0) listOf(0, 4, 8, 12) else listOf(0, 4, 8, 12, 14)
            MusicGenre.DNB -> listOf(0, 6, 10)
            MusicGenre.DUBSTEP -> listOf(0, 10, 14)
            MusicGenre.LOFI, MusicGenre.HIP_HOP, MusicGenre.RNB ->
                if (variant % 2 == 0) listOf(0, 7, 10) else listOf(0, 3, 10, 13)
            else -> // Trap / Phonk
                when (variant % 3) {
                    0 -> listOf(0, 6, 10, 14)
                    1 -> listOf(0, 3, 7, 11)
                    else -> listOf(0, 6, 9, 12)
                }
        }
        val kickPad = if (variant % 2 == 1) 1 else 0
        slots.add(ClipSlot(0, "Kick", 16, kickSteps.map { ClipHit(it, kickPad) }, "#38E54D"))

        // Clip 1: Snare / Clap
        val snareSteps = when (genre) {
            MusicGenre.DUBSTEP -> listOf(8)
            MusicGenre.TRAP, MusicGenre.PHONK -> if (variant % 2 == 0) listOf(8) else listOf(4, 12, 15)
            MusicGenre.DNB -> listOf(4, 12)
            else -> if (variant % 2 == 0) listOf(4, 12) else listOf(4, 12, 14)
        }
        val snarePad = if (variant % 2 == 1) 3 else 2
        slots.add(ClipSlot(1, "Snare", 16, snareSteps.map { ClipHit(it, snarePad) }, "#FF7A00"))

        // Clip 2: Hi-Hat groove
        val hatSteps = when (genre) {
            MusicGenre.TRAP, MusicGenre.PHONK ->
                if (variant % 2 == 0) listOf(0, 2, 4, 6, 8, 10, 12, 13, 14, 15)
                else listOf(0, 1, 2, 4, 6, 8, 9, 10, 12, 14)
            MusicGenre.HOUSE, MusicGenre.TECHNO -> listOf(2, 6, 10, 14)
            else -> listOf(0, 2, 4, 6, 8, 10, 12, 14)
        }
        val hatPad = if (genre == MusicGenre.HOUSE || genre == MusicGenre.TECHNO) 5 else 4
        slots.add(ClipSlot(2, "Hi-Hats", 16, hatSteps.map { ClipHit(it, hatPad) }, "#00B0FF"))

        // Clip 3: Bassline unique per pack/variant
        val bassHits = when (variant) {
            0 -> listOf(ClipHit(0, 6), ClipHit(3, 7), ClipHit(6, 8), ClipHit(8, 9), ClipHit(11, 8), ClipHit(14, 7))
            1 -> listOf(ClipHit(0, 7), ClipHit(4, 9), ClipHit(6, 10), ClipHit(10, 8), ClipHit(12, 6), ClipHit(14, 11))
            2 -> listOf(ClipHit(0, 8), ClipHit(2, 6), ClipHit(8, 10), ClipHit(10, 9), ClipHit(12, 7))
            3 -> listOf(ClipHit(0, 6), ClipHit(6, 10), ClipHit(8, 11), ClipHit(12, 8), ClipHit(15, 7))
            4 -> listOf(ClipHit(2, 9), ClipHit(5, 7), ClipHit(8, 6), ClipHit(11, 10), ClipHit(14, 8))
            else -> listOf(ClipHit(0, 10), ClipHit(3, 8), ClipHit(7, 6), ClipHit(10, 11), ClipHit(13, 9))
        }
        slots.add(ClipSlot(3, "Bassline", 16, bassHits, "#E040FB"))

        // Clip 4: Melody Hook — genuinely distinct melodies per pack!
        val melodyHits = when (variant) {
            0 -> listOf(ClipHit(0, 12), ClipHit(3, 14), ClipHit(6, 15), ClipHit(8, 16), ClipHit(12, 17), ClipHit(14, 15))
            1 -> listOf(ClipHit(0, 17), ClipHit(2, 16), ClipHit(4, 14), ClipHit(8, 15), ClipHit(10, 13), ClipHit(12, 12))
            2 -> listOf(ClipHit(0, 13), ClipHit(2, 13), ClipHit(6, 15), ClipHit(8, 17), ClipHit(10, 16), ClipHit(14, 14))
            3 -> listOf(ClipHit(0, 14), ClipHit(4, 12), ClipHit(6, 16), ClipHit(10, 17), ClipHit(12, 14), ClipHit(14, 13))
            4 -> listOf(ClipHit(0, 15), ClipHit(3, 17), ClipHit(6, 14), ClipHit(9, 16), ClipHit(12, 12), ClipHit(14, 15))
            else -> listOf(ClipHit(0, 16), ClipHit(3, 15), ClipHit(5, 13), ClipHit(8, 17), ClipHit(11, 14), ClipHit(14, 12))
        }
        slots.add(ClipSlot(4, "Melody", 16, melodyHits, "#FF2E93"))

        // Clip 5: Percussion & Toms
        val percHits = listOf(ClipHit(3, 18), ClipHit(7, 19), ClipHit(11, 18), ClipHit(15, 19))
        slots.add(ClipSlot(5, "Percs", 16, percHits, "#FFD600"))

        // Clip 6: Vocal Accent
        val voxHits = if (variant % 2 == 0) {
            listOf(ClipHit(4, 22), ClipHit(12, 23))
        } else {
            listOf(ClipHit(2, 23), ClipHit(10, 22), ClipHit(14, 23))
        }
        slots.add(ClipSlot(6, "Vocals", 16, voxHits, "#00F5D4"))

        // Clip 7: FX & Drop
        val fxHits = listOf(ClipHit(0, 20), ClipHit(14, 21))
        slots.add(ClipSlot(7, "FX Drop", 16, fxHits, "#9D00FF"))

        // Clips 8..11: Empty user slots ready for live multi-layer recording or step editing!
        for (i in 8..11) {
            slots.add(ClipSlot(i, "Clip ${i + 1}", 16, emptyList(), "#6C3CE9"))
        }

        return slots
    }
}
