package com.drumpads.app.domain.model

enum class MusicGenre(val label: String, val defaultBpm: Int, val accentColor: Long) {
    ALL("All", 120, 0xFF00DFD8),
    EDM("EDM", 128, 0xFF00C9FF),
    TRAP("Trap", 138, 0xFFE94057),
    HIP_HOP("Hip-Hop", 92, 0xFFF5AF19),
    HOUSE("House", 124, 0xFFFF9966),
    DUBSTEP("Dubstep", 145, 0xFFF857A6),
    PHONK("Phonk", 140, 0xFF9D00FF),
    LOFI("Lo-Fi", 82, 0xFF38EF7D),
    SYNTHWAVE("Synthwave", 118, 0xFFFF2A85),
    DNB("Drum & Bass", 174, 0xFF06D6A0),
    TECHNO("Techno", 132, 0xFF2193B0),
    RNB("R&B", 98, 0xFFE040FB),
    POP("Pop", 116, 0xFFFFD600);

    companion object {
        val playableGenres: List<MusicGenre> = entries.filter { it != ALL }
    }
}

data class SoundPack(
    val id: String,
    val title: String,
    val subtitle: String,
    val artist: String,
    val genre: MusicGenre,
    val bpm: Int,
    val gradientStart: Long,
    val gradientEnd: Long,
    val isCustom: Boolean = false,
    val isFeaturedStory: Boolean = false,
    val isNewRelease: Boolean = false,
    val isDownloaded: Boolean = true,
    val downloadsLabel: String = "50K+",
    val pads: List<Pad> = emptyList()
)

object SoundPackCatalog {

