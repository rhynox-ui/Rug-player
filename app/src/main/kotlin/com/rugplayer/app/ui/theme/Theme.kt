package com.rugplayer.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColors = darkColorScheme(
    primary = RugAccent,
    onPrimary = RugInk,
    secondary = RugAccentHigh,
    background = RugInk,
    onBackground = RugPaper,
    surface = RugSurface,
    onSurface = RugPaper,
    surfaceVariant = RugSurfaceHigh,
    onSurfaceVariant = RugMuted,
)

private val LightColors = lightColorScheme(
    primary = RugAccent,
    onPrimary = RugPaper,
    secondary = RugAccentHigh,
    background = RugLightBg,
    onBackground = RugInk,
    surface = RugLightSurface,
    onSurface = RugInk,
    surfaceVariant = Color(0xFFF3E2DC),
    onSurfaceVariant = Color(0xFF6B6560),
)

@Composable
fun RugPlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RugTypography,
        content = content,
    )
}
