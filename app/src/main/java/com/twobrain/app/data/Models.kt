package com.twobrain.app.data

import android.net.Uri
import java.time.LocalDate
import java.time.LocalDateTime

data class AgendaEvent(
    val id: Long,
    val title: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val allDay: Boolean,
    val location: String? = null,
    val color: Int? = null,
)

data class DaySection(
    val date: LocalDate,
    val events: List<AgendaEvent>,
)

data class Photo(val uri: Uri, val takenAt: LocalDateTime?)
