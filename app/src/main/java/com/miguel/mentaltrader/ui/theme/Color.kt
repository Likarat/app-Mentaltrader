package com.miguel.mentaltrader.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta "Morado medianoche + turquesa": fondo con mezcla morado-negro (varias superficies
// elevadas, no un solo azul/negro plano) y acentos turquesa/lavanda. Sin variante clara en v1.
//
// Bug real reportado por el usuario (2026-08-04): el fondo azulado anterior (MidnightBackground
// 0xFF0C1220 y el resto de la escala) se percibía "muy plano". Se desplaza el matiz de toda la
// escala de fondo/superficies de azul hacia morado (más rojo relativo al verde, manteniendo el
// azul como componente dominante) preservando la misma progresión de luminosidad -- sigue siendo
// un tema oscuro real, solo con más profundidad de matiz.

// Fondo y superficies (de más profunda a más elevada).
val MidnightBackground = Color(0xFF120E24)
val MidnightSurface = Color(0xFF1A1533)
val MidnightSurfaceContainerLowest = Color(0xFF0F0C1E)
val MidnightSurfaceContainerLow = Color(0xFF201A3C)
val MidnightSurfaceContainer = Color(0xFF281F49)
val MidnightSurfaceContainerHigh = Color(0xFF332856)
val MidnightSurfaceContainerHighest = Color(0xFF3E3268)
val MidnightOutline = Color(0xFF4F4478)
val MidnightOutlineVariant = Color(0xFF362C54)

// Acento primario: turquesa (evoluciona el acento previo, más profundidad de contenedor).
val TurquoiseAccent = Color(0xFF4FD8C8)
val OnTurquoiseAccent = Color(0xFF00332C)
val TurquoiseAccentContainer = Color(0xFF1E4A45)
val OnTurquoiseAccentContainer = Color(0xFFB8F5EA)

// Acento secundario: lavanda, para elementos de apoyo (chips no seleccionados, iconografía).
val LavenderAccent = Color(0xFFC0AEEB)
val OnLavenderAccent = Color(0xFF29204A)
val LavenderAccentContainer = Color(0xFF3B3163)
val OnLavenderAccentContainer = Color(0xFFE7DFFF)

// Semántico: resultado de operación (WIN/LOSS) y error/destructivo.
val WinGreen = Color(0xFF4FDB8C)
val OnWinGreen = Color(0xFF00391C)
val LossCoral = Color(0xFFFF7A6E)
val OnLossCoral = Color(0xFF3A0800)
val ErrorContainer = Color(0xFF4A241F)
val OnErrorContainer = Color(0xFFFFDAD3)

val OnDark = Color(0xFFE7EBF3)
val OnDarkMuted = Color(0xFFA6B0C4)
