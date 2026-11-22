package com.twobrain.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import com.twobrain.app.R
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.twobrain.app.VoicePhase
import com.twobrain.app.ui.theme.Brain

/**
 * Shown once. Android only lets permissions be granted by tapping its own dialogs,
 * so this is the single screen in 2brain with a button.
 */
@Composable
fun WelcomeScreen(micDenied: Boolean, onContinue: () -> Unit, frozenOrb: Float? = null) {
    val c = Brain.colors; val t = Brain.type
    Box(Modifier.fillMaxSize().background(c.paper)) {
        Wash()
        Column(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 28.dp)
        ) {
            Spacer(Modifier.height(56.dp))
            Icon(painterResource(R.drawable.ic_logo_mark), contentDescription = "2brain", tint = c.ink, modifier = Modifier.height(30.dp))
            Spacer(Modifier.height(36.dp))
            Text("Hey.", style = t.greeting, color = c.ink)
            Text("I'm 2brain.", style = t.greeting, color = c.muted)
            Spacer(Modifier.height(28.dp))
            Text(
                "You talk, I handle the rest. To do that I need to hear you, and see a little of your phone.",
                style = t.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight(400)), color = c.ink,
            )
            Spacer(Modifier.height(30.dp))
            listOf(
                "Microphone" to "so you never have to touch the screen",
                "Calendar" to "to know your days and add plans",
                "Location" to "to tell you where you are, and get you places",
                "Photos" to "to pull up pictures when you ask",
            ).forEach { (a, b) ->
                Row(Modifier.padding(vertical = 9.dp)) {
                    Text(a, style = t.body, color = c.ink, modifier = Modifier.width(118.dp))
                    Text(b, style = t.small.copy(lineHeight = t.body.lineHeight), color = c.muted)
                }
            }
            Spacer(Modifier.weight(1f))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                VoiceOrb(if (micDenied) VoicePhase.Unavailable else VoicePhase.Listening, 0f, size = 54.dp, frozenTime = frozenOrb)
            }
            if (micDenied) {
                Text(
                    "2brain is voice-only, so the microphone is the one thing it can't do without.",
                    style = t.small, color = c.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                )
            }
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(c.ink).clickable(onClick = onContinue)
                    .padding(vertical = 17.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (micDenied) "Open settings" else "Let's begin", style = t.body, color = c.paper)
            }
            Spacer(Modifier.height(22.dp))
        }
    }
}
