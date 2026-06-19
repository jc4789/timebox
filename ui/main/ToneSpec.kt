package com.example.timeboxvibe.ui.main

data class ToneSpec(
    val freq: Float,
    val startMs: Int,
    val durationMs: Int,
    val volume: Float,
    val type: String = "sine",
    // ADSR envelope — only used when useADSR = true
    val useADSR: Boolean = false,
    val attackMs: Int = 10,
    val decayMs: Int = 50,
    val sustainLevel: Float = 0.7f,
    val releaseMs: Int = 80
)
