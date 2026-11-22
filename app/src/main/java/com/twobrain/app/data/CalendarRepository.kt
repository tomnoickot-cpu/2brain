package com.twobrain.app.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class CalendarRepository(private val context: Context) {

    /** Events from [from] (inclusive) for [days] days, grouped by day, sorted. */
    suspend fun agenda(from: LocalDate, days: Int): List<DaySection> = withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        val begin = from.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = from.plusDays(days.toLong()).atStartOfDay(zone).toInstant().toEpochMilli()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, begin)
            ContentUris.appendId(it, end)
        }.build()
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.DISPLAY_COLOR,
        )
        val events = mutableListOf<AgendaEvent>()
        context.contentResolver.query(
            uri, projection, "${CalendarContract.Instances.VISIBLE} = 1", null,
            "${CalendarContract.Instances.BEGIN} ASC",
        )?.use { c ->
            while (c.moveToNext()) {
                val allDay = c.getInt(4) == 1
                // All-day events are stored at UTC midnight.
                val z = if (allDay) ZoneOffset.UTC else zone
                events += AgendaEvent(
                    id = c.getLong(0),
                    title = c.getString(1)?.takeIf { it.isNotBlank() } ?: "Untitled",
                    start = LocalDateTime.ofInstant(Instant.ofEpochMilli(c.getLong(2)), z),
                    end = LocalDateTime.ofInstant(Instant.ofEpochMilli(c.getLong(3)), z),
                    allDay = allDay,
                    location = c.getString(5)?.takeIf { it.isNotBlank() },
                    color = if (c.isNull(6)) null else c.getInt(6),
                )
            }
        }
        group(from, days, events)
    }

    /** Inserts into the primary writable calendar. Returns false if no calendar is writable. */
    suspend fun addEvent(title: String, start: LocalDateTime, minutes: Long, allDay: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            val calId = writableCalendarId() ?: return@withContext false
            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calId)
                put(CalendarContract.Events.TITLE, title)
                if (allDay) {
                    val d = start.toLocalDate()
                    put(CalendarContract.Events.DTSTART, d.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
                    put(CalendarContract.Events.DTEND, d.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
                    put(CalendarContract.Events.ALL_DAY, 1)
                    put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
                } else {
                    val zone = ZoneId.systemDefault()
                    put(CalendarContract.Events.DTSTART, start.atZone(zone).toInstant().toEpochMilli())
                    put(CalendarContract.Events.DTEND, start.plusMinutes(minutes).atZone(zone).toInstant().toEpochMilli())
                    put(CalendarContract.Events.EVENT_TIMEZONE, zone.id)
                }
            }
            context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values) != null
        }

    private fun writableCalendarId(): Long? {
        val projection = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.IS_PRIMARY)
        val selection = "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ? AND ${CalendarContract.Calendars.VISIBLE} = 1"
        val args = arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString())
        var fallback: Long? = null
        context.contentResolver.query(CalendarContract.Calendars.CONTENT_URI, projection, selection, args, null)?.use { c ->
            while (c.moveToNext()) {
                if (c.getInt(1) == 1) return c.getLong(0)
                if (fallback == null) fallback = c.getLong(0)
            }
        }
        return fallback
    }

    companion object {
        fun group(from: LocalDate, days: Int, events: List<AgendaEvent>): List<DaySection> =
            (0 until days).map { offset ->
                val day = from.plusDays(offset.toLong())
                DaySection(
                    date = day,
                    events = events.filter { e ->
                        val s = e.start.toLocalDate()
                        val last = if (e.allDay) e.end.toLocalDate().minusDays(1) else e.end.minusNanos(1).toLocalDate()
                        !day.isBefore(s) && !day.isAfter(maxOf(s, last))
                    }.sortedWith(compareBy({ !it.allDay }, { it.start })),
                )
            }
    }
}
