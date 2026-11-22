package com.twobrain.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.twobrain.app.Overlay
import com.twobrain.app.VoiceState
import com.twobrain.app.data.Photo
import com.twobrain.app.ui.theme.Brain
import java.time.LocalDate

/** Lets screenshot tests supply images without touching MediaStore. */
val LocalPhotoPainter = compositionLocalOf<((Photo) -> Painter)?> { null }

@Composable
fun OverlaySheet(overlay: Overlay, voice: VoiceState, frozenOrb: Float? = null) {
    val c = Brain.colors; val t = Brain.type
    Box(Modifier.fillMaxSize().background(c.paper)) {
        Wash()
        val header: @Composable () -> Unit = {
            Column(Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(top = 36.dp, bottom = 22.dp)) {
                Text("Say “close” to go back", style = t.subline, color = c.muted)
                Spacer(Modifier.height(14.dp))
                Text(
                    when (overlay) { is Overlay.Photos -> overlay.title; is Overlay.Agenda -> overlay.title },
                    style = t.greeting, color = c.ink,
                )
                if (overlay is Overlay.Photos) Text("${overlay.photos.size} photos", style = t.subline, color = c.muted)
            }
        }
        when (overlay) {
            is Overlay.Photos -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 220.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item(span = { GridItemSpan(3) }) { Box(Modifier.padding(horizontal = 8.dp)) { header() } }
                items(overlay.photos, key = { it.uri.toString() }) { PhotoTile(it) }
            }
            is Overlay.Agenda -> LazyColumn(contentPadding = PaddingValues(start = 28.dp, end = 28.dp, bottom = 220.dp)) {
                item { header() }
                val today = LocalDate.now()
                val days = overlay.days.filter { it.events.isNotEmpty() }
                if (days.isEmpty()) item { Quiet("Nothing planned.") }
                items(days, key = { it.date.toString() }) { d ->
                    DayCard(dayTitle(d.date, today), d, java.time.LocalDateTime.now(), "")
                    Spacer(Modifier.height(14.dp))
                }
            }
        }
        VoiceDock(voice, Modifier.align(Alignment.BottomCenter), frozenOrb)
    }
}

@Composable
private fun PhotoTile(photo: Photo) {
    val shape = RoundedCornerShape(14.dp)
    val mod = Modifier.aspectRatio(1f).clip(shape).background(Brain.colors.hairline)
    val override = LocalPhotoPainter.current
    if (override != null) {
        Image(override(photo), null, mod, contentScale = ContentScale.Crop)
    } else {
        AsyncImage(
            model = photo.uri, contentDescription = null, modifier = mod,
            contentScale = ContentScale.Crop, placeholder = ColorPainter(Brain.colors.hairline),
        )
    }
}
