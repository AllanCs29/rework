package com.example.routineapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val DarkScheme = darkColorScheme(
    primary = Olive,
    secondary = OliveDark,
    surface = SurfaceDark,
)

@Composable
fun RoutineTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else lightColorScheme(),
        typography = Typography(),
        shapes = Shapes(),
        content = content
    )
}