package com.pareto.timetracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Nötr (gri tonlu) şema: renk sadece kategori renkleri ve Yatırım/Tüketim göstergelerinde olsun.
 * Dinamik renk kapalı; sistem açık/koyu temasını takip eder.
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF2B2B2B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6E6E6),
    onPrimaryContainer = Color(0xFF1A1A1A),
    secondary = Color(0xFF5E5E5E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6E6E6),
    onSecondaryContainer = Color(0xFF1A1A1A),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1A1A1A),
    surface = Color(0xFFFAFAFA),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFEDEDED),
    onSurfaceVariant = Color(0xFF5A5A5A),
    outline = Color(0xFF8A8A8A),
    outlineVariant = Color(0xFFD6D6D6),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF4F4F4),
    surfaceContainer = Color(0xFFEFEFEF),
    surfaceContainerHigh = Color(0xFFE9E9E9),
    surfaceContainerHighest = Color(0xFFE3E3E3),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE6E6E6),
    onPrimary = Color(0xFF1A1A1A),
    primaryContainer = Color(0xFF3A3A3A),
    onPrimaryContainer = Color(0xFFEDEDED),
    secondary = Color(0xFFBDBDBD),
    onSecondary = Color(0xFF1A1A1A),
    secondaryContainer = Color(0xFF3A3A3A),
    onSecondaryContainer = Color(0xFFEDEDED),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE6E6E6),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFE6E6E6),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFB5B5B5),
    outline = Color(0xFF7A7A7A),
    outlineVariant = Color(0xFF3F3F3F),
    surfaceContainerLowest = Color(0xFF0D0D0D),
    surfaceContainerLow = Color(0xFF1A1A1A),
    surfaceContainer = Color(0xFF1E1E1E),
    surfaceContainerHigh = Color(0xFF262626),
    surfaceContainerHighest = Color(0xFF2E2E2E),
)

@Composable
fun ParetoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
