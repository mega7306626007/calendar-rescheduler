package com.emmanuel.calendarrescheduler.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.emmanuel.calendarrescheduler.R

val Gold = Color(0xFFD4A843)
val GoldDim = Color(0xFF967828)
val Violet = Color(0xFF7C3AED)
val HeaderA = Color(0xFF180A32)
val HeaderB = Color(0xFF3E1870)
val DeepPurple = Color(0xFF3C2A64)
val InkOnGold = Color(0xFF141E1E)

@Immutable
data class CalScheme(
    val bg: Color,
    val header: List<Color>,
    val card: Color,
    val muted: Color,
    val text: Color,
    val soft: Color,
    val row: Color,
    val sheet: Color,
    val btn: Color,
    val calBar: Color,
    val tab: Color,
    val line: Color,
    val goldTxt: Color,
    val heat: Color,
)

val DarkCal = CalScheme(
    bg = Color(0xFF12101C),
    header = listOf(HeaderA, HeaderB),
    card = Color(0xFF2C2742),
    muted = Color(0xFF8A93A8),
    text = Color(0xFFF0EDE8),
    soft = Color(0xFFD2D0D8),
    row = Color(0xFF241E3A),
    sheet = Color(0xFF1E1B2E),
    btn = Color(0xFF28243C),
    calBar = Color(0xFF161324),
    tab = Color(0xFF181228),
    line = Color(0xFF242036),
    goldTxt = Gold,
    heat = Color(0xFF1F5F1F),
)

val LightCal = CalScheme(
    bg = Color(0xFFF3F0F8),
    header = listOf(HeaderA, HeaderB),
    card = Color(0xFFFFFFFF),
    muted = Color(0xFF76718A),
    text = Color(0xFF1C1928),
    soft = Color(0xFF46425A),
    row = Color(0xFFE8E3F3),
    sheet = Color(0xFFFFFFFF),
    btn = Color(0xFFE2DCEE),
    calBar = Color(0xFFECE8F5),
    tab = Color(0xFFEEEAF6),
    line = Color(0xFFD0CBE0),
    goldTxt = Color(0xFF966914),
    heat = Color(0xFFC4ECC2),
)

val Playfair: FontFamily
    @Composable get() = FontFamily(Font(R.font.playfair))
val Inter: FontFamily
    @Composable get() = FontFamily(Font(R.font.inter))

@Composable
fun CalTheme(dark: Boolean, content: @Composable () -> Unit) {
    val scheme = if (dark) DarkCal else LightCal
    val cs = if (dark) {
        darkColorScheme(
            primary = Gold, onPrimary = Color(0xFF141E1E),
            secondary = Violet, background = scheme.bg, surface = scheme.card,
            onBackground = scheme.text, onSurface = scheme.text,
        )
    } else {
        lightColorScheme(
            primary = Violet, onPrimary = Color.White,
            secondary = GoldDim, background = scheme.bg, surface = scheme.card,
            onBackground = scheme.text, onSurface = scheme.text,
        )
    }
    MaterialTheme(colorScheme = cs, content = content)
}
