package com.twobrain.app

import com.twobrain.app.voice.Command
import com.twobrain.app.voice.CommandParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class CommandParserTest {
    // Sunday 22 Nov 2026, 9:00
    private val now = LocalDateTime.of(2026, 11, 22, 9, 0)
    private fun p(s: String) = CommandParser.parse(s, now)

    @Test fun agenda() {
        assertEquals(Command.Agenda(now.toLocalDate(), 1, "today"), p("What's on today?"))
        assertEquals(Command.Agenda(now.toLocalDate().plusDays(1), 1, "tomorrow"), p("what do I have tomorrow"))
        assertEquals(Command.Agenda(LocalDate.of(2026, 11, 27), 1, "Friday"), p("What's on Friday"))
        assertEquals(7, (p("show me my week, this week") as Command.Agenda).days)
    }

    @Test fun addEvent() {
        val c = p("Add lunch with Sam on Friday at 12:30 p.m.") as Command.AddEvent
        assertEquals("Lunch with sam", c.title)
        assertEquals(LocalDateTime.of(2026, 11, 27, 12, 30), c.start)
        assertEquals(false, c.allDay)

        val d = p("schedule dentist tomorrow at 3") as Command.AddEvent
        assertEquals(LocalDateTime.of(2026, 11, 23, 15, 0), d.start)
        assertEquals("Dentist", d.title)

        val r = p("remind me to call mom at 6 pm") as Command.AddEvent
        assertEquals("Call mom", r.title)
        assertEquals(LocalTime.of(18, 0), r.start.toLocalTime())

        val a = p("add mum's birthday on Tuesday") as Command.AddEvent
        assertTrue(a.allDay)
        assertEquals(LocalDate.of(2026, 11, 24), a.start.toLocalDate())

        val m = p("add team sync tomorrow at 10 am for 30 minutes") as Command.AddEvent
        assertEquals(30, m.minutes); assertEquals("Team sync", m.title)
    }

    @Test fun photos() {
        assertEquals(Command.Photos(LocalDate.of(2026, 11, 21), LocalDate.of(2026, 11, 21), "yesterday"), p("show me my photos from yesterday"))
        assertEquals("recent", (p("show pictures") as Command.Photos).label)
    }

    @Test fun other() {
        assertEquals(Command.WhereAmI, p("Where am I?"))
        assertEquals(Command.Navigate("the airport"), p("navigate to the airport"))
        assertEquals(Command.Timer(600, "10 minutes"), p("set a timer for 10 minutes"))
        assertEquals(Command.Alarm(LocalTime.of(7, 0), null), p("set an alarm for 7 am"))
        assertEquals(Command.SetName("Alex"), p("call me alex"))
        assertEquals(Command.Close, p("close"))
        assertEquals(Command.Sleep, p("stop listening"))
        assertEquals(Command.Wake, p("Hey 2brain"))
        assertEquals(Command.Time, p("what time is it"))
        assertTrue(p("purple monkey dishwasher") is Command.Unknown)
    }

    @Test fun greeting() {
        assertEquals("Good morning", Greeting.forTime(now))
        assertEquals("Good afternoon, Alex", Greeting.forTime(now.withHour(14), "Alex"))
        assertEquals("Good evening", Greeting.forTime(now.withHour(19)))
        assertEquals("Hey", Greeting.forTime(now.withHour(1)))
    }
}