    val allPacks: List<SoundPack> = listOf(
        SoundPack(
            id = "hyphy_trap",
            title = "Hyphy",
            subtitle = "West Coast 808s & Slap",
            artist = "Drum Pads 24 Crew",
            genre = MusicGenre.TRAP,
            bpm = 138,
            gradientStart = 0xFFE94057,
            gradientEnd = 0xFFF27121,
            isNewRelease = true,
            downloadsLabel = "420K+"
        ),
        SoundPack(
            id = "neon_pulse_edm",
            title = "Neon Pulse 24",
            subtitle = "Mainstage Festival EDM",
            artist = "Drum Pads 24 Official",
            genre = MusicGenre.EDM,
            bpm = 128,
            gradientStart = 0xFFFF2A85,
            gradientEnd = 0xFF00DFD8,
            isNewRelease = true,
            downloadsLabel = "890K+"
        ),
        SoundPack(
            id = "tokyo_drift_phonk",
            title = "Tokyo Drift 808",
            subtitle = "Memphis Cowbell & Drift Bass",
            artist = "Kaito Phonk",
            genre = MusicGenre.PHONK,
            bpm = 140,
            gradientStart = 0xFF4776E6,
            gradientEnd = 0xFF8E54E9,
            isFeaturedStory = true,
            isNewRelease = true,
            downloadsLabel = "310K+"
        ),
        SoundPack(
            id = "you_are_dead_dubstep",
            title = "You Are Dead",
            subtitle = "Heavy Riddim & Wobble Drops",
            artist = "Vexor Bass",
            genre = MusicGenre.DUBSTEP,
            bpm = 145,
            gradientStart = 0xFFF857A6,
            gradientEnd = 0xFFFF5858,
            isNewRelease = true,
            downloadsLabel = "285K+"
        ),
        SoundPack(
            id = "lofi_sunset",
            title = "Lo-Fi Sunset",
            subtitle = "Warm Rhodes & Vinyl Dust",
            artist = "Mellow Tape",
            genre = MusicGenre.LOFI,
            bpm = 82,
            gradientStart = 0xFFFA709A,
            gradientEnd = 0xFFFEE140,
            isNewRelease = true,
            downloadsLabel = "540K+"
        ),
        SoundPack(
            id = "kilimanjaro_house",
            title = "Kilimanjaro",
            subtitle = "Afro House & Organic Percs",
            artist = "Zuri Groove",
            genre = MusicGenre.HOUSE,
            bpm = 124,
            gradientStart = 0xFFFF9966,
            gradientEnd = 0xFFFF5E62,
            isFeaturedStory = true,
            downloadsLabel = "190K+"
        ),
        SoundPack(
            id = "rain_dance_techno",
            title = "Rain Dance",
            subtitle = "Berlin Warehouse Acid 303",
            artist = "Klangwerk",
            genre = MusicGenre.TECHNO,
            bpm = 132,
            gradientStart = 0xFF2193B0,
            gradientEnd = 0xFF6DD5ED,
            isFeaturedStory = true,
            downloadsLabel = "165K+"
        ),
        SoundPack(
            id = "equilibrium_dnb",
            title = "Equilibrium",
            subtitle = "Neurofunk & Liquid Breakbeat",
            artist = "SubZero DnB",
            genre = MusicGenre.DNB,
            bpm = 174,
            gradientStart = 0xFF8A2387,
            gradientEnd = 0xFFE94057,
            isFeaturedStory = true,
            downloadsLabel = "230K+"
        ),
        SoundPack(
            id = "medium_rare_hiphop",
            title = "Medium Rare",
            subtitle = "90s Boom Bap & Brass Chops",
            artist = "Classified Beatz",
            genre = MusicGenre.HIP_HOP,
            bpm = 92,
            gradientStart = 0xFFF12711,
            gradientEnd = 0xFFF5AF19,
            isFeaturedStory = true,
            downloadsLabel = "370K+"
        ),
        SoundPack(
            id = "heavenly_rnb",
            title = "Heavenly",
            subtitle = "Midnight Trap Soul & Keys",
            artist = "Nova Velvet",
            genre = MusicGenre.RNB,
            bpm = 98,
            gradientStart = 0xFF00C9FF,
            gradientEnd = 0xFF92FE9D,
            isFeaturedStory = true,
            downloadsLabel = "215K+"
        ),
        SoundPack(
            id = "retro_city_synthwave",
            title = "Retro City 1984",
            subtitle = "Outrun Analog & Gated Snare",
            artist = "Neon Drive",
            genre = MusicGenre.SYNTHWAVE,
            bpm = 118,
            gradientStart = 0xFF5F2C82,
            gradientEnd = 0xFF49A09D,
            isNewRelease = true,
            downloadsLabel = "260K+"
        ),
        SoundPack(
            id = "starlight_pop",
            title = "Starlight Pop",
            subtitle = "Chart-Ready Plucks & Snaps",
            artist = "Aria Beats",
            genre = MusicGenre.POP,
            bpm = 116,
            gradientStart = 0xFFFFD600,
            gradientEnd = 0xFFFF416C,
            isNewRelease = true,
            downloadsLabel = "340K+"
        ),
        SoundPack(
            id = "bleed_dubstep",
            title = "Bleed",
            subtitle = "Tearout Bass & Heavy Growls",
            artist = "Drum Pads 24 Crew",
            genre = MusicGenre.DUBSTEP,
            bpm = 150,
            gradientStart = 0xFFFF0844,
            gradientEnd = 0xFFFFB199,
            isNewRelease = true,
            downloadsLabel = "175K+"
        ),
        SoundPack(
            id = "zombie_land_edm",
            title = "Zombie Land",
            subtitle = "Hardstyle & Electro Stabs",
            artist = "Drum Pads 24 Crew",
            genre = MusicGenre.EDM,
            bpm = 148,
            gradientStart = 0xFF11998E,
            gradientEnd = 0xFF38EF7D,
            downloadsLabel = "198K+"
        ),
        SoundPack(
            id = "apollo_11_trap",
            title = "Apollo 11",
            subtitle = "Space Trap & Rolling Hi-Hats",
            artist = "Cahya Pads",
            genre = MusicGenre.TRAP,
            bpm = 142,
            gradientStart = 0xFF6A11CB,
            gradientEnd = 0xFF2575FC,
            downloadsLabel = "480K+"
        ),
        SoundPack(
            id = "gasolina_house",
            title = "Gasolina",
            subtitle = "Latin Tech House & Bassline",
            artist = "Rico Fuego",
            genre = MusicGenre.HOUSE,
            bpm = 126,
            gradientStart = 0xFFFC4A1A,
            gradientEnd = 0xFFF7B733,
            downloadsLabel = "290K+"
        ),
        SoundPack(
            id = "dark_matter_techno",
            title = "Dark Matter",
            subtitle = "Hypnotic Peak-Time Techno",
            artist = "Rave Unit",
            genre = MusicGenre.TECHNO,
            bpm = 136,
            gradientStart = 0xFF30CFD0,
            gradientEnd = 0xFF330867,
            downloadsLabel = "155K+"
        ),
        SoundPack(
            id = "midnight_chill_lofi",
            title = "Midnight Study",
            subtitle = "Chillhop & Rain Texture",
            artist = "Mellow Tape",
            genre = MusicGenre.LOFI,
            bpm = 78,
            gradientStart = 0xFF3A1C71,
            gradientEnd = 0xFFD76D77,
            downloadsLabel = "310K+"
        )
    )

    fun getPackById(id: String): SoundPack =
        allPacks.firstOrNull { it.id == id } ?: allPacks.first()

