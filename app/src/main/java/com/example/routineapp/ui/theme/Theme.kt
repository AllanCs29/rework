package com.example.routineapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Olive = Color(0xFF6B7D57)
private val OliveDark = Color(0xFF546346)

private val DarkColors = darkColorScheme(
    primary = Olive,
    secondary = Olive,
    tertiary = OliveDark
)
private val LightColors = lightColorScheme(
    primary = OliveDark,
    secondary = OliveDark,
    tertiary = Olive
)

@Composable
fun RoutineTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
