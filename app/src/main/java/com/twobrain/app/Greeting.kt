package com.twobrain.app

import java.time.LocalDateTime

object Greeting {
    /** "Good morning", "Good afternoon", "Good evening", "Hey" — chosen by the clock. */
    fun forTime(now: LocalDateTime, name: String? = null): String {
        val base = when (now.hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Hey"
        }
        return if (name.isNullOrBlank()) base else "$base, $name"
    }

    /** A small second line that changes with the time of day. */
    fun mood(now: LocalDateTime): String = when (now.hour) {
        in 5..8 -> "Fresh start."
        in 9..11 -> "Here's your day."
        in 12..16 -> "Here's the rest of your day."
        in 17..21 -> "Winding down."
        else -> "Still up?"
    }
}