    /**
     * Generates a genre-tailored 24-pad metadata configuration (names, colors, choke groups).
     */
    fun buildPadsForGenre(genre: MusicGenre): List<Pad> {
        val sceneAColors = listOf(
            "#38E54D", "#38E54D", "#38E54D",
            "#38E54D", "#FF7A00", "#E040FB",
            "#FF7A00", "#FF7A00", "#FF7A00",
            "#00B0FF", "#00B0FF", "#00B0FF"
        )
        val sceneBColors = listOf(
            "#FF2E93", "#FF2E93", "#FF2E93",
            "#9D00FF", "#9D00FF", "#9D00FF",
            "#FFD600", "#FFD600", "#FFD600",
            "#00F5D4", "#00F5D4", "#00F5D4"
        )
        val names: List<String> = when (genre) {
            MusicGenre.PHONK -> listOf(
                "Phonk Kick", "Dist 808", "Memphis Snr", "Tape Clap", "Tick Hat", "Open Hat",
                "Cowbell C", "Cowbell D#", "Cowbell F", "Cowbell G", "Cowbell A#", "Dark Sub",
                "Drift Lead1", "Drift Lead2", "Drift Lead3", "Drift Lead4", "Phonk Bell", "Siren Hit",
                "808 Tom Lo", "808 Tom Hi", "Crash", "Tape Stop", "Vox Mane", "Vox Yeah"
            )
            MusicGenre.DUBSTEP -> listOf(
                "Stomp Kick", "Sub Thump", "Heavy Snare", "Metal Clap", "Ride Hat", "Open Hat",
                "Wobble C", "Wobble D#", "Growl F", "Growl G", "Yoi Bass", "Sub Drop",
                "Riddim Stb1", "Riddim Stb2", "Laser Synth", "Screech", "Arp High", "Pluck C5",
                "Impact Lo", "Impact Hi", "White Crash", "Laser Zp", "Vox Hey", "Vox Drop"
            )
            MusicGenre.LOFI -> listOf(
                "Dusty Kick", "Soft Kick", "Tape Snare", "Rim Shot", "Shaker Hat", "Open Hat",
                "Warm Sub C", "Warm Sub D", "Upright E", "Upright G", "Mellow A", "Mellow C",
                "Rhodes Cmaj", "Rhodes Dm7", "Rhodes Em7", "Rhodes G9", "Rhodes Am7", "Bell Pluck",
                "Perc Wood", "Vinyl Crkl", "Soft Crash", "Tape Sweep", "Vox Ahh", "Vox Ohh"
            )
            MusicGenre.TRAP -> listOf(
                "808 Punch", "Boom 808", "Trap Snare", "Crisp Clap", "Roll Hat", "Open Hat",
                "808 Slide C", "808 Slide D", "808 Pluck E", "808 Pluck G", "Dark Bass A", "Dark Bass C",
                "Dark Bell C", "Dark Bell D", "Flute E", "Flute G", "Flute A", "Arp Bell",
                "808 Tom Lo", "808 Tom Hi", "Cymbal", "Riser Fx", "Chant Hey", "Chant Ay"
            )
            MusicGenre.HOUSE -> listOf(
                "909 Kick", "Deep Kick", "909 Snare", "House Clap", "Closed Hat", "Offbeat Hat",
                "Organ Sub C", "Organ Bass D", "FM Pluck E", "FM Pluck G", "Bass Stab A", "Bass Stab C",
                "House Pno C", "House Pno D", "House Pno E", "House Pno G", "House Pno A", "Disco Plk",
                "Conga Lo", "Conga Hi", "909 Crash", "Sweep Up", "Vox House", "Vox Oh"
            )
            MusicGenre.SYNTHWAVE -> listOf(
                "Linn Kick", "Punch Kick", "Gated Snare", "80s Clap", "Crisp Hat", "Open Hat",
                "Analog C", "Analog D", "Arp Bass E", "Arp Bass G", "Moog A", "Moog C",
                "Poly Saw C", "Poly Saw D", "Poly Saw E", "Poly Saw G", "Poly Saw A", "Retro Bell",
                "Simmons Lo", "Simmons Hi", "Retro Crash", "Laser 84", "Vox Night", "Vox Drive"
            )
            MusicGenre.DNB -> listOf(
                "DnB Kick", "Punch Sub", "Amen Snare", "Ghost Snare", "Break Hat", "Ride Cym",
                "Reese C", "Reese D", "Reese E", "Reese G", "Neuro A", "Neuro C",
                "Liquid Pad1", "Liquid Pad2", "Liquid Pad3", "Atmos G", "Atmos A", "Bell C5",
                "Break Tom1", "Break Tom2", "Splash", "Laser Fx", "Vox Hey", "Vox Chop"
            )
            MusicGenre.TECHNO -> listOf(
                "Rumble Kick", "909 Hard", "Noise Snare", "Indus Clap", "909 Hat Cl", "909 Hat Op",
                "Acid 303 C", "Acid 303 D", "Acid 303 E", "Acid 303 G", "Sub Pulse A", "Sub Pulse C",
                "Chord Stab1", "Chord Stab2", "Chord Stab3", "Rave Lead1", "Rave Lead2", "Arp Hit",
                "Metal Hit1", "Metal Hit2", "Noise Crash", "Siren Fx", "Vox Dark", "Vox Pulse"
            )
            MusicGenre.HIP_HOP -> listOf(
                "Boom Kick", "Vinyl Kick", "Boom Snare", "Crispy Clp", "Swing Hat", "Open Hat",
                "Jazz Bass C", "Jazz Bass D", "Funk Plk E", "Funk Plk G", "Moog Sub A", "Moog Sub C",
                "Brass Hit1", "Brass Hit2", "Keys Chop1", "Keys Chop2", "Soul Lead", "Guitar Plk",
                "Bongo Lo", "Bongo Hi", "Crash", "Scratch Fx", "Vox Yo", "Vox Uh"
            )
            MusicGenre.RNB -> listOf(
                "Silky Kick", "Sub Kick", "Snap Rim", "Soft Clap", "Tight Hat", "Open Hat",
                "Smooth C", "Smooth D", "Sine Bass E", "Sine Bass G", "Warm Sub A", "Warm Sub C",
                "EPiano C", "EPiano D", "EPiano E", "EPiano G", "EPiano A", "Harp Plk",
                "Perc Lo", "Perc Hi", "Chime Crash", "Reverse Fx", "Vox Ooh", "Vox Ahh"
            )
            MusicGenre.POP -> listOf(
                "Pop Kick", "Club Kick", "Pop Snare", "Bright Clap", "Clean Hat", "Open Hat",
                "Sub Pop C", "Sub Pop D", "Funk Bass E", "Funk Bass G", "Synth Bs A", "Synth Bs C",
                "Pluck C4", "Pluck D4", "Pluck E4", "Pluck G4", "Pluck A4", "Sparkle C5",
                "Tom Lo", "Tom Hi", "Bright Csh", "Riser Pop", "Vox Hey", "Vox Oh"
            )
            else -> listOf(
                "Kick Punch", "Kick Deep", "Snare Ac", "Snare El", "Hat Cl", "Hat Op",
                "Sub C", "Sub D", "Pluck E", "Pluck G", "Synth A", "Synth C",
                "Lead C4", "Lead D4", "Lead E4", "Lead G4", "Lead A4", "Pluck C5",
                "Tom Low", "Tom High", "Crash", "Laser", "Chant Hey", "Chop Oh"
            )
        }

        val starterFiles = listOf(
            "starter/samples/kick_punch.wav", "starter/samples/kick_deep.wav",
            "starter/samples/snare_acoustic.wav", "starter/samples/snare_electro.wav",
            "starter/samples/hat_closed.wav", "starter/samples/hat_open.wav",
            "starter/samples/bass_sub_c.wav", "starter/samples/bass_sub_d.wav",
            "starter/samples/bass_pluck_e.wav", "starter/samples/bass_pluck_g.wav",
            "starter/samples/bass_synth_a.wav", "starter/samples/bass_synth_c.wav",
            "starter/samples/melody_lead_c4.wav", "starter/samples/melody_lead_d4.wav",
            "starter/samples/melody_lead_e4.wav", "starter/samples/melody_lead_g4.wav",
            "starter/samples/melody_lead_a4.wav", "starter/samples/melody_pluck_c5.wav",
            "starter/samples/tom_low.wav", "starter/samples/tom_high.wav",
            "starter/samples/fx_crash.wav", "starter/samples/fx_laser.wav",
            "starter/samples/vocal_chant_hey.wav", "starter/samples/vocal_chop_oh.wav"
        )

        return (0 until 24).map { idx ->
            val group = when {
                idx < 6 -> PadGroup.DRUM
                idx < 12 -> PadGroup.BASS
                idx < 18 -> PadGroup.MELODY
                else -> PadGroup.FX
            }
            val colorHex = if (idx < 12) sceneAColors[idx] else sceneBColors[idx - 12]
            // Assign natural choke group 1 to closed/open hi-hat (4 & 5)
            val chokeGroup = if (idx == 4 || idx == 5) 1 else 0
            Pad(
                index = idx,
                name = names[idx],
                group = group,
                sampleId = "${genre.name.lowercase()}_pad_$idx",
                samplePath = starterFiles[idx],
                colorHex = colorHex,
                chokeGroup = chokeGroup
            )
        }
    }
}
