package com.arproperty.android.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = GumiBlue,
    secondary = GumiAccent,
    surface = GumiSurface,
)

private val DarkColors = darkColorScheme(
    primary = GumiBlueLight,
    secondary = GumiAccent,
)

@Composable
fun ARPropertyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = ARPropertyTypography,
        content = content,
    )
}
