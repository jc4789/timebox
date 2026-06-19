package com.example.timeboxvibe.ui.main

import kotlinx.serialization.Serializable

/**
 * TimerPreset — Full data model matching the original React i18n.js presets.
 * Supports all 5 engine modes: classic, dual, dual.5, sequence, dual-sequence.
 */
@Serializable
data class TimerPreset(
    val id: String,
    val name: String,
    val mode: String,                    // "classic" | "dual" | "dual.5" | "sequence" | "dual-sequence" | "calendar"
    val sequence: List<Int> = emptyList(), // Array of seconds for sequence modes
    val dualBigDuration: Int = 3600,     // Macro timer total seconds
    val dualMidDuration: Int = 900,      // Medium timer loop seconds (Dual.5 only)
    val dualSmallDuration: Int = 90,     // Micro timer loop seconds
    val alarmBehavior: String = "alarm", // "alarm" (requires dismissal) | "auto" (auto-progresses)
    val description: String = "",
    val sequenceTypes: List<String> = emptyList(), // "focus" | "relax"
    val sequenceLabels: List<String> = emptyList() // custom text labels
)

/**
 * TimerEngine — Pure Kotlin port of useTimer.js.
 * Manages all 5 timer modes with multi-tier countdown logic.
 *
 * Call tick() every second. The engine updates its internal state
 * and returns events (interval complete, sequence complete, ringing).
 */
