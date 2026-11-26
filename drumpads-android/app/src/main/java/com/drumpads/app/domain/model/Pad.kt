package com.drumpads.app.domain.model

enum class PadPlayMode(val label: String) {
    ONE_SHOT("ONE SHOT"),
    LOOP("LOOP"),
    HOLD("HOLD")
}

data class Pad(
    val index: Int,
    val name: String,
    val group: PadGroup,
    val sampleId: String,
    val samplePath: String,
    val colorHex: String,
    val volume: Float = 1.0f,
    val pitchSemitones: Float = 0f,
    val pan: Float = 0f,
    val trimStart: Float = 0f,
    val trimEnd: Float = 1f,
    val fadeInRatio: Float = 0f,
    val fadeOutRatio: Float = 0f,
    val playMode: PadPlayMode = PadPlayMode.ONE_SHOT,
    val chokeGroup: Int = 0, // 0 = off, 1 = group 1, 2 = group 2
    val padReverbSend: Float = 0f,
    val padDistortion: Float = 0f,
    val customFilePath: String? = null
)
