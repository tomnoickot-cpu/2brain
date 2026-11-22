package com.twobrain.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.net.Uri
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.test.junit4.createComposeRule
import com.github.takahirom.roborazzi.captureRoboImage
import com.twobrain.app.data.AgendaEvent
import com.twobrain.app.data.CalendarRepository
import com.twobrain.app.data.Photo
import com.twobrain.app.ui.HomeScreen
import com.twobrain.app.ui.LocalPhotoPainter
import com.twobrain.app.ui.WelcomeScreen
import com.twobrain.app.ui.theme.TwoBrainTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.LocalDateTime

/** Renders the real Compose screens with sample data. Output: app/build/outputs/roborazzi/. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val day = LocalDate.of(2026, 11, 22)
    private fun ev(id: Long, title: String, d: Int, h: Int, m: Int = 0, mins: Long = 60, loc: String? = null, color: Long? = null, allDay: Boolean = false) =
        AgendaEvent(id, title, day.plusDays(d.toLong()).atTime(h, m), day.plusDays(d.toLong()).atTime(h, m).plusMinutes(mins), allDay, loc, color?.toInt())

    private val events = listOf(
        ev(1, "Run along the river", 0, 7, 0, 45, color = 0xFF7BC8A4),
        ev(2, "Design review", 0, 10, 30, 60, "Studio 4", 0xFF9B87F5),
        ev(3, "Lunch with Maya", 0, 13, 0, 60, "Café Lumen", 0xFFF2A66B),
        ev(4, "Call with Dad", 0, 18, 30, 30, color = 0xFF6BB8F2),
        ev(5, "Dentist", 1, 9, 15, 45, "Dr. Ortiz, 4th Ave", 0xFFF2A66B),
        ev(6, "Product sync", 1, 14, 0, 30, color = 0xFF9B87F5),
        ev(7, "Ava's birthday", 3, 0, allDay = true, color = 0xFFE58FB4),
        ev(8, "Dinner at Nonna's", 3, 19, 30),
        ev(9, "Flight to Lisbon", 5, 8, 40, 180, "JFK T4"),
    ).map { if (it.allDay) it.copy(start = it.start.toLocalDate().atStartOfDay(), end = it.start.toLocalDate().plusDays(1).atStartOfDay()) else it }

    private val days = CalendarRepository.group(day, 7, events)

    private fun state(hour: Int, min: Int = 0, voice: VoiceState = VoiceState(), overlay: Overlay? = null) = BrainState(
        now = LocalDateTime.of(2026, 11, 22, hour, min), place = "Brooklyn", days = days, voice = voice, overlay = overlay,
    )

    private fun shoot(name: String, dark: Boolean = false, content: @androidx.compose.runtime.Composable () -> Unit) {
        compose.setContent {
            TwoBrainTheme(dark = dark) {
                CompositionLocalProvider(LocalPhotoPainter provides { p -> BitmapPainter(sample(p.uri.lastPathSegment!!.toInt()).asImageBitmap()) }) { content() }
            }
        }
        compose.mainClock.autoAdvance = false
        compose.mainClock.advanceTimeBy(1200)
        compose.onRoot().captureRoboImage("../screenshots/$name.png")
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onRoot() =
        onNode(androidx.compose.ui.test.isRoot())

    @Test fun home_morning() = shoot("01_home_morning") { HomeScreen(state(9, 20), frozenOrb = 0.35f) }

    @Test fun home_afternoon_hearing() = shoot("02_home_afternoon_listening") {
        HomeScreen(state(14, 5, VoiceState(VoicePhase.Hearing, transcript = "what's on tomorrow", level = 0.7f)), frozenOrb = 0.6f)
    }

    @Test fun home_evening_reply() = shoot("03_home_evening_reply") {
        HomeScreen(state(19, 10, VoiceState(VoicePhase.Speaking, reply = "Added Coffee with Jo, tomorrow at 8:30 am.")), frozenOrb = 0.1f)
    }

    @Test fun home_night_dark() = shoot("04_home_night_dark", dark = true) {
        HomeScreen(state(23, 40, VoiceState(VoicePhase.Listening)), frozenOrb = 0.8f)
    }

    @Test fun photos_overlay() = shoot("05_photos_yesterday") {
        val photos = (1..15).map { Photo(Uri.parse("content://sample/$it"), null) }
        HomeScreen(state(16, 0, VoiceState(VoicePhase.Speaking, reply = "15 photos from yesterday."), Overlay.Photos("Yesterday", photos)), frozenOrb = 0.4f)
    }

    @Test fun week_overlay() = shoot("06_this_week") {
        HomeScreen(state(11, 0, VoiceState(VoicePhase.Listening, reply = "9 events across 4 days this week."), Overlay.Agenda("This week", days)), frozenOrb = 0.2f)
    }

    @Test fun asleep() = shoot("07_asleep") {
        HomeScreen(state(15, 30, VoiceState(VoicePhase.Asleep, reply = "Going quiet. Say 2brain to wake me.")), frozenOrb = 0.5f)
    }

    @Test fun welcome() = shoot("00_welcome") { WelcomeScreen(micDenied = false, onContinue = {}, frozenOrb = 0.3f) }

    /** Soft abstract gradients standing in for camera-roll photos. */
    private fun sample(i: Int): Bitmap {
        val palettes = listOf(
            intArrayOf(0xFFF6C9A8.toInt(), 0xFFE2856E.toInt()), intArrayOf(0xFFBFD8E8.toInt(), 0xFF5B8DB8.toInt()),
            intArrayOf(0xFFD9E5C4.toInt(), 0xFF7FA37A.toInt()), intArrayOf(0xFFE9D7F2.toInt(), 0xFF9A7BC4.toInt()),
            intArrayOf(0xFFF3E3B5.toInt(), 0xFFD19A4E.toInt()), intArrayOf(0xFFCFE6E3.toInt(), 0xFF4E9A93.toInt()),
        )
        val (a, b) = palettes[i % palettes.size].let { it[0] to it[1] }
        val bmp = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        cv.drawRect(0f, 0f, 300f, 300f, Paint().apply { shader = LinearGradient(0f, 0f, (i * 37 % 300).toFloat(), 300f, a, b, Shader.TileMode.CLAMP) })
        cv.drawCircle((i * 53 % 300).toFloat(), (i * 97 % 220).toFloat() + 40f, 90f, Paint().apply {
            shader = RadialGradient((i * 53 % 300).toFloat(), (i * 97 % 220).toFloat() + 40f, 90f, 0x66FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
        })
        cv.drawRect(0f, 210f, 300f, 300f, Paint().apply { shader = LinearGradient(0f, 210f, 0f, 300f, 0x00000000, 0x33000000, Shader.TileMode.CLAMP) })
        return bmp
    }
}
