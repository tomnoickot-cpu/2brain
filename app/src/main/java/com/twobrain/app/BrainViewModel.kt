package com.twobrain.app

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.twobrain.app.data.CalendarRepository
import com.twobrain.app.data.DaySection
import com.twobrain.app.data.LocationRepository
import com.twobrain.app.data.Photo
import com.twobrain.app.data.PhotosRepository
import com.twobrain.app.voice.Command
import com.twobrain.app.voice.CommandParser
import com.twobrain.app.voice.Speaker
import com.twobrain.app.voice.VoiceListener
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class VoicePhase { Listening, Hearing, Thinking, Speaking, Asleep, Unavailable }

data class VoiceState(
    val phase: VoicePhase = VoicePhase.Listening,
    val transcript: String = "",
    val reply: String = "",
    val level: Float = 0f,
)

sealed interface Overlay {
    data class Photos(val title: String, val photos: List<Photo>) : Overlay
    data class Agenda(val title: String, val days: List<DaySection>) : Overlay
}

data class BrainState(
    val now: LocalDateTime = LocalDateTime.now(),
    val name: String? = null,
    val place: String? = null,
    val days: List<DaySection> = emptyList(),
    val calendarAllowed: Boolean = true,
    val voice: VoiceState = VoiceState(),
    val overlay: Overlay? = null,
)

class BrainViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx: Context get() = getApplication()
    private val prefs = app.getSharedPreferences("2brain", Context.MODE_PRIVATE)
    private val calendar = CalendarRepository(app)
    private val location = LocationRepository(app)
    private val photos = PhotosRepository(app)

    private val _state = MutableStateFlow(BrainState(name = prefs.getString("name", null)))
    val state: StateFlow<BrainState> = _state

    private var replyClear: Job? = null

    private val speaker = Speaker(app) { speaking ->
        viewModelScope.launch {
            if (speaking) {
                listener.hold()
                setPhase(VoicePhase.Speaking)
            } else {
                setPhase(if (_state.value.voice.phase == VoicePhase.Asleep) VoicePhase.Asleep else VoicePhase.Listening)
                listener.release()
            }
        }
    }

    private val listener = VoiceListener(
        context = app,
        onPartial = { t -> if (!asleep) voice { it.copy(transcript = t, phase = VoicePhase.Hearing) } },
        onUtterance = ::onHeard,
        onLevel = { l -> voice { it.copy(level = l) } },
        onState = { hearing ->
            if (!asleep) voice {
                when {
                    hearing -> it.copy(phase = VoicePhase.Hearing)
                    it.phase == VoicePhase.Hearing -> it.copy(phase = VoicePhase.Listening, level = 0f)
                    else -> it
                }
            }
        },
        onUnavailable = { voice { it.copy(phase = VoicePhase.Unavailable, reply = "Speech recognition isn't available on this phone.") } },
    )

    private val asleep get() = _state.value.voice.phase == VoicePhase.Asleep

    init {
        viewModelScope.launch {
            while (isActive) {
                _state.update { it.copy(now = LocalDateTime.now()) }
                delay(30_000)
            }
        }
    }

    // ---------- lifecycle (called from the Activity) ----------

    fun onResume() {
        refresh()
        if (granted(Manifest.permission.RECORD_AUDIO)) listener.start()
        else voice { it.copy(phase = VoicePhase.Unavailable, reply = "2brain needs the microphone.") }
    }

    fun onPause() = listener.stop()

    fun refresh() = viewModelScope.launch {
        val now = LocalDateTime.now()
        val calOk = granted(Manifest.permission.READ_CALENDAR)
        val days = if (calOk) runCatching { calendar.agenda(now.toLocalDate(), 7) }.getOrDefault(emptyList()) else emptyList()
        _state.update { it.copy(now = now, days = days, calendarAllowed = calOk) }
        if (granted(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            location.current()?.let { p -> _state.update { it.copy(place = p.short) } }
        }
    }

    override fun onCleared() {
        listener.stop()
        speaker.shutdown()
    }

    // ---------- voice ----------

    private fun onHeard(text: String) {
        val cmd = CommandParser.parse(text)
        if (asleep) {
            if (cmd == Command.Wake) {
                setPhase(VoicePhase.Listening)
                say("I'm here.")
            }
            return
        }
        voice { it.copy(transcript = text, phase = VoicePhase.Thinking) }
        viewModelScope.launch { say(execute(cmd)) }
    }

    private suspend fun execute(cmd: Command): String = when (cmd) {
        is Command.Agenda -> {
            if (!granted(Manifest.permission.READ_CALENDAR)) "I need calendar access for that."
            else {
                val days = calendar.agenda(cmd.from, cmd.days)
                if (cmd.days > 1 || cmd.from != _state.value.now.toLocalDate()) {
                    _state.update { it.copy(overlay = Overlay.Agenda(cmd.label.replaceFirstChar(Char::titlecase), days)) }
                }
                summarize(cmd.label, days)
            }
        }
        is Command.AddEvent -> {
            if (!granted(Manifest.permission.WRITE_CALENDAR)) "I need calendar access to add that."
            else if (calendar.addEvent(cmd.title, cmd.start, cmd.minutes, cmd.allDay)) {
                refresh()
                val whenText = dayName(cmd.start) + if (cmd.allDay) "" else " at " + clock(cmd.start)
                "Added ${cmd.title}, $whenText."
            } else "I couldn't find a calendar I can write to."
        }
        Command.WhereAmI -> {
            if (!granted(Manifest.permission.ACCESS_COARSE_LOCATION)) "I need location access for that."
            else location.current()?.let { p ->
                _state.update { it.copy(place = p.short) }
                "You're at ${p.full}."
            } ?: "I can't find you right now."
        }
        is Command.Navigate -> {
            openIntent(Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + Uri.encode(cmd.destination))))
                ?: openIntent(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(cmd.destination))))
            "Heading to ${cmd.destination}."
        }
        is Command.Photos -> {
            if (!(granted(Manifest.permission.READ_MEDIA_IMAGES) || granted(Manifest.permission.READ_EXTERNAL_STORAGE) ||
                    granted("android.permission.READ_MEDIA_VISUAL_USER_SELECTED"))) "I need access to your photos for that."
            else {
                val list = photos.photos(cmd.from, cmd.to)
                if (list.isEmpty()) "No photos from ${cmd.label}."
                else {
                    val title = if (cmd.label == "recent") "Recent" else cmd.label.replaceFirstChar(Char::titlecase)
                    _state.update { it.copy(overlay = Overlay.Photos(title, list)) }
                    val n = list.size
                    if (cmd.label == "recent") "Here are your latest photos." else "$n photo${if (n == 1) "" else "s"} from ${cmd.label}."
                }
            }
        }
        is Command.Timer -> {
            openIntent(Intent(AlarmClock.ACTION_SET_TIMER).putExtra(AlarmClock.EXTRA_LENGTH, cmd.seconds).putExtra(AlarmClock.EXTRA_SKIP_UI, true))
            "Timer set for ${cmd.spoken}."
        }
        is Command.Alarm -> {
            openIntent(Intent(AlarmClock.ACTION_SET_ALARM).putExtra(AlarmClock.EXTRA_HOUR, cmd.time.hour)
                .putExtra(AlarmClock.EXTRA_MINUTES, cmd.time.minute).putExtra(AlarmClock.EXTRA_SKIP_UI, true))
            "Alarm set for ${clock(cmd.time.atDate(_state.value.now.toLocalDate()))}."
        }
        is Command.SetName -> {
            prefs.edit().putString("name", cmd.name).apply()
            _state.update { it.copy(name = cmd.name) }
            "Nice to meet you, ${cmd.name}."
        }
        Command.Time -> "It's ${clock(LocalDateTime.now())}."
        Command.Date -> "It's " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())) + "."
        Command.Close -> { _state.update { it.copy(overlay = null) }; "" }
        Command.Help -> "Try: what's tomorrow, add lunch with Sam Friday at noon, show photos from yesterday, where am I, set a timer for ten minutes, or go to sleep."
        Command.Sleep -> { setPhase(VoicePhase.Asleep); "Going quiet. Say 2brain to wake me." }
        Command.Wake -> "I'm listening."
        Command.Thanks -> "Anytime."
        is Command.Unknown -> "Sorry, I didn't catch that."
    }

    private fun summarize(label: String, days: List<DaySection>): String {
        val events = days.flatMap { it.events }
        if (events.isEmpty()) return "Nothing on $label."
        if (days.size == 1) {
            val e = events
            val first = e.take(3).joinToString(", ") { ev -> if (ev.allDay) ev.title else "${ev.title} at ${clock(ev.start)}" }
            val more = if (e.size > 3) ", and ${e.size - 3} more" else ""
            return "${e.size} thing${if (e.size == 1) "" else "s"} $label: $first$more."
        }
        val busy = days.count { it.events.isNotEmpty() }
        return "${events.size} events across $busy day${if (busy == 1) "" else "s"} $label."
    }

    private fun say(text: String) {
        voice { it.copy(reply = text, phase = if (it.phase == VoicePhase.Asleep) VoicePhase.Asleep else VoicePhase.Listening) }
        if (text.isNotBlank()) speaker.speak(text)
        replyClear?.cancel()
        replyClear = viewModelScope.launch {
            delay(9000)
            voice { it.copy(reply = "", transcript = "") }
        }
    }

    private fun openIntent(intent: Intent): Unit? = runCatching {
        ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.getOrNull()

    private fun granted(p: String) = ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED

    private fun setPhase(p: VoicePhase) = voice { it.copy(phase = p, level = 0f) }
    private inline fun voice(f: (VoiceState) -> VoiceState) = _state.update { it.copy(voice = f(it.voice)) }

    companion object {
        fun clock(t: LocalDateTime): String =
            t.format(DateTimeFormatter.ofPattern(if (t.minute == 0) "h a" else "h:mm a", Locale.US)).lowercase()

        fun dayName(t: LocalDateTime): String {
            val today = LocalDateTime.now().toLocalDate()
            return when (t.toLocalDate()) {
                today -> "today"
                today.plusDays(1) -> "tomorrow"
                else -> t.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault()))
            }
        }

    }
}
