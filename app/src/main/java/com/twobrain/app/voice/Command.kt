package com.twobrain.app.voice

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

sealed interface Command {
    data class Agenda(val from: LocalDate, val days: Int, val label: String) : Command
    data class AddEvent(val title: String, val start: LocalDateTime, val minutes: Long, val allDay: Boolean) : Command
    data object WhereAmI : Command
    data class Navigate(val destination: String) : Command
    data class Photos(val from: LocalDate?, val to: LocalDate?, val label: String) : Command
    data class Timer(val seconds: Int, val spoken: String) : Command
    data class Alarm(val time: LocalTime, val label: String?) : Command
    data class SetName(val name: String) : Command
    data object Time : Command
    data object Date : Command
    data object Close : Command
    data object Help : Command
    data object Sleep : Command
    data object Wake : Command
    data object Thanks : Command
    data class Unknown(val heard: String) : Command
}

/**
 * Turns a spoken sentence into a [Command]. Deliberately rule-based: fast, offline, predictable.
 */
object CommandParser {

    fun parse(raw: String, now: LocalDateTime = LocalDateTime.now()): Command {
        val s = normalize(raw)
        val today = now.toLocalDate()

        if (s.isBlank()) return Command.Unknown(raw)
        if (s.matches(Regex("(ok |okay )?(hey |hi )?(2 ?brain|two ?brain|to brain|wake up|i'?m back)"))) return Command.Wake
        if (s.matches(Regex("(go to sleep|sleep|stop listening|pause|be quiet|mute)( please)?"))) return Command.Sleep
        if (s.matches(Regex("(close|close (it|this|that|photos?|pictures?)|go back|back|go home|home|done|dismiss|never ?mind|cancel)( please)?"))) return Command.Close
        if (s.matches(Regex("(thanks|thank you|thank you so much|cheers|perfect|great|awesome|cool)( 2 ?brain| two ?brain)?"))) return Command.Thanks
        if (s.contains("what can you do") || s == "help" || s.contains("what can i say")) return Command.Help

        Regex("^(?:call me|my name is|my name's) (.+)$").find(s)?.let {
            return Command.SetName(it.groupValues[1].trim().split(" ").joinToString(" ") { w -> w.replaceFirstChar(Char::titlecase) })
        }

        if (s.matches(Regex("(what'?s|what is) (the )?time( is it)?( now)?|what time is it( now)?"))) return Command.Time
        if (s.matches(Regex("(what'?s|what is) (the )?(date|day)( today)?|what day is (it|today)( today)?"))) return Command.Date

        if (s.matches(Regex("where am i( right now)?|(what'?s|what is) my (location|address)|where are we"))) return Command.WhereAmI
        Regex("^(?:navigate|directions|take me|get me|drive|walk) (?:me )?(?:to|home to)? ?(.+)$").find(s)?.let {
            val dest = it.groupValues[1].removePrefix("to ").trim()
            if (dest.isNotEmpty()) return Command.Navigate(dest)
        }

        parseTimer(s)?.let { return it }
        parseAlarm(s)?.let { return it }
        parseAdd(s, now)?.let { return it }
        parsePhotos(s, today)?.let { return it }
        parseAgenda(s, today)?.let { return it }

        return Command.Unknown(raw)
    }

    // ---------- pieces ----------

    private fun parseAgenda(s: String, today: LocalDate): Command? {
        val asks = Regex("(what'?s|what is|what do i have|what have i got|anything|do i have|show|read|tell me|my|schedule|agenda|calendar|plans|events|meetings|busy|free)")
        if (!asks.containsMatchIn(s)) return null
        return when {
            s.contains("this week") || s.contains("the week") || s.contains("next few days") ->
                Command.Agenda(today, 7, "this week")
            s.contains("weekend") -> {
                val sat = nextOrSame(today, DayOfWeek.SATURDAY)
                Command.Agenda(if (today.dayOfWeek == DayOfWeek.SUNDAY) today else sat, if (today.dayOfWeek == DayOfWeek.SUNDAY) 1 else 2, "this weekend")
            }
            s.contains("tomorrow") -> Command.Agenda(today.plusDays(1), 1, "tomorrow")
            else -> dayWord(s, today)?.let { (d, label) -> Command.Agenda(d, 1, label) }
                ?: if (s.contains("today") || s.contains("schedule") || s.contains("agenda") || s.contains("calendar") || s.contains("plans") || s.contains("my day"))
                    Command.Agenda(today, 1, "today") else null
        }
    }