class TimerEngine(
    var preset: TimerPreset
) {
    // Primary (small/action) timer
    var timeRemaining: Int = 0; private set
    var totalDuration: Int = 0; private set

    // Medium timer (Dual.5 only)
    var midTimeRemaining: Int = 0; private set
    var midTotalDuration: Int = 0; private set

    // Big (macro) timer
    var bigTimeRemaining: Int = 0; private set
    var bigTotalDuration: Int = 0; private set

    // Sequence tracking
    var currentIndex: Int = 0; private set
    val sequenceLength: Int get() = preset.sequence.size

    // State
    var isActive: Boolean = false; private set
    var isRinging: Boolean = false; private set

    // Mode shortcut
    val mode: String get() = preset.mode
    val isDual: Boolean get() = mode == "dual" || mode == "dual-sequence" || mode == "dual.5"
    val isBreak: Boolean get() = (mode == "calendar" && currentIndex < preset.sequenceTypes.size && preset.sequenceTypes[currentIndex] == "relax")

    init {
        initialize()
    }

    /** Reset everything to the preset's initial state. */
    fun initialize() {
        isActive = false
        isRinging = false
        currentIndex = 0

        when (mode) {
            "classic" -> {
                val dur = preset.sequence.firstOrNull() ?: 1500
                timeRemaining = dur; totalDuration = dur
                midTimeRemaining = 0; midTotalDuration = 0
                bigTimeRemaining = 0; bigTotalDuration = 0
            }
            "dual" -> {
                timeRemaining = preset.dualSmallDuration; totalDuration = preset.dualSmallDuration
                midTimeRemaining = 0; midTotalDuration = 0
                bigTimeRemaining = preset.dualBigDuration; bigTotalDuration = preset.dualBigDuration
            }
            "dual.5" -> {
                timeRemaining = preset.dualSmallDuration; totalDuration = preset.dualSmallDuration
                midTimeRemaining = preset.dualMidDuration; midTotalDuration = preset.dualMidDuration
                bigTimeRemaining = preset.dualBigDuration; bigTotalDuration = preset.dualBigDuration
            }
            "sequence" -> {
                val first = preset.sequence.firstOrNull() ?: 60
                timeRemaining = first; totalDuration = first
                midTimeRemaining = 0; midTotalDuration = 0
                bigTimeRemaining = 0; bigTotalDuration = 0
            }
            "calendar" -> {
                val first = preset.sequence.firstOrNull() ?: 600
                timeRemaining = first; totalDuration = first
                midTimeRemaining = 0; midTotalDuration = 0
                bigTotalDuration = preset.sequence.sum()
                bigTimeRemaining = bigTotalDuration
            }
            "dual-sequence" -> {
                val firstBig = preset.sequence.firstOrNull() ?: 600
                timeRemaining = preset.dualSmallDuration; totalDuration = preset.dualSmallDuration
                midTimeRemaining = 0; midTotalDuration = 0
                bigTimeRemaining = firstBig; bigTotalDuration = firstBig
            }
        }
    }

    fun start() { isActive = true; isRinging = false }
    fun pause() { isActive = false }

    fun reset() { initialize() }

    fun restoreState(
        timeRemaining: Int,
        midTimeRemaining: Int,
        bigTimeRemaining: Int,
        currentIndex: Int
    ) {
        this.timeRemaining = timeRemaining
        this.midTimeRemaining = midTimeRemaining
        this.bigTimeRemaining = bigTimeRemaining
        this.currentIndex = currentIndex
    }

    fun changePreset(newPreset: TimerPreset) {
        preset = newPreset
        initialize()
    }

    sealed interface TickEvent {
        data object None : TickEvent
        data object IntervalComplete : TickEvent
        data object SequenceComplete : TickEvent
    }

    /**
     * Advance the timer by 1 second. Returns a TickEvent indicating
     * what happened (nothing, interval ended, or full sequence ended).
     */
    fun tick(): TickEvent {
        if (!isActive || isRinging) return TickEvent.None

        // Decrement all active tiers
        timeRemaining--
        if (mode == "dual.5") midTimeRemaining--
        if (isDual || mode == "calendar") bigTimeRemaining--

        // === A. BIG TIMER COMPLETE (Dual and Calendar modes) ===
        if ((isDual || mode == "calendar") && bigTimeRemaining <= 0) {
            if (mode == "dual-sequence" && currentIndex < preset.sequence.size - 1) {
                // Move to next sequence block
                if (preset.alarmBehavior == "alarm") {
                    isActive = false; isRinging = true
                    return TickEvent.IntervalComplete
                } else {
                    val nextIdx = currentIndex + 1
                    currentIndex = nextIdx
                    val nextBig = preset.sequence[nextIdx]
                    bigTimeRemaining = nextBig; bigTotalDuration = nextBig
                    timeRemaining = preset.dualSmallDuration; totalDuration = preset.dualSmallDuration
                    return TickEvent.IntervalComplete
                }
            }
            // Session over
            isActive = false
            timeRemaining = 0; midTimeRemaining = 0; bigTimeRemaining = 0
            if (preset.alarmBehavior == "alarm") isRinging = true
            return TickEvent.SequenceComplete
        }

        // === B. MID TIMER COMPLETE (Dual.5 only) ===
        if (mode == "dual.5" && midTimeRemaining <= 0) {
            if (preset.alarmBehavior == "alarm") {
                isActive = false; isRinging = true
                return TickEvent.IntervalComplete
            } else {
                val nextMid = minOf(preset.dualMidDuration, bigTimeRemaining)
                midTimeRemaining = nextMid; midTotalDuration = nextMid
                timeRemaining = preset.dualSmallDuration; totalDuration = preset.dualSmallDuration
                return TickEvent.IntervalComplete
            }
        }

        // === C. SMALL TIMER COMPLETE ===
        if (timeRemaining <= 0) {
            return when (mode) {
                "classic" -> {
                    isActive = false
                    if (preset.alarmBehavior == "alarm") isRinging = true
                    TickEvent.SequenceComplete
                }
                "dual" -> {
                    if (preset.alarmBehavior == "alarm") {
                        isActive = false; isRinging = true
                    } else {
                        val next = minOf(preset.dualSmallDuration, bigTimeRemaining)
                        timeRemaining = next; totalDuration = next
                    }
                    TickEvent.IntervalComplete
                }
                "dual.5" -> {
                    // Micro stopwatch always auto-loops
                    val next = minOf(preset.dualSmallDuration, midTimeRemaining)
                    timeRemaining = next; totalDuration = next
                    TickEvent.IntervalComplete
                }
                "sequence" -> {
                    if (currentIndex < preset.sequence.size - 1) {
                        if (preset.alarmBehavior == "alarm") {
                            isActive = false; isRinging = true
                        } else {
                            val nextIdx = currentIndex + 1
                            currentIndex = nextIdx
                            timeRemaining = preset.sequence[nextIdx]
                            totalDuration = preset.sequence[nextIdx]
                        }
                        TickEvent.IntervalComplete
                    } else {
                        isActive = false
                        if (preset.alarmBehavior == "alarm") isRinging = true
                        TickEvent.SequenceComplete
                    }
                }
                "calendar" -> {
                    isActive = false
                    isRinging = true
                    if (currentIndex < preset.sequence.size - 1) {
                        TickEvent.IntervalComplete
                    } else {
                        TickEvent.SequenceComplete
                    }
                }
                "dual-sequence" -> {
                    if (preset.alarmBehavior == "alarm") {
                        isActive = false; isRinging = true
                    } else {
                        val next = minOf(preset.dualSmallDuration, bigTimeRemaining)
                        timeRemaining = next; totalDuration = next
                    }
                    TickEvent.IntervalComplete
                }
                else -> TickEvent.None
            }
        }

        return TickEvent.None
    }

    /**
     * Dismiss the alarm and resume or end the session.
     * Mirrors the dismissAlarm() logic from useTimer.js.
     */
    fun dismissAlarm() {
        isRinging = false

        if (isDual && bigTimeRemaining <= 0) {
            // Full session ended
            isActive = false
            return
        }

        when (mode) {
            "dual.5" -> {
                if (midTimeRemaining <= 0) {
                    val nextMid = minOf(preset.dualMidDuration, bigTimeRemaining)
                    midTimeRemaining = nextMid; midTotalDuration = nextMid
                    val nextSub = minOf(preset.dualSmallDuration, nextMid)
                    timeRemaining = nextSub; totalDuration = nextSub
                }
                isActive = true
            }
            "dual" -> {
                if (timeRemaining <= 0) {
                    val next = minOf(preset.dualSmallDuration, bigTimeRemaining)
                    timeRemaining = next; totalDuration = next
                }
                isActive = true
            }
            "sequence" -> {
                if (timeRemaining <= 0 && currentIndex < preset.sequence.size - 1) {
                    val nextIdx = currentIndex + 1
                    currentIndex = nextIdx
                    timeRemaining = preset.sequence[nextIdx]
                    totalDuration = preset.sequence[nextIdx]
                    isActive = true
                }
            }
            "calendar" -> {
                if (timeRemaining <= 0 && currentIndex < preset.sequence.size - 1) {
                    val nextIdx = currentIndex + 1
                    currentIndex = nextIdx
                    timeRemaining = preset.sequence[nextIdx]
                    totalDuration = preset.sequence[nextIdx]
                    bigTimeRemaining = preset.sequence.drop(nextIdx).sum()
                    isActive = true
                } else {
                    isActive = false
                }
            }
            "dual-sequence" -> {
                if (bigTimeRemaining <= 0 && currentIndex < preset.sequence.size - 1) {
                    val nextIdx = currentIndex + 1
                    currentIndex = nextIdx
                    val nextBig = preset.sequence[nextIdx]
                    bigTimeRemaining = nextBig; bigTotalDuration = nextBig
                    timeRemaining = preset.dualSmallDuration; totalDuration = preset.dualSmallDuration
                    isActive = true
                } else if (timeRemaining <= 0) {
                    val next = minOf(preset.dualSmallDuration, bigTimeRemaining)
                    timeRemaining = next; totalDuration = next
                    isActive = true
                }
            }
            "classic" -> {
                // Classic session is done after alarm
                isActive = false
            }
        }
    }

    /**
     * Skip to the next interval (for sequence and dual-sequence modes).
     */
    fun skip() {
        when (mode) {
            "sequence" -> {
                if (currentIndex < preset.sequence.size - 1) {
                    val nextIdx = currentIndex + 1
                    currentIndex = nextIdx
                    timeRemaining = preset.sequence[nextIdx]
                    totalDuration = preset.sequence[nextIdx]
                } else {
                    isActive = false; timeRemaining = 0
                }
            }
            "calendar" -> {
                if (currentIndex < preset.sequence.size - 1) {
                    val nextIdx = currentIndex + 1
                    currentIndex = nextIdx
                    timeRemaining = preset.sequence[nextIdx]
                    totalDuration = preset.sequence[nextIdx]
                    bigTimeRemaining = preset.sequence.drop(nextIdx).sum()
                } else {
                    isActive = false; timeRemaining = 0; bigTimeRemaining = 0
                }
            }
            "dual-sequence" -> {
                if (currentIndex < preset.sequence.size - 1) {
                    val nextIdx = currentIndex + 1
                    currentIndex = nextIdx
                    bigTimeRemaining = preset.sequence[nextIdx]
                    bigTotalDuration = preset.sequence[nextIdx]
                    timeRemaining = preset.dualSmallDuration
                    totalDuration = preset.dualSmallDuration
                } else {
                    isActive = false; timeRemaining = 0; bigTimeRemaining = 0
                }
            }
        }
    }
}
