package com.miguel.mentaltrader.feature.historial

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * HU-022 Escenario 2 (tasks.md 5.5, sub-slice EP-003-e): resuelve el rango [desde, hasta] de un
 * periodo predefinido ("Última semana", "Último mes", "Este mes", "Últimos 3 meses",
 * "Personalizado" -- notas técnicas de HU-022) de forma PURA y determinista, recibiendo "hoy"
 * como parámetro explícito (no `LocalDate.now()`) para poder testear sin depender del reloj real.
 * "Última semana"/"Último mes"/"Últimos 3 meses" son ventanas rodantes de días (no meses de
 * calendario, ver nota técnica explícita de HU-022: "'Última semana' es un filtro (últimos 7
 * días)"); "Este mes" sí es el mes de calendario actual, consistente con su propio nombre.
 * "Personalizado" no tiene rango propio -- lo aporta el usuario en HistorialFilterState.
 */
class PeriodoFiltroRangeTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val hoy = LocalDate.of(2026, 8, 15)

    private fun startOfDay(date: LocalDate) = date.atStartOfDay(zone).toInstant().toEpochMilli()
    private fun endOfDay(date: LocalDate) = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

    @Test
    fun `ultima semana son los ultimos 7 dias incluyendo hoy`() {
        val rango = PeriodoFiltroRange.rangeFor(PeriodoFiltro.ULTIMA_SEMANA, hoy, zone)

        assertEquals(startOfDay(hoy.minusDays(6)) to endOfDay(hoy), rango)
    }

    @Test
    fun `ultimo mes son los ultimos 30 dias incluyendo hoy`() {
        val rango = PeriodoFiltroRange.rangeFor(PeriodoFiltro.ULTIMO_MES, hoy, zone)

        assertEquals(startOfDay(hoy.minusDays(29)) to endOfDay(hoy), rango)
    }

    @Test
    fun `este mes va desde el primer dia del mes de calendario actual hasta hoy`() {
        val rango = PeriodoFiltroRange.rangeFor(PeriodoFiltro.ESTE_MES, hoy, zone)

        assertEquals(startOfDay(hoy.withDayOfMonth(1)) to endOfDay(hoy), rango)
    }

    @Test
    fun `ultimos 3 meses son los ultimos 90 dias incluyendo hoy`() {
        val rango = PeriodoFiltroRange.rangeFor(PeriodoFiltro.ULTIMOS_3_MESES, hoy, zone)

        assertEquals(startOfDay(hoy.minusDays(89)) to endOfDay(hoy), rango)
    }

    @Test
    fun `personalizado no tiene rango propio, lo aporta el usuario`() {
        assertNull(PeriodoFiltroRange.rangeFor(PeriodoFiltro.PERSONALIZADO, hoy, zone))
    }
}
