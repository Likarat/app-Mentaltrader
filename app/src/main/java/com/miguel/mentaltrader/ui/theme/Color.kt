package com.miguel.mentaltrader.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta "Pizarra azulada + azul/vinotinto": fondo y superficies en un gris pizarra oscuro de baja
// saturación (look "terminal de trading", elegante y neutro) con DOS acentos -- azul profundo y
// vinotinto -- elegidos por el usuario tras varias rondas de ajuste (comparación visual vía
// artifact, no a ciegas). El dúo esmeralda/ámbar de la ronda anterior no convenció ("no combina
// mucho"). Ver AppBackgroundGradient/CardBaseGradient en Theme.kt para los degradés.

// Fondo y superficies (de más profunda a más elevada) -- gris-pizarra, saturación baja.
val SlateBackground = Color(0xFF11151C)
val SlateSurface = Color(0xFF161B24)
val SlateSurfaceContainerLowest = Color(0xFF0D1016)
val SlateSurfaceContainerLow = Color(0xFF1B212B)
val SlateSurfaceContainer = Color(0xFF212836)
val SlateSurfaceContainerHigh = Color(0xFF2A3242)
val SlateSurfaceContainerHighest = Color(0xFF333D50)
val SlateOutline = Color(0xFF4A5568)
val SlateOutlineVariant = Color(0xFF2E3644)

// Acento primario: azul profundo (relleno de fondo/tarjetas/FAB -- ver DeepBlueAccent en
// AppBackgroundGradient/CardBaseGradient/PrimaryActionGradient, Theme.kt) + una variante clara
// (BrightBlueAccent) para TEXTO/ÍCONOS ("Filtros activos", títulos de sección). Bug real encontrado
// antes de mostrarlo: usar el mismo DeepBlueAccent oscuro como colorScheme.primary hacía
// ILEGIBLE cualquier texto de ese color sobre la propia franja azul del fondo (mismo tono exacto
// detrás y adelante) -- BrightBlueAccent es sensiblemente más claro que el azul del fondo/tarjetas,
// así que el texto se lee sin importar qué haya detrás.
val DeepBlueAccent = Color(0xFF1B3A78)
val OnDeepBlueAccent = Color(0xFFDCE6FF)
val BlueAccentContainer = Color(0xFF14224A)
val OnBlueAccentContainer = Color(0xFFB9D0FF)
val BrightBlueAccent = Color(0xFF7FB0FF)
val OnBrightBlueAccent = Color(0xFF07203F)

// Acento secundario: vinotinto.
val WineAccent = Color(0xFF6B1E2E)
val OnWineAccent = Color(0xFFF7E2E7)
val WineAccentContainer = Color(0xFF3A1420)
val OnWineAccentContainer = Color(0xFFFFD9E0)

// Semántico: resultado de operación (WIN/LOSS) y error/destructivo.
val WinGreen = Color(0xFF4FDB8C)
val OnWinGreen = Color(0xFF00391C)
val LossCoral = Color(0xFFFF7A6E)
val OnLossCoral = Color(0xFF3A0800)
val ErrorContainer = Color(0xFF4A241F)
val OnErrorContainer = Color(0xFFFFDAD3)

val OnDark = Color(0xFFE7EBF3)
val OnDarkMuted = Color(0xFFA6B0C4)

// Degradé PROPIO del menú inferior (NavigationBar, MainActivity) -- brush INDEPENDIENTE del fondo
// general y de las tarjetas: el usuario pidió mantener el menú tal cual quedó en una ronda
// anterior ("el menú me gustó, se mantiene igual"), sin relación con el resto de la paleta, por más
// que el fondo/tarjetas sigan cambiando. Ver BottomNavGradient en Theme.kt.
val DeepTealAccent = Color(0xFF14524A)
val NavyBackground = Color(0xFF0A0E1A)
