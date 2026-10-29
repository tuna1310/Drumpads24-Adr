package com.drumpads.app.domain.model

data class Pad(
    val index: Int,
    val name: String,
    val group: PadGroup,
    val sampleId: String,
    val samplePath: String,
    val colorHex: String
)
