package com.rzzisan.carrental.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val AppColorScheme = lightColorScheme(
    primary          = Primary,
    onPrimary        = androidx.compose.ui.graphics.Color.White,
    primaryContainer = PrimaryLight,
    secondary        = Secondary,
    background       = Background,
    surface          = Surface,
    onBackground     = Ink,
    onSurface        = Ink,
)

@Composable
fun CarRentalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        content = content
    )
}
