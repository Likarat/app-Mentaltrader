package com.miguel.mentaltrader.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

val MentaltraderDarkColorScheme = darkColorScheme(
    primary = TurquoiseAccent,
    onPrimary = OnTurquoiseAccent,
    primaryContainer = TurquoiseAccentContainer,
    onPrimaryContainer = OnTurquoiseAccentContainer,
    secondary = LavenderAccent,
    onSecondary = OnLavenderAccent,
    secondaryContainer = LavenderAccentContainer,
    onSecondaryContainer = OnLavenderAccentContainer,
    tertiary = WinGreen,
    onTertiary = OnWinGreen,
    error = LossCoral,
    onError = OnLossCoral,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    background = MidnightBackground,
    onBackground = OnDark,
    surface = MidnightSurface,
    onSurface = OnDark,
    surfaceVariant = MidnightSurfaceContainerHigh,
    onSurfaceVariant = OnDarkMuted,
    surfaceContainerLowest = MidnightSurfaceContainerLowest,
    surfaceContainerLow = MidnightSurfaceContainerLow,
    surfaceContainer = MidnightSurfaceContainer,
    surfaceContainerHigh = MidnightSurfaceContainerHigh,
    surfaceContainerHighest = MidnightSurfaceContainerHighest,
    outline = MidnightOutline,
    outlineVariant = MidnightOutlineVariant
)

/** Formas más redondeadas que el default de Material3 — parte del pedido de "no tan plano". */
val MentaltraderShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/** Gradiente turquesa→lavanda para las acciones primarias (botón "Guardar", FAB) — evita que los
 * botones se vean como un relleno de color plano. */
val PrimaryActionGradient = Brush.linearGradient(listOf(TurquoiseAccent, LavenderAccent))

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
        shapes = MentaltraderShapes,
        content = content
    )
}
