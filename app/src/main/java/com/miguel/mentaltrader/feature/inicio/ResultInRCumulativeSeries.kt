package com.miguel.mentaltrader.feature.inicio

import com.miguel.mentaltrader.core.data.OperationDateResult

/**
 * HU-026 Escenario 2: transforma la lista de `dateTime`+`resultInR` (YA ordenada cronológicamente
 * ascendente por `OperationDao.resultInROrderedByDateAsc`) en los puntos punto-a-punto de la
 * gráfica de línea de R acumulado. Post-procesamiento en Kotlin puro (no SQL/función de ventana) --
 * ver KDoc de `OperationDao.resultInROrderedByDateAsc` para el porqué (compatibilidad de SQLite en
 * minSdk 27). Función pura, sin Room ni Compose, testeada aparte en JVM (`ResultInRCumulativeSeriesTest`).
 *
 * HU-026 Escenario 4: un valor `null` (operación sin R cargado todavía) suma 0 al acumulado sin
 * romper la serie -- cálculo seguro, sin errores ni caídas.
 *
 * HU-037: cada [Point] carga también `dateTime` (antes solo `index`+`cumulativeResultInR`) -- el
 * tooltip de arrastre de `RAcumuladoLineChart` lo necesita para mostrar la fecha del punto más
 * cercano al gesto.
 */
object ResultInRCumulativeSeries {

    data class Point(val index: Int, val dateTime: Long, val cumulativeResultInR: Float)

    fun build(resultsOrderedByDateAsc: List<OperationDateResult>): List<Point> {
        var running = 0f
        return resultsOrderedByDateAsc.mapIndexed { index, result ->
            running += result.resultInR ?: 0f
            Point(index, result.dateTime, running)
        }
    }
}
