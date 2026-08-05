package com.miguel.mentaltrader.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val MentaltraderDarkColorScheme = darkColorScheme(
    // BrightBlueAccent (no DeepBlueAccent) -- colorScheme.primary tiñe TEXTO/ÍCONOS ("Filtros
    // activos", títulos de sección) que a veces caen directo sobre la franja azul del fondo/
    // tarjetas; con el mismo tono oscuro detrás y adelante el texto se volvía ilegible (bug real
    // encontrado antes de mostrarlo). Ver KDoc de BrightBlueAccent en Color.kt.
    primary = BrightBlueAccent,
    onPrimary = OnBrightBlueAccent,
    primaryContainer = BlueAccentContainer,
    onPrimaryContainer = OnBlueAccentContainer,
    secondary = WineAccent,
    onSecondary = OnWineAccent,
    secondaryContainer = WineAccentContainer,
    onSecondaryContainer = OnWineAccentContainer,
    tertiary = WinGreen,
    onTertiary = OnWinGreen,
    error = LossCoral,
    onError = OnLossCoral,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    background = SlateBackground,
    onBackground = OnDark,
    surface = SlateSurface,
    onSurface = OnDark,
    surfaceVariant = SlateSurfaceContainerHigh,
    onSurfaceVariant = OnDarkMuted,
    surfaceContainerLowest = SlateSurfaceContainerLowest,
    surfaceContainerLow = SlateSurfaceContainerLow,
    surfaceContainer = SlateSurfaceContainer,
    surfaceContainerHigh = SlateSurfaceContainerHigh,
    surfaceContainerHighest = SlateSurfaceContainerHighest,
    outline = SlateOutline,
    outlineVariant = SlateOutlineVariant
)

/** Formas más redondeadas que el default de Material3 — parte del pedido de "no tan plano". */
val MentaltraderShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/** Gradiente azul→vinotinto para las acciones primarias (botón "Guardar", FAB) — mismo dúo de
 * acentos que [AppBackgroundGradient]/[CardBaseGradient], evita que los botones se vean como un
 * relleno plano. */
val PrimaryActionGradient = Brush.linearGradient(listOf(DeepBlueAccent, WineAccent))

/** Degradé de fondo GENERAL de las pantallas (detrás del NavHost, MainActivity): azul profundo en
 * la esquina superior, vinotinto en la esquina inferior, pizarra oscura neutra en el medio --
 * elegido por el usuario tras comparar varias combinaciones en un artifact (no a ciegas). Azul y
 * vinotinto nunca son stops vecinos (lección de una ronda anterior: dos acentos consecutivos
 * fuerzan a Compose a mezclarlos directamente en el medio, y esa mezcla suele dar un tono turbio)
 * -- cada uno transiciona a [SlateBackground] por separado. */
val AppBackgroundGradient = Brush.linearGradient(
    0f to DeepBlueAccent,
    0.46f to SlateBackground,
    0.58f to SlateBackground,
    1f to WineAccent
)

/** Degradé BASE de las tarjetas (MetricCard, tarjetas de gráficas en Inicio): mismo dúo azul→vino
 * que el fondo general, diagonal simple (sin el tramo neutro del fondo -- las tarjetas son chicas,
 * el tramo neutro apenas se notaría). Se combina siempre con [CardVignetteOverlay] encima. */
val CardBaseGradient = Brush.linearGradient(listOf(DeepBlueAccent, WineAccent))

/** Viñeta oscura para las tarjetas -- estilo elegido explícitamente por el usuario entre 6
 * tratamientos de degradé distintos comparados en un artifact ("Diagonal + viñeta oscura"): oscurece
 * las esquinas/bordes de la tarjeta sobre [CardBaseGradient], dándole más profundidad y foco al
 * centro en vez de un degradé plano de esquina a esquina. Se aplica como un `Modifier.background`
 * adicional ENCIMA de [CardBaseGradient] (el radial con alpha compone sobre el brush de abajo). */
val CardVignetteOverlay = Brush.radialGradient(
    0f to Color.Transparent,
    0.35f to Color.Transparent,
    1f to Color.Black.copy(alpha = 0.55f)
)

/** Degradé del menú inferior (NavigationBar, MainActivity) -- brush INDEPENDIENTE del de fondo
 * general: el usuario pidió explícitamente mantener este tal cual quedó ("el menú me gusta, me
 * gusta ese degradé") mientras seguía ajustando el fondo general por separado, así que ya no
 * comparten el mismo [Brush] (antes lo hacían, y tocar uno alteraba el otro). Mismos 4 stops que la
 * versión aprobada: destello teal->azul y un tramo final en [NavyBackground] (el menú es corto, ese
 * tramo casi no se ve, a diferencia del fondo general). */
val BottomNavGradient = Brush.linearGradient(
    0f to DeepTealAccent,
    0.22f to DeepBlueAccent,
    0.5f to NavyBackground,
    1f to NavyBackground
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
        shapes = MentaltraderShapes,
        content = content
    )
}
