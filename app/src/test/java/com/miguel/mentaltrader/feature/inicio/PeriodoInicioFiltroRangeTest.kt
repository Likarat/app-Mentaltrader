package com.miguel.mentaltrader.feature.inicio

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * HU-028 Escenario 1 (tasks.md 2.5, sub-slice EP-004-b): resuelve el rango [desde, hasta] de un
 * [PeriodoInicioFiltro] (Día/Semana/Mes/Últimos 3 meses) de forma PURA y determinista, recibiendo
 * "hoy" explícito (no `LocalDate.now()`) -- mismo patrón de test que
 * `com.miguel.mentaltrader.feature.historial.PeriodoFiltroRangeTest` (EP-003). Semántica de cada
 * periodo documentada en el KDoc de [PeriodoInicioFiltroRange] y en design.md decisión #7.
 */
class PeriodoInicioFiltroRangeTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val hoy = LocalDate.of(2026, 8, 15) // sábado

    private fun startOfDay(date: LocalDate) = date.atStartOfDay(zone).toInstant().toEpochMilli()
    private fun endOfDay(date: LocalDate) = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

    @Test
    fun `dia es el dia de hoy completo`() {
        val rango = PeriodoInicioFiltroRange.rangeFor(PeriodoInicioFiltro.DIA, hoy, zone)

        assertEquals(startOfDay(hoy) to endOfDay(hoy), rango)
    }

    @Test
    fun `semana es la semana calendario actual, de lunes a hoy`() {
        val rango = PeriodoInicioFiltroRange.rangeFor(PeriodoInicioFiltro.SEMANA, hoy, zone)

        assertEquals(startOfDay(hoy.with(DayOfWeek.MONDAY)) to endOfDay(hoy), rango)
        // Verificación independiente (no solo recalculando la misma fórmula): el "desde" resuelto
        // cae realmente en lunes.
        val desde = Instant.ofEpochMilli(rango.first).atZone(zone).toLocalDate()
        assertEquals(DayOfWeek.MONDAY, desde.dayOfWeek)
    }

    @Test
    fun `mes es el mes calendario actual, del dia 1 a hoy`() {
        val rango = PeriodoInicioFiltroRange.rangeFor(PeriodoInicioFiltro.MES, hoy, zone)

        assertEquals(startOfDay(hoy.withDayOfMonth(1)) to endOfDay(hoy), rango)
    }

    @Test
    fun `ultimos 3 meses son los ultimos 90 dias incluyendo hoy`() {
        val rango = PeriodoInicioFiltroRange.rangeFor(PeriodoInicioFiltro.ULTIMOS_3_MESES, hoy, zone)

        assertEquals(startOfDay(hoy.minusDays(89)) to endOfDay(hoy), rango)
    }

    @Test
    fun `todos los periodos terminan al final del dia de hoy`() {
        PeriodoInicioFiltro.entries.forEach { periodo ->
            val rango = PeriodoInicioFiltroRange.rangeFor(periodo, hoy, zone)
            assertEquals("periodo=$periodo", endOfDay(hoy), rango.second)
        }
    }
}
