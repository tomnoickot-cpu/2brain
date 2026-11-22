package com.twobrain.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.twobrain.app.VoicePhase
import com.twobrain.app.ui.theme.Brain
import kotlin.math.cos
import kotlin.math.sin

/**
 * The always-listening orb. Breathes slowly while idle, swells with your voice,
 * spins while thinking, ripples while speaking, and greys out while asleep.
 */
@Composable
fun VoiceOrb(phase: VoicePhase, level: Float, modifier: Modifier = Modifier, size: Dp = 76.dp, frozenTime: Float? = null) {
    val c = Brain.colors
    val t = rememberInfiniteTransition(label = "orb")
    val spinAnim by t.animateFloat(0f, 360f, infiniteRepeatable(tween(if (phase == VoicePhase.Thinking) 1400 else 9000, easing = LinearEasing)), label = "spin")
    val breatheAnim by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breathe")
    val spin = frozenTime?.let { it * 360f } ?: spinAnim
    val breathe = frozenTime ?: breatheAnim

    val lvl by animateFloatAsState(if (phase == VoicePhase.Hearing) level else 0f, spring(stiffness = 300f), label = "lvl")
    val asleep = phase == VoicePhase.Asleep || phase == VoicePhase.Unavailable
    val sat by animateFloatAsState(if (asleep) 0f else 1f, tween(600), label = "sat")

    fun tint(col: Color) = lerpColor(c.faint, col, sat)

    Canvas(modifier.size(size * 2.2f)) {
        val center = Offset(this.size.width / 2, this.size.height / 2)
        val r = size.toPx() / 2
        val swell = 1f + 0.04f * breathe + 0.22f * lvl
        val speaking = phase == VoicePhase.Speaking

        // Soft halo, layered radial gradients (no blur needed, works on every API level).
        val haloR = r * (1.55f + 0.35f * lvl + 0.12f * breathe + if (speaking) 0.15f * breathe else 0f)
        drawCircle(
            Brush.radialGradient(
                listOf(tint(c.glowB).copy(alpha = 0.30f * (0.6f + 0.4f * sat)), Color.Transparent),
                center = center, radius = haloR,
            ), radius = haloR, center = center,
        )
        // Three drifting colour blobs make the orb feel alive.
        val blobs = listOf(c.glowA, c.glowB, c.glowC)
        blobs.forEachIndexed { i, col ->
            val a = Math.toRadians((spin + i * 120f).toDouble())
            val d = r * 0.22f * (1f + lvl)
            val o = Offset(center.x + (cos(a) * d).toFloat(), center.y + (sin(a) * d).toFloat())
            drawCircle(
                Brush.radialGradient(listOf(tint(col).copy(alpha = 0.95f), tint(col).copy(alpha = 0f)), center = o, radius = r * swell * 1.05f),
                radius = r * swell * 1.05f, center = o,
            )
        }
        // Glassy core.
        rotate(spin, center) {
            drawCircle(
                Brush.sweepGradient(listOf(tint(c.glowA), tint(c.glowB), tint(c.glowC), tint(c.glowA)), center),
                radius = r * swell * 0.86f, center = center, alpha = 0.45f,
            )
        }
        // Melt the sweep's seams into a soft centre.
        drawCircle(
            Brush.radialGradient(listOf(tint(c.glowB).copy(alpha = 0.55f), tint(c.glowC).copy(alpha = 0.15f), Color.Transparent), center = center, radius = r * swell * 0.86f),
            radius = r * swell * 0.86f, center = center,
        )
        drawCircle(
            Brush.radialGradient(
                listOf(Color.White.copy(alpha = if (c.dark) 0.35f else 0.65f), Color.White.copy(alpha = 0f)),
                center = Offset(center.x - r * 0.25f, center.y - r * 0.3f), radius = r * 0.75f,
            ),
            radius = r * swell * 0.86f, center = center,
        )
    }
}

private fun lerpColor(a: Color, b: Color, f: Float) = Color(
    red = a.red + (b.red - a.red) * f,
    green = a.green + (b.green - a.green) * f,
    blue = a.blue + (b.blue - a.blue) * f,
    alpha = a.alpha + (b.alpha - a.alpha) * f,
)
