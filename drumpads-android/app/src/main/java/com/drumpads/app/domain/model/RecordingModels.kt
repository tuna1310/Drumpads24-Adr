package com.drumpads.app.domain.model

data class RecordedPadEvent(
    val timeMs: Long,
    val padIndex: Int
)

data class RecordingItem(
    val id: String,
    val title: String,
    val packName: String,
    val packId: String,
    val bpm: Int,
    val durationMs: Long,
    val durationFormatted: String,
    val timestamp: Long,
    val filePath: String,
    val mode: RecordingMode = RecordingMode.FREE,
    val padEvents: List<RecordedPadEvent> = emptyList()
)