    private fun parsePhotos(s: String, today: LocalDate): Command? {
        if (!Regex("\\b(photos?|pictures?|pics|images|gallery|camera roll|selfies?)\\b").containsMatchIn(s)) return null
        return when {
            s.contains("yesterday") -> today.minusDays(1).let { Command.Photos(it, it, "yesterday") }
            s.contains("today") -> Command.Photos(today, today, "today")
            s.contains("last week") -> Command.Photos(today.minusDays(13), today.minusDays(7), "last week")
            s.contains("this week") || s.contains("past week") -> Command.Photos(today.minusDays(6), today, "this week")
            s.contains("last month") || s.contains("past month") || s.contains("this month") -> Command.Photos(today.minusDays(30), today, "this month")
            s.contains("last weekend") || s.contains("weekend") -> {
                val sat = today.minusDays(((today.dayOfWeek.value - DayOfWeek.SATURDAY.value + 7) % 7).toLong().let { if (it == 0L) 7 else it })
                Command.Photos(sat, sat.plusDays(1), "last weekend")
            }
            else -> Command.Photos(null, null, "recent")
        }
    }

    private fun parseAdd(s: String, now: LocalDateTime): Command? {
        val m = Regex("^(?:please )?(?:add|schedule|create|put|book|set up|make|remind me(?: to)?)(?: an?| my)?(?: new)?(?: event| meeting| appointment| reminder)?(?: called| for| to)? (.+)$").find(s)
            ?: return null
        var rest = m.groupValues[1]
        rest = rest.replace(Regex("\\b(to|on|in) (my|the) calendar\\b"), " ").trim()

        val today = now.toLocalDate()
        var date: LocalDate? = null
        dayWord(rest, today)?.let { (d, _) -> date = d }
        rest = rest.replace(DAY_WORD, " ")

        var time: LocalTime? = null
        TIME.find(rest)?.let { tm ->
            time = toTime(tm)
            rest = rest.removeRange(tm.range)
        }
        var minutes = 60L
        Regex("\\bfor (an? |one )?(\\d+)? ?(hours?|minutes?|mins?)\\b").find(rest)?.let { d ->
            val n = d.groupValues[2].toLongOrNull() ?: 1
            minutes = if (d.groupValues[3].startsWith("h")) n * 60 else n
            rest = rest.removeRange(d.range)
        }
        val title = rest.replace(Regex("\\s+"), " ").trim().trimEnd('.', ',')
            .removeSuffix(" on").removeSuffix(" at").removeSuffix(" for").trim()
            .replaceFirstChar(Char::titlecase)
        if (title.isEmpty()) return null

        val allDay = time == null
        val day = date ?: if (time != null && time!!.isBefore(now.toLocalTime())) today.plusDays(1) else today
        return Command.AddEvent(title, day.atTime(time ?: LocalTime.MIDNIGHT), minutes, allDay)
    }

    private fun parseTimer(s: String): Command? {
        val m = Regex("(?:set |start )?(?:a )?timer (?:for )?(.+)|(.+) timer$").find(s) ?: return null
        val body = m.groupValues[1].ifEmpty { m.groupValues[2] }
        var total = 0
        val parts = mutableListOf<String>()
        Regex("(\\d+|an?|one|half an?) ?(hours?|minutes?|mins?|seconds?|secs?)").findAll(body).forEach { p ->
            val n = when (p.groupValues[1]) { "a", "an", "one" -> 1; "half a", "half an" -> 0; else -> p.groupValues[1].toInt() }
            val unit = p.groupValues[2]
            val half = p.groupValues[1].startsWith("half")
            total += when {
                unit.startsWith("h") -> if (half) 1800 else n * 3600
                unit.startsWith("m") -> if (half) 30 else n * 60
                else -> n
            }
            parts += p.value
        }
        if (total <= 0) return null
        return Command.Timer(total, parts.joinToString(" and "))
    }

