package com.drumpads.app.domain.model

data class FeedComment(
    val id: String,
    val author: String,
    val text: String,
    val timeAgo: String
)

data class TutorialStep(
    val padIndex: Int,
    val label: String,
    val delayMs: Long = 450L
)

data class CommunityPost(
    val id: String,
    val authorName: String,
    val authorInitials: String,
    val avatarStart: Long,
    val avatarEnd: Long,
    val timeAgo: String,
    val caption: String,
    val trackTitle: String,
    val packId: String,
    val genre: MusicGenre,
    val bpm: Int,
    val likesCount: Int,
    val isLiked: Boolean = false,
    val isFollowingAuthor: Boolean = false,
    val isCrewTutorial: Boolean = false,
    val isUserPost: Boolean = false,
    val tutorialSteps: List<TutorialStep> = emptyList(),
    val recordingFilePath: String? = null,
    val comments: List<FeedComment> = emptyList()
)

object CommunityCatalog {
    val initialPosts: List<CommunityPost> = listOf(
        CommunityPost(
            id = "crew_tut_1",
            authorName = "Beat Maker Crew",
            authorInitials = "BM",
            avatarStart = 0xFFFF2A85,
            avatarEnd = 0xFF00DFD8,
            timeAgo = "Official Lesson",
            caption = "🎓 Finger Drumming 101: Learn the classic Trap Bounce pattern! Follow the glowing guide pads to master Kick + Hi-Hat + Snare + 808 Sub timing.",
            trackTitle = "Trap Bounce Lightshow Lesson",
            packId = "hyphy_trap",
            genre = MusicGenre.TRAP,
            bpm = 138,
            likesCount = 1420,
            isFollowingAuthor = true,
            isCrewTutorial = true,
            tutorialSteps = listOf(
                TutorialStep(0, "Kick Punch"),
                TutorialStep(4, "Closed Hat"),
                TutorialStep(2, "Snare Hit"),
                TutorialStep(4, "Closed Hat"),
                TutorialStep(0, "Kick Double"),
                TutorialStep(6, "808 Sub C"),
                TutorialStep(2, "Snare Hit"),
                TutorialStep(5, "Open Hat")
            ),
            comments = listOf(
                FeedComment("c1", "Minh Beat", "Bài tập lightshow này dễ hiểu quá, tập 5 phút đánh mượt luôn!", "2h ago"),
                FeedComment("c2", "Kaito Phonk", "Try turning on Echo 1/16 on the last snare hit!", "1h ago")
            )
        ),
        CommunityPost(
            id = "crew_tut_2",
            authorName = "Beat Maker Crew",
            authorInitials = "BM",
            avatarStart = 0xFF4776E6,
            avatarEnd = 0xFF8E54E9,
            timeAgo = "Official Lesson",
            caption = "🔥 Drift Phonk Cowbell Melody Tutorial! Master the iconic Memphis cowbell rhythm + punchy 808 distortion at 140 BPM.",
            trackTitle = "Tokyo Drift Cowbell Routine",
            packId = "tokyo_drift_phonk",
            genre = MusicGenre.PHONK,
            bpm = 140,
            likesCount = 985,
            isFollowingAuthor = true,
            isCrewTutorial = true,
            tutorialSteps = listOf(
                TutorialStep(0, "Phonk Kick"),
                TutorialStep(6, "Cowbell C"),
                TutorialStep(7, "Cowbell D#"),
                TutorialStep(2, "Memphis Snr"),
                TutorialStep(8, "Cowbell F"),
                TutorialStep(6, "Cowbell C"),
                TutorialStep(9, "Cowbell G"),
                TutorialStep(1, "Dist 808")
            ),
            comments = listOf(
                FeedComment("c3", "DriftKing", "That cowbell tone slaps with Distortion at 40%!", "4h ago")
            )
        ),
        CommunityPost(
            id = "post_classified_1",
            authorName = "Classified Beatz",
            authorInitials = "CB",
            avatarStart = 0xFFE02476,
            avatarEnd = 0xFFFFD233,
            timeAgo = "3 hours ago",
            caption = "New pack! 🔥 This came out pretty clean ngl, smooth recording with Lo-Fi & Reverb FX. BPM is 92. Tap Play to listen or load the pack to jam!",
            trackTitle = "21 savage x baby keem x drake type beat",
            packId = "medium_rare_hiphop",
            genre = MusicGenre.HIP_HOP,
            bpm = 92,
            likesCount = 328,
            comments = listOf(
                FeedComment("c4", "Cahya Pads", "Clean snare pocket bro 🔥", "2h ago"),
                FeedComment("c5", "Mellow Tape", "Love the boom bap bounce!", "1h ago")
            )
        ),
        CommunityPost(
            id = "post_cahya_1",
            authorName = "Cahya Pads",
            authorInitials = "CP",
            avatarStart = 0xFF6A11CB,
            avatarEnd = 0xFF2575FC,
            timeAgo = "1 day ago",
            caption = "HIGHEST IN THE ROOM live finger drumming performance using Apollo 11 Trap kit + 1/16 Echo & Fill button on the drop 🚀",
            trackTitle = "HIGHEST IN THE ROOM (Live Pad Remix)",
            packId = "apollo_11_trap",
            genre = MusicGenre.TRAP,
            bpm = 142,
            likesCount = 874,
            comments = listOf(
                FeedComment("c6", "Aria Beats", "The hi-hat roll in clip 3 is insane!", "12h ago")
            )
        ),
        CommunityPost(
            id = "post_vexor_1",
            authorName = "Vexor Bass",
            authorInitials = "VB",
            avatarStart = 0xFFF857A6,
            avatarEnd = 0xFFFF5858,
            timeAgo = "2 days ago",
            caption = "Testing the Wobble + Distortion FX faders on You Are Dead Dubstep pack! Crank Wobble to 75% during the bassline drop 💀⚡",
            trackTitle = "Heavy Riddim Wobble Session",
            packId = "you_are_dead_dubstep",
            genre = MusicGenre.DUBSTEP,
            bpm = 145,
            likesCount = 612,
            comments = listOf(
                FeedComment("c7", "SubZero DnB", "Wobble LFO synced to 145 BPM goes crazy!", "1d ago")
            )
        ),
        CommunityPost(
            id = "post_mellow_1",
            authorName = "Mellow Tape",
            authorInitials = "MT",
            avatarStart = 0xFFFA709A,
            avatarEnd = 0xFFFEE140,
            timeAgo = "3 days ago",
            caption = "Late night study vibes ☕ Recorded in Loop Mode layering Rhodes chords, dusty kick, and subtle Lo-Fi cassette crackle.",
            trackTitle = "Rainy Window Chillhop Loop",
            packId = "lofi_sunset",
            genre = MusicGenre.LOFI,
            bpm = 82,
            likesCount = 540,
            comments = emptyList()
        )
    )
}
