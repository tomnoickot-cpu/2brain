package com.twobrain.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.twobrain.app.BrainState
import com.twobrain.app.BrainViewModel
import com.twobrain.app.Greeting
import com.twobrain.app.VoicePhase
import com.twobrain.app.VoiceState
import com.twobrain.app.data.AgendaEvent
import com.twobrain.app.data.DaySection
import com.twobrain.app.ui.theme.Brain
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(state: BrainState, frozenOrb: Float? = null) {
    val c = Brain.colors
    Box(
        Modifier
            .fillMaxSize()
            .background(c.paper)
    ) {
        Wash()
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 28.dp, end = 28.dp, bottom = 230.dp),
        ) {
            item { Header(state) }
            item { Spacer(Modifier.height(40.dp)) }
            if (!state.calendarAllowed) {
                item { Quiet("Calendar is off. Say “what can you do” to hear more.") }
            } else {
                agenda(state.days, state.now)
            }
        }
        VoiceDock(state.voice, Modifier.align(Alignment.BottomCenter), frozenOrb)
        AnimatedVisibility(
            visible = state.overlay != null,
            enter = slideInVertically(tween(420)) { it / 6 } + fadeIn(tween(300)),
            exit = slideOutVertically(tween(300)) { it / 6 } + fadeOut(tween(220)),
        ) {
            state.overlay?.let { OverlaySheet(it, state.voice, frozenOrb) }
        }
    }
}

/** A very faint colour wash at the top, the only decoration on the page. */
@Composable
fun Wash() {
    val c = Brain.colors
    Canvas(Modifier.fillMaxWidth().height(420.dp)) {
        val a = if (c.dark) 0.16f else 0.22f
        drawCircle(
            Brush.radialGradient(listOf(c.glowA.copy(alpha = a), Color.Transparent), center = Offset(size.width * 0.05f, 0f), radius = size.width * 0.85f),
            radius = size.width * 0.85f, center = Offset(size.width * 0.05f, 0f),
        )
        drawCircle(
            Brush.radialGradient(listOf(c.glowB.copy(alpha = a * 0.8f), Color.Transparent), center = Offset(size.width * 0.95f, size.height * 0.1f), radius = size.width * 0.75f),
            radius = size.width * 0.75f, center = Offset(size.width * 0.95f, size.height * 0.1f),
        )
    }
}

@Composable
private fun Header(state: BrainState) {
    val c = Brain.colors; val t = Brain.type
    Column(Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(top = 36.dp)) {
        val date = state.now.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault()))
        Text(
            listOfNotNull(date, state.place).joinToString("  ·  "),
            style = t.subline, color = c.muted,
        )
        Spacer(Modifier.height(18.dp))
        AnimatedContent(Greeting.forTime(state.now, state.name), transitionSpec = { fadeIn(tween(600)) togetherWith fadeOut(tween(300)) }, label = "greet") {
            Text("$it.", style = t.greeting, color = c.ink)
        }
        Spacer(Modifier.height(6.dp))
        Text(Greeting.mood(state.now), style = t.greeting.copy(fontSize = t.greeting.fontSize * 0.62f, lineHeight = t.greeting.lineHeight * 0.62f), color = c.muted)
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.agenda(days: List<DaySection>, now: LocalDateTime) {
    val today = now.toLocalDate()
    val todaySec = days.firstOrNull { it.date == today } ?: DaySection(today, emptyList())
    val tomorrowSec = days.firstOrNull { it.date == today.plusDays(1) } ?: DaySection(today.plusDays(1), emptyList())
    val later = days.filter { it.date.isAfter(today.plusDays(1)) && it.events.isNotEmpty() }

    item { DayCard("Today", todaySec, now, emptyText = "Nothing planned. Enjoy the space.") }
    item { Spacer(Modifier.height(14.dp)) }
    item { DayCard("Tomorrow", tomorrowSec, now, emptyText = "Wide open.") }
    if (later.isNotEmpty()) {
        item { Spacer(Modifier.height(34.dp)); SectionLabel("Later") ; Spacer(Modifier.height(6.dp)) }
        items(later, key = { it.date.toString() }) { LaterDay(it) }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = Brain.type.label, color = Brain.colors.muted, modifier = modifier.padding(start = 4.dp))
}

