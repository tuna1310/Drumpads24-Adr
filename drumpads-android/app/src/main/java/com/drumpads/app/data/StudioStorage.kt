package com.drumpads.app.data

import android.content.Context
import com.drumpads.app.domain.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Local persistent storage (SharedPreferences + JSON + filesDir WAV assets)
 * for Recordings, Custom Sound Packs / Projects, Favorites, Recently Used,
 * Community Posts/Comments/Likes/Follows, User Profile, and Settings.
 */
class StudioStorage(private val context: Context) {

    private val prefs = context.getSharedPreferences("drumpads24_studio_v2", Context.MODE_PRIVATE)

    // --- RECORDINGS ---
    fun saveRecordings(list: List<RecordingItem>) {
        val arr = JSONArray()
        for (r in list) {
            val obj = JSONObject().apply {
                put("id", r.id)
                put("title", r.title)
                put("packName", r.packName)
                put("packId", r.packId)
                put("bpm", r.bpm)
                put("durationMs", r.durationMs)
                put("durationFormatted", r.durationFormatted)
                put("timestamp", r.timestamp)
                put("filePath", r.filePath)
                put("mode", r.mode.name)
                val evArr = JSONArray()
                for (ev in r.padEvents) {
                    evArr.put(JSONObject().put("t", ev.timeMs).put("p", ev.padIndex))
                }
                put("events", evArr)
            }
            arr.put(obj)
        }
        prefs.edit().putString("recordings", arr.toString()).apply()
    }

