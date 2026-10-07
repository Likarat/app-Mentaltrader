package com.miguel.mentaltrader.feature.inicio

/**
 * HU-038 (nuevo slice, post-construccion): modo de visualización del resumen de Inicio -- alterna
 * las tarjetas de resumen (`MetricsSummaryCards`) y la gráfica de barras de distribución
 * (`ResultDistributionBarChart`) entre porcentaje y conteo absoluto de operaciones por categoría.
 * "Riesgo promedio" (`MetricsSummaryCards`) ignora este modo: siempre se muestra en porcentaje
 * (HU-038 Escenario 4), no tiene equivalente en conteo.
 */
enum class ResumenDisplayMode {
    PORCENTAJE,
    NUMERO;

    fun toggled(): ResumenDisplayMode = when (this) {
        PORCENTAJE -> NUMERO
        NUMERO -> PORCENTAJE
    }
}
