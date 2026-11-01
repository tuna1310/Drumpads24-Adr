package com.drumpads.app.domain.repository

import com.drumpads.app.domain.model.Pad
import com.drumpads.app.domain.model.PadGroup
import javax.inject.Inject
import javax.inject.Singleton

interface PadRepository {
    fun getPads(): List<Pad>
    fun getPad(index: Int): Pad?
}

@Singleton
class DefaultPadRepository @Inject constructor() : PadRepository {

    private val pads: List<Pad> = listOf(
        // SCENE A (0..11) - Signature Drum Pads 24 Layout
        // Row 0: 3 Lime Green pads
        Pad(0, "Kick Punch", PadGroup.DRUM, "drum_kick_punch", "starter/samples/kick_punch.wav", "#38E54D"),
        Pad(1, "Kick Deep", PadGroup.DRUM, "drum_kick_deep", "starter/samples/kick_deep.wav", "#38E54D"),
        Pad(2, "Snare Ac", PadGroup.DRUM, "drum_snare_acoustic", "starter/samples/snare_acoustic.wav", "#38E54D"),
        // Row 1: 1 Lime Green, 1 Vivid Orange, 1 Neon Magenta/Purple
        Pad(3, "Snare El", PadGroup.DRUM, "drum_snare_electro", "starter/samples/snare_electro.wav", "#38E54D"),
        Pad(4, "Hat Cl", PadGroup.DRUM, "drum_hat_closed", "starter/samples/hat_closed.wav", "#FF7A00"),
        Pad(5, "Hat Op", PadGroup.DRUM, "drum_hat_open", "starter/samples/hat_open.wav", "#E040FB"),
        // Row 2: 3 Vivid Orange pads
        Pad(6, "Sub C", PadGroup.BASS, "bass_sub_c", "starter/samples/bass_sub_c.wav", "#FF7A00"),
        Pad(7, "Sub D", PadGroup.BASS, "bass_sub_d", "starter/samples/bass_sub_d.wav", "#FF7A00"),
        Pad(8, "Pluck E", PadGroup.BASS, "bass_pluck_e", "starter/samples/bass_pluck_e.wav", "#FF7A00"),
        // Row 3: 3 Electric Sky Blue / Cyan pads
        Pad(9, "Pluck G", PadGroup.BASS, "bass_pluck_g", "starter/samples/bass_pluck_g.wav", "#00B0FF"),
        Pad(10, "Synth A", PadGroup.BASS, "bass_synth_a", "starter/samples/bass_synth_a.wav", "#00B0FF"),
        Pad(11, "Synth C", PadGroup.BASS, "bass_synth_c", "starter/samples/bass_synth_c.wav", "#00B0FF"),

        // SCENE B (12..23) - Electronic Kit Complementary Neon Colors
        // Row 0: Hot Pink
        Pad(12, "Lead C4", PadGroup.MELODY, "melody_lead_c4", "starter/samples/melody_lead_c4.wav", "#FF2E93"),
        Pad(13, "Lead D4", PadGroup.MELODY, "melody_lead_d4", "starter/samples/melody_lead_d4.wav", "#FF2E93"),
        Pad(14, "Lead E4", PadGroup.MELODY, "melody_lead_e4", "starter/samples/melody_lead_e4.wav", "#FF2E93"),
        // Row 1: Electric Violet
        Pad(15, "Lead G4", PadGroup.MELODY, "melody_lead_g4", "starter/samples/melody_lead_g4.wav", "#9D00FF"),
        Pad(16, "Lead A4", PadGroup.MELODY, "melody_lead_a4", "starter/samples/melody_lead_a4.wav", "#9D00FF"),
        Pad(17, "Pluck C5", PadGroup.MELODY, "melody_pluck_c5", "starter/samples/melody_pluck_c5.wav", "#9D00FF"),
        // Row 2: Neon Amber
        Pad(18, "Tom Low", PadGroup.FX, "fx_tom_low", "starter/samples/tom_low.wav", "#FFD600"),
        Pad(19, "Tom High", PadGroup.FX, "fx_tom_high", "starter/samples/tom_high.wav", "#FFD600"),
        Pad(20, "Crash", PadGroup.FX, "fx_crash", "starter/samples/fx_crash.wav", "#FFD600"),
        // Row 3: Neon Aqua
        Pad(21, "Laser", PadGroup.FX, "fx_laser", "starter/samples/fx_laser.wav", "#00F5D4"),
        Pad(22, "Chant Hey", PadGroup.FX, "fx_chant_hey", "starter/samples/vocal_chant_hey.wav", "#00F5D4"),
        Pad(23, "Chop Oh", PadGroup.FX, "fx_chop_oh", "starter/samples/vocal_chop_oh.wav", "#00F5D4")
    )

    override fun getPads(): List<Pad> = pads

    override fun getPad(index: Int): Pad? = pads.getOrNull(index)
}