    fun loadRecordings(): List<RecordingItem> {
        val raw = prefs.getString("recordings", null) ?: return emptyList()
        val result = mutableListOf<RecordingItem>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val path = o.getString("filePath")
                if (!File(path).exists()) continue
                val events = mutableListOf<RecordedPadEvent>()
                val evArr = o.optJSONArray("events")
                if (evArr != null) {
                    for (j in 0 until evArr.length()) {
                        val e = evArr.getJSONObject(j)
                        events.add(RecordedPadEvent(e.getLong("t"), e.getInt("p")))
                    }
                }
                result.add(
                    RecordingItem(
                        id = o.getString("id"),
                        title = o.getString("title"),
                        packName = o.optString("packName", "Drum Pads 24"),
                        packId = o.optString("packId", "neon_pulse_edm"),
                        bpm = o.optInt("bpm", 120),
                        durationMs = o.optLong("durationMs", 1000L),
                        durationFormatted = o.optString("durationFormatted", "00:01"),
                        timestamp = o.optLong("timestamp", System.currentTimeMillis()),
                        filePath = path,
                        mode = runCatching { RecordingMode.valueOf(o.optString("mode", "FREE")) }.getOrDefault(RecordingMode.FREE),
                        padEvents = events
                    )
                )
            }
        } catch (_: Throwable) {}
        return result
    }

    // --- CUSTOM SOUND PACKS / PROJECTS ---
    fun saveCustomPacks(packs: List<SoundPack>) {
        val arr = JSONArray()
        for (p in packs) {
            val obj = JSONObject().apply {
                put("id", p.id)
                put("title", p.title)
                put("subtitle", p.subtitle)
                put("artist", p.artist)
                put("genre", p.genre.name)
                put("bpm", p.bpm)
                put("gradientStart", p.gradientStart)
                put("gradientEnd", p.gradientEnd)
                val padsArr = JSONArray()
                for (pad in p.pads) {
                    padsArr.put(JSONObject().apply {
                        put("index", pad.index)
                        put("name", pad.name)
                        put("group", pad.group.name)
                        put("sampleId", pad.sampleId)
                        put("samplePath", pad.samplePath)
                        put("colorHex", pad.colorHex)
                        put("volume", pad.volume.toDouble())
                        put("pitch", pad.pitchSemitones.toDouble())
                        put("pan", pad.pan.toDouble())
                        put("trimStart", pad.trimStart.toDouble())
                        put("trimEnd", pad.trimEnd.toDouble())
                        put("fadeIn", pad.fadeInRatio.toDouble())
                        put("fadeOut", pad.fadeOutRatio.toDouble())
                        put("playMode", pad.playMode.name)
                        put("chokeGroup", pad.chokeGroup)
                        put("reverb", pad.padReverbSend.toDouble())
                        put("dist", pad.padDistortion.toDouble())
                        if (pad.customFilePath != null) put("customFilePath", pad.customFilePath)
                    })
                }
                put("pads", padsArr)
            }
            arr.put(obj)
        }
        prefs.edit().putString("custom_packs", arr.toString()).apply()
    }

    fun loadCustomPacks(): List<SoundPack> {
        val raw = prefs.getString("custom_packs", null) ?: return emptyList()
        val list = mutableListOf<SoundPack>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val genre = runCatching { MusicGenre.valueOf(o.optString("genre", "EDM")) }.getOrDefault(MusicGenre.EDM)
                val pads = mutableListOf<Pad>()
                val padsArr = o.optJSONArray("pads")
                if (padsArr != null) {
                    for (j in 0 until padsArr.length()) {
                        val po = padsArr.getJSONObject(j)
                        pads.add(
                            Pad(
                                index = po.getInt("index"),
                                name = po.getString("name"),
                                group = runCatching { PadGroup.valueOf(po.getString("group")) }.getOrDefault(PadGroup.DRUM),
                                sampleId = po.getString("sampleId"),
                                samplePath = po.getString("samplePath"),
                                colorHex = po.getString("colorHex"),
                                volume = po.optDouble("volume", 1.0).toFloat(),
                                pitchSemitones = po.optDouble("pitch", 0.0).toFloat(),
                                pan = po.optDouble("pan", 0.0).toFloat(),
                                trimStart = po.optDouble("trimStart", 0.0).toFloat(),
                                trimEnd = po.optDouble("trimEnd", 1.0).toFloat(),
                                fadeInRatio = po.optDouble("fadeIn", 0.0).toFloat(),
                                fadeOutRatio = po.optDouble("fadeOut", 0.0).toFloat(),
                                playMode = runCatching { PadPlayMode.valueOf(po.optString("playMode", "ONE_SHOT")) }.getOrDefault(PadPlayMode.ONE_SHOT),
                                chokeGroup = po.optInt("chokeGroup", 0),
                                padReverbSend = po.optDouble("reverb", 0.0).toFloat(),
                                padDistortion = po.optDouble("dist", 0.0).toFloat(),
                                customFilePath = po.optString("customFilePath", "").ifBlank { null }
                            )
                        )
                    }
                }
                list.add(
                    SoundPack(
                        id = o.getString("id"),
                        title = o.getString("title"),
                        subtitle = o.optString("subtitle", "Custom Pack"),
                        artist = o.optString("artist", "You"),
                        genre = genre,
                        bpm = o.optInt("bpm", 120),
                        gradientStart = o.optLong("gradientStart", 0xFFFF2A85),
                        gradientEnd = o.optLong("gradientEnd", 0xFF00DFD8),
                        isCustom = true,
                        isDownloaded = true,
                        downloadsLabel = "Custom",
                        pads = pads.ifEmpty { SoundPackCatalog.buildPadsForGenre(genre) }
                    )
                )
            }
        } catch (_: Throwable) {}
        return list
    }

    // --- FAVORITES & RECENTLY USED ---
    fun saveStringSet(key: String, values: Set<String>) {
        prefs.edit().putStringSet(key, values).apply()
    }

    fun loadStringSet(key: String, default: Set<String> = emptySet()): Set<String> {
        return prefs.getStringSet(key, default)?.toSet() ?: default
    }

    fun saveRecentPackIds(ids: List<String>) {
        prefs.edit().putString("recent_packs", ids.joinToString(",")).apply()
    }

    fun loadRecentPackIds(): List<String> {
        val raw = prefs.getString("recent_packs", null)
        return if (raw.isNullOrBlank()) {
            listOf("hyphy_trap", "neon_pulse_edm", "tokyo_drift_phonk", "lofi_sunset")
        } else {
            raw.split(",").filter { it.isNotBlank() }
        }
    }

    // --- PROFILE ---
    fun saveProfile(name: String, bio: String, link: String) {
        prefs.edit()
            .putString("user_name", name)
            .putString("user_bio", bio)
            .putString("user_link", link)
            .apply()
    }

    fun loadUserName(): String = prefs.getString("user_name", "Tuấn Phạm Anh") ?: "Tuấn Phạm Anh"
    fun loadUserBio(): String = prefs.getString("user_bio", "Beat maker & Finger drummer") ?: "Beat maker & Finger drummer"
    fun loadUserLink(): String = prefs.getString("user_link", "https://instagram.com/tuanphamanh") ?: ""

    // --- USER FEED POSTS ---
    fun saveUserPosts(posts: List<CommunityPost>) {
        val arr = JSONArray()
        for (p in posts.filter { it.isUserPost }) {
            arr.put(JSONObject().apply {
                put("id", p.id)
                put("authorName", p.authorName)
                put("caption", p.caption)
                put("trackTitle", p.trackTitle)
                put("packId", p.packId)
                put("genre", p.genre.name)
                put("bpm", p.bpm)
                put("likesCount", p.likesCount)
                if (p.recordingFilePath != null) put("recordingFilePath", p.recordingFilePath)
            })
        }
        prefs.edit().putString("user_posts", arr.toString()).apply()
    }

    fun loadUserPosts(): List<CommunityPost> {
        val raw = prefs.getString("user_posts", null) ?: return emptyList()
        val list = mutableListOf<CommunityPost>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val name = o.optString("authorName", loadUserName())
                val initials = name.split(" ").mapNotNull { it.firstOrNull()?.uppercaseChar() }.take(2).joinToString("").ifBlank { "ME" }
                list.add(
                    CommunityPost(
                        id = o.getString("id"),
                        authorName = name,
                        authorInitials = initials,
                        avatarStart = 0xFFFFD600,
                        avatarEnd = 0xFFFF2A85,
                        timeAgo = "Just now",
                        caption = o.getString("caption"),
                        trackTitle = o.getString("trackTitle"),
                        packId = o.optString("packId", "neon_pulse_edm"),
                        genre = runCatching { MusicGenre.valueOf(o.optString("genre", "EDM")) }.getOrDefault(MusicGenre.EDM),
                        bpm = o.optInt("bpm", 120),
                        likesCount = o.optInt("likesCount", 1),
                        isUserPost = true,
                        recordingFilePath = o.optString("recordingFilePath", "").ifBlank { null }
                    )
                )
            }
        } catch (_: Throwable) {}
        return list
    }

    fun getCustomSamplesDir(): File = File(context.filesDir, "CustomPacks").apply { mkdirs() }
}