@Composable
fun DayCard(label: String, day: DaySection, now: LocalDateTime, emptyText: String) {
    val c = Brain.colors; val t = Brain.type
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(c.card.copy(alpha = if (c.dark) 1f else 0.72f))
            .border(1.dp, c.hairline, RoundedCornerShape(26.dp))
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label.uppercase(), style = t.label, color = c.muted)
            Spacer(Modifier.weight(1f))
            if (day.events.isNotEmpty()) Text("${day.events.size}", style = t.label, color = c.faint)
        }
        Spacer(Modifier.height(12.dp))
        if (day.events.isEmpty()) {
            Text(emptyText, style = t.quote, color = c.muted)
        } else {
            val nextId = day.events.firstOrNull { !it.allDay && it.end.isAfter(now) }?.id
            day.events.forEachIndexed { i, e ->
                if (i > 0) Spacer(Modifier.height(12.dp))
                EventRow(e, past = !e.allDay && e.end.isBefore(now), next = e.id == nextId && day.date == now.toLocalDate())
            }
        }
    }
}

@Composable
fun EventRow(e: AgendaEvent, past: Boolean = false, next: Boolean = false) {
    val c = Brain.colors; val t = Brain.type
    val ink = if (past) c.faint else c.ink
    Row(verticalAlignment = Alignment.Top) {
        Text(
            if (e.allDay) "All day" else BrainViewModel.clock(e.start),
            style = t.time, color = if (past) c.faint else c.muted,
            modifier = Modifier.width(72.dp),
        )
        Box(
            Modifier.padding(top = 8.dp, end = 12.dp).size(6.dp).clip(CircleShape)
                .background(if (past) c.faint else e.color?.let { Color(it) } ?: c.ink)
        )
        Column(Modifier.weight(1f)) {
            Text(e.title, style = t.body, color = ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val sub = listOfNotNull(if (next) "Up next" else null, e.location).joinToString("  ·  ")
            if (sub.isNotEmpty()) Text(sub, style = t.small, color = if (past) c.faint else c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun LaterDay(day: DaySection) {
    val c = Brain.colors; val t = Brain.type
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 4.dp)) {
        Column(Modifier.width(72.dp)) {
            Text(day.date.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault())), style = t.body, color = c.ink)
            Text(day.date.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())), style = t.small, color = c.muted)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            day.events.take(3).forEach { e ->
                Row {
                    Text(e.title, style = t.body, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    Text("  " + if (e.allDay) "all day" else BrainViewModel.clock(e.start), style = t.small.copy(lineHeight = t.body.lineHeight), color = c.muted)
                }
            }
            if (day.events.size > 3) Text("+${day.events.size - 3} more", style = t.small, color = c.muted)
        }
    }
}

@Composable
fun Quiet(text: String) = Text(text, style = Brain.type.quote, color = Brain.colors.muted)

/** Bottom of the screen: transcript / reply, the orb, and a tiny status word. */
@Composable
fun VoiceDock(voice: VoiceState, modifier: Modifier = Modifier, frozenOrb: Float? = null) {
    val c = Brain.colors; val t = Brain.type
    Box(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(c.paper.copy(alpha = 0f), c.paper.copy(alpha = 0.94f), c.paper), startY = 0f, endY = 180f))
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(top = 40.dp, bottom = 14.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val line = when {
                voice.reply.isNotBlank() -> voice.reply
                voice.transcript.isNotBlank() -> "“${voice.transcript}”"
                else -> ""
            }
            val isReply = voice.reply.isNotBlank()
            AnimatedContent(line, transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) }, label = "line") { l ->
                if (l.isNotEmpty()) Text(
                    l, style = if (isReply) t.body else t.quote.copy(fontSize = t.quote.fontSize * 0.9f),
                    color = if (isReply) c.ink else c.muted,
                    textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 36.dp),
                )
            }
            VoiceOrb(voice.phase, voice.level, size = 64.dp, frozenTime = frozenOrb)
            Text(
                when (voice.phase) {
                    VoicePhase.Listening -> "Listening"
                    VoicePhase.Hearing -> "Hearing you"
                    VoicePhase.Thinking -> "Thinking"
                    VoicePhase.Speaking -> "Speaking"
                    VoicePhase.Asleep -> "Asleep · say “2brain”"
                    VoicePhase.Unavailable -> "Mic unavailable"
                },
                style = t.label, color = c.faint,
            )
        }
    }
}

fun dayTitle(d: LocalDate, today: LocalDate): String = when (d) {
    today -> "Today"
    today.plusDays(1) -> "Tomorrow"
    else -> d.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault()))
}
