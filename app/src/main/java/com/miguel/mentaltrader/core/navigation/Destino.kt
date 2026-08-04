package com.miguel.mentaltrader.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Destino(val route: String, val label: String, val icon: ImageVector) {
    data object Inicio : Destino("inicio", "Inicio", Icons.Filled.Home)
    data object Historial : Destino("historial", "Historial", Icons.AutoMirrored.Filled.List)
    // Bug real reportado por el usuario (2026-08-04): usaba Icons.Filled.Settings, el mismo
    // ícono que Ajustes (MainActivity.kt, acción del TopAppBar) -- confundía las dos pantallas.
    data object Etiquetas : Destino("etiquetas", "Etiquetas", Icons.AutoMirrored.Filled.Label)

    companion object {
        // `by lazy`: evita un problema de orden de inicialización estática entre el companion
        // object y los `data object` hermanos declarados en esta misma sealed class (referenciar
        // `Inicio` eagerly aquí puede resolver null si el JVM aún no terminó de inicializarlo).
        val items: List<Destino> by lazy { listOf(Inicio, Historial, Etiquetas) }
    }
}
