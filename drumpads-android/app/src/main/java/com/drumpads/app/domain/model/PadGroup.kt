package com.drumpads.app.domain.model

enum class PadGroup(val displayName: String, val baseColorHex: String) {
    DRUM("Drum", "#E53935"),
    BASS("Bass", "#8E24AA"),
    MELODY("Melody", "#00897B"),
    FX("FX", "#FB8C00")
}
