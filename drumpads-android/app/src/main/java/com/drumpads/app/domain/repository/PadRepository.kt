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
        // DRUMS (0..5) - Red group
        Pad(0, "Kick Punch", PadGroup.DRUM, "drum_kick_punch", "starter/samples/kick_punch.wav", "#E53935"),
        Pad(1, "Kick Deep", PadGroup.DRUM, "drum_kick_deep", "starter/samples/kick_deep.wav", "#E53935"),
        Pad(2, "Snare Ac", PadGroup.DRUM, "drum_snare_acoustic", "starter/samples/snare_acoustic.wav", "#E53935"),
        Pad(3, "Snare El", PadGroup.DRUM, "drum_snare_electro", "starter/samples/snare_electro.wav", "#E53935"),
        Pad(4, "Hat Cl", PadGroup.DRUM, "drum_hat_closed", "starter/samples/hat_closed.wav", "#E53935"),
        Pad(5, "Hat Op", PadGroup.DRUM, "drum_hat_open", "starter/samples/hat_open.wav", "#E53935"),

        // BASS (6..11) - Purple group
        Pad(6, "Sub C", PadGroup.BASS, "bass_sub_c", "starter/samples/bass_sub_c.wav", "#8E24AA"),
        Pad(7, "Sub D", PadGroup.BASS, "bass_sub_d", "starter/samples/bass_sub_d.wav", "#8E24AA"),
        Pad(8, "Pluck E", PadGroup.BASS, "bass_pluck_e", "starter/samples/bass_pluck_e.wav", "#8E24AA"),
        Pad(9, "Pluck G", PadGroup.BASS, "bass_pluck_g", "starter/samples/bass_pluck_g.wav", "#8E24AA"),
        Pad(10, "Synth A", PadGroup.BASS, "bass_synth_a", "starter/samples/bass_synth_a.wav", "#8E24AA"),
        Pad(11, "Synth C", PadGroup.BASS, "bass_synth_c", "starter/samples/bass_synth_c.wav", "#8E24AA"),

        // MELODY (12..17) - Teal group
        Pad(12, "Lead C4", PadGroup.MELODY, "melody_lead_c4", "starter/samples/melody_lead_c4.wav", "#00897B"),
        Pad(13, "Lead D4", PadGroup.MELODY, "melody_lead_d4", "starter/samples/melody_lead_d4.wav", "#00897B"),
        Pad(14, "Lead E4", PadGroup.MELODY, "melody_lead_e4", "starter/samples/melody_lead_e4.wav", "#00897B"),
        Pad(15, "Lead G4", PadGroup.MELODY, "melody_lead_g4", "starter/samples/melody_lead_g4.wav", "#00897B"),
        Pad(16, "Lead A4", PadGroup.MELODY, "melody_lead_a4", "starter/samples/melody_lead_a4.wav", "#00897B"),
        Pad(17, "Pluck C5", PadGroup.MELODY, "melody_pluck_c5", "starter/samples/melody_pluck_c5.wav", "#00897B"),

        // FX (18..23) - Orange group
        Pad(18, "Tom Low", PadGroup.FX, "fx_tom_low", "starter/samples/tom_low.wav", "#FB8C00"),
        Pad(19, "Tom High", PadGroup.FX, "fx_tom_high", "starter/samples/tom_high.wav", "#FB8C00"),
        Pad(20, "Crash", PadGroup.FX, "fx_crash", "starter/samples/fx_crash.wav", "#FB8C00"),
        Pad(21, "Laser", PadGroup.FX, "fx_laser", "starter/samples/fx_laser.wav", "#FB8C00"),
        Pad(22, "Chant Hey", PadGroup.FX, "fx_chant_hey", "starter/samples/vocal_chant_hey.wav", "#FB8C00"),
        Pad(23, "Chop Oh", PadGroup.FX, "fx_chop_oh", "starter/samples/vocal_chop_oh.wav", "#FB8C00")
    )

    override fun getPads(): List<Pad> = pads

    override fun getPad(index: Int): Pad? = pads.getOrNull(index)
}