    private fun parseAlarm(s: String): Command? {
        if (!Regex("\\b(alarm|wake me( up)?)\\b").containsMatchIn(s)) return null
        val tm = TIME.find(s) ?: return null
        return Command.Alarm(toTime(tm) ?: return null, null)
    }

    // ---------- helpers ----------

    private val WEEKDAYS = DayOfWeek.entries.associateBy { it.name.lowercase() }
    private val DAY_WORD = Regex("\\b(?:on |this |next )?(today|tonight|tomorrow|day after tomorrow|monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b")

    /** Optional "at", hour, optional :mm, optional am/pm — or noon/midnight. */
    private val TIME = Regex("\\b(?:at |by |around )?(?:(noon|midday|midnight)|(\\d{1,2})(?::(\\d{2}))? ?(am|pm|in the morning|in the afternoon|in the evening|at night|tonight)?)\\b(?! ?(hours?|minutes?|mins?|days?|people|of))")

    private fun toTime(m: MatchResult): LocalTime? {
        m.groupValues[1].takeIf { it.isNotEmpty() }?.let { return if (it == "midnight") LocalTime.MIDNIGHT else LocalTime.NOON }
        var h = m.groupValues[2].toIntOrNull() ?: return null
        val min = m.groupValues[3].toIntOrNull() ?: 0
        val suffix = m.groupValues[4]
        // Require an "at" or a suffix for bare numbers, so "dinner with 2" isn't 2:00.
        val hasAt = m.value.trimStart().let { it.startsWith("at ") || it.startsWith("by ") || it.startsWith("around ") }
        if (suffix.isEmpty() && m.groupValues[3].isEmpty() && !hasAt) return null
        val pm = suffix == "pm" || suffix.contains("afternoon") || suffix.contains("evening") || suffix.contains("night")
        if (h > 23 || min > 59) return null
        if (pm && h < 12) h += 12
        if (suffix == "am" && h == 12) h = 0
        // Bare "at 3" -> assume afternoon for 1..7.
        if (suffix.isEmpty() && h in 1..7) h += 12
        return LocalTime.of(h, min)
    }

    private fun dayWord(s: String, today: LocalDate): Pair<LocalDate, String>? {
        val m = DAY_WORD.find(s) ?: return null
        val w = m.groupValues[1]
        return when (w) {
            "today", "tonight" -> today to "today"
            "tomorrow" -> today.plusDays(1) to "tomorrow"
            "day after tomorrow" -> today.plusDays(2) to "the day after tomorrow"
            else -> {
                val target = WEEKDAYS.getValue(w)
                val next = m.value.contains("next ")
                var d = nextOrSame(today, target)
                if (next && d == today) d = d.plusDays(7)
                d to w.replaceFirstChar(Char::titlecase)
            }
        }
    }

    private fun nextOrSame(from: LocalDate, dow: DayOfWeek): LocalDate =
        from.plusDays(((dow.value - from.dayOfWeek.value + 7) % 7).toLong())

    fun normalize(raw: String): String = raw.lowercase()
        .replace("a.m.", "am").replace("p.m.", "pm")
        .replace(Regex("(\\d)\\s?(am|pm)\\b"), "$1 $2")
        .replace(Regex("(\\d{1,2}):00\\b"), "$1")
        .replace(Regex("[?!,]"), "")
        .replace(Regex("\\.$"), "")
        .replace("’", "'")
        .replace(Regex("\\s+"), " ")
        .trim()
}
