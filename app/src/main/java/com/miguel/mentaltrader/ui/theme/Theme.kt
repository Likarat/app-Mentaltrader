package com.miguel.mentaltrader.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

val MentaltraderDarkColorScheme = darkColorScheme(
    primary = AccentPrimary,
    secondary = AccentSecondary,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = DarkOnBackground,
    onSurface = DarkOnSurface
)

/**
 * Tema oscuro fijo de la app. Sin parámetro de tema claro ni color dinámico:
 * el modo claro está fuera de alcance en v1 (PRD, Non-goals) y el color dinámico
 * (Material You) rompería la consistencia de acentos entre pantallas.
 */
@Composable
fun MentaltraderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MentaltraderDarkColorScheme,
        typography = Typography,
        content = content
    )
}
