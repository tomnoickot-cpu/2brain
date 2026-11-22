package com.twobrain.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.twobrain.app.R

/** Quiet, paper-like palette. Colour is used sparingly: only the voice orb and event dots. */
@Immutable
data class BrainColors(
    val paper: Color,
    val ink: Color,
    val muted: Color,
    val faint: Color,
    val hairline: Color,
    val card: Color,
    val glowA: Color,
    val glowB: Color,
    val glowC: Color,
    val dark: Boolean,
)

val LightColors = BrainColors(
    paper = Color(0xFFF7F4EF),
    ink = Color(0xFF1C1B1A),
    muted = Color(0xFF8C867E),
    faint = Color(0xFFBDB7AE),
    hairline = Color(0x141C1B1A),
    card = Color(0xFFFFFFFF),
    glowA = Color(0xFFFFB38A), // apricot
    glowB = Color(0xFFB9A6FF), // lavender
    glowC = Color(0xFF8FD3FF), // sky
    dark = false,
)

val DarkColors = BrainColors(
    paper = Color(0xFF131211),
    ink = Color(0xFFF2EFEA),
    muted = Color(0xFF8E887F),
    faint = Color(0xFF55514B),
    hairline = Color(0x1AF2EFEA),
    card = Color(0xFF1C1B19),
    glowA = Color(0xFFFF9466),
    glowB = Color(0xFF8E74FF),
    glowC = Color(0xFF4FB6F5),
    dark = true,
)

@OptIn(ExperimentalTextApi::class)
private fun inter(weight: Int) = Font(
    R.font.inter,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Inter = FontFamily(inter(400), inter(500), inter(600))
val Serif = FontFamily(
    Font(R.font.instrument_serif, FontWeight.Normal),
    Font(R.font.instrument_serif_italic, FontWeight.Normal, FontStyle.Italic),
)

@Immutable
data class BrainType(
    val greeting: TextStyle = TextStyle(fontFamily = Serif, fontSize = 46.sp, lineHeight = 50.sp, letterSpacing = (-0.5).sp),
    val subline: TextStyle = TextStyle(fontFamily = Inter, fontWeight = FontWeight(450), fontSize = 15.sp, lineHeight = 20.sp),
    val label: TextStyle = TextStyle(fontFamily = Inter, fontWeight = FontWeight(600), fontSize = 11.sp, letterSpacing = 1.4.sp),
    val body: TextStyle = TextStyle(fontFamily = Inter, fontWeight = FontWeight(500), fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = (-0.1).sp),
    val small: TextStyle = TextStyle(fontFamily = Inter, fontWeight = FontWeight(400), fontSize = 13.sp, lineHeight = 18.sp),
    val time: TextStyle = TextStyle(fontFamily = Inter, fontWeight = FontWeight(500), fontSize = 13.sp, lineHeight = 22.sp),
    val quote: TextStyle = TextStyle(fontFamily = Serif, fontStyle = FontStyle.Italic, fontSize = 22.sp, lineHeight = 28.sp),
)

val LocalBrainColors = staticCompositionLocalOf { LightColors }
val LocalBrainType = staticCompositionLocalOf { BrainType() }

object Brain {
    val colors: BrainColors @Composable get() = LocalBrainColors.current
    val type: BrainType @Composable get() = LocalBrainType.current
}

@Composable
fun TwoBrainTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalBrainColors provides if (dark) DarkColors else LightColors,
        LocalBrainType provides BrainType(),
        content = content,
    )
}
