package com.miguel.mentaltrader.feature.inicio

/**
 * HU-026 Escenario 2: transforma la lista de `resultInR` (YA ordenada cronológicamente ascendente
 * por [com.miguel.mentaltrader.core.data.OperationDao.resultInROrderedByDateAsc]) en los puntos
 * punto-a-punto de la gráfica de línea de R acumulado. Post-procesamiento en Kotlin puro (no
 * SQL/función de ventana) -- ver KDoc de `OperationDao.resultInROrderedByDateAsc` para el porqué
 * (compatibilidad de SQLite en minSdk 27). Función pura, sin Room ni Compose, testeada aparte en
 * JVM (`ResultInRCumulativeSeriesTest`).
 *
 * HU-026 Escenario 4: un valor `null` (operación sin R cargado todavía) suma 0 al acumulado sin
 * romper la serie -- cálculo seguro, sin errores ni caídas.
 */
object ResultInRCumulativeSeries {

    data class Point(val index: Int, val cumulativeResultInR: Float)

    fun build(resultInRValuesOrderedByDateAsc: List<Float?>): List<Point> {
        var running = 0f
        return resultInRValuesOrderedByDateAsc.mapIndexed { index, value ->
            running += value ?: 0f
            Point(index, running)
        }
    }
}
