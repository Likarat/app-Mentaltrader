package com.miguel.mentaltrader.feature.inicio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * HU-026 Escenario 2 (tasks.md, sub-slice EP-004-a): transforma la lista de `resultInR` (ya
 * ordenada cronológicamente ascendente por `OperationDao.resultInROrderedByDateAsc`) en los
 * puntos punto-a-punto de la gráfica de línea de R acumulado. Lógica pura en Kotlin (sin Room,
 * sin Compose) -- ver KDoc de `ResultInRCumulativeSeries` y de `OperationDao.resultInROrderedByDateAsc`
 * para el porqué de este post-procesamiento fuera de SQL.
 */
class ResultInRCumulativeSeriesTest {

    @Test
    fun `sin operaciones la serie queda vacia`() {
        val series = ResultInRCumulativeSeries.build(emptyList())

        assertTrue(series.isEmpty())
    }

    @Test
    fun `una sola operacion produce un unico punto con su propio valor`() {
        val series = ResultInRCumulativeSeries.build(listOf(2.5f))

        assertEquals(1, series.size)
        assertEquals(0, series[0].index)
        assertEquals(2.5f, series[0].cumulativeResultInR)
    }

    @Test
    fun `varias operaciones acumulan el R punto a punto en orden`() {
        val series = ResultInRCumulativeSeries.build(listOf(1f, -0.5f, 2f))

        assertEquals(listOf(1f, 0.5f, 2.5f), series.map { it.cumulativeResultInR })
        assertEquals(listOf(0, 1, 2), series.map { it.index })
    }

    // HU-026 Escenario 4: cálculo seguro -- una operación sin R cargado (null) no rompe la serie,
    // suma 0 y el acumulado sigue desde el valor previo.
    @Test
    fun `un valor nulo de resultInR suma cero sin romper la serie`() {
        val series = ResultInRCumulativeSeries.build(listOf(1f, null, 3f))

        assertEquals(listOf(1f, 1f, 4f), series.map { it.cumulativeResultInR })
    }

    @Test
    fun `todos los valores nulos produce una serie plana en cero`() {
        val series = ResultInRCumulativeSeries.build(listOf(null, null))

        assertEquals(listOf(0f, 0f), series.map { it.cumulativeResultInR })
    }

    @Test
    fun `el R acumulado puede terminar en negativo si las perdidas superan las ganancias`() {
        val series = ResultInRCumulativeSeries.build(listOf(1f, -3f))

        assertEquals(listOf(1f, -2f), series.map { it.cumulativeResultInR })
    }
}
