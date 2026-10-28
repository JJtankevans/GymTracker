package com.vitoroliveira.gymtracker.ios

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Lime = Color(0xFFC6FF00)
val Ink = Color(0xFF0B0D0B)
val Card = Color(0xFF171A17)
val Muted = Color(0xFF9AA09A)
val Gold = Color(0xFFFFC53D)

private val Scheme = darkColorScheme(
    primary = Lime, onPrimary = Color.Black,
    secondary = Color(0xFF22E3FF), onSecondary = Color.Black,
    tertiary = Gold,
    background = Ink, onBackground = Color.White,
    surface = Ink, onSurface = Color.White,
    surfaceVariant = Card, onSurfaceVariant = Muted,
    surfaceContainer = Card, surfaceContainerHigh = Card, surfaceContainerHighest = Color(0xFF1F231F),
    error = Color(0xFFFF6B6B)
)

/** Tema do iPhone: o "Carbon" escuro com acento limão (o mesmo do relógio). Os 40 temas do Android vêm depois. */
@Composable
fun GymTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = Scheme, content = content)
