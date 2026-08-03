package com.miguel.mentaltrader.core.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * HU-017 (tasks.md 4.4, sub-slice EP-003-d): lógica pura de formateo/signo del resumen mensual --
 * el cálculo real de conteo/% ganadas/R acumulado lo hace Room vía `GROUP BY` en
 * `OperationDao.monthlySummaries` (design.md decisión #2, no en memoria), sin poder testearse en
 * JVM sin Room real (mismo criterio ya aplicado a `pagingSourceOrderedByDateDesc`, diferido a
 * instrumentado). Lo que SÍ es lógica pura y testeable aquí: el signo del R acumulado
 * (Escenario 4) y el formateo del resumen sin errores de cálculo cuando el % ganadas es 0
 * (Escenario 3).
 */
class MonthSummaryTest {

    private fun summary(operationCount: Int = 1, winRatePercent: Int = 0, totalResultInR: Float = 0f) =
        MonthSummary(
            year = 2026,
            month = 8,
            operationCount = operationCount,
            winRatePercent = winRatePercent,
            totalResultInR = totalResultInR
        )

    // HU-017 Escenario 4: R acumulado negativo -> signo NEGATIVE (color rojo en la UI).
    @Test
    fun `R acumulado negativo tiene signo NEGATIVE`() {
        assertEquals(ResultSign.NEGATIVE, summary(totalResultInR = -3.2f).resultSign)
    }

    // HU-017 Escenario 1 (implícito): R acumulado positivo -> signo POSITIVE (color verde).
    @Test
    fun `R acumulado positivo tiene signo POSITIVE`() {
        assertEquals(ResultSign.POSITIVE, summary(totalResultInR = 4.5f).resultSign)
    }

    // Edge: R acumulado exactamente cero -> signo NEUTRAL (ni verde ni rojo), sin excepción.
    @Test
    fun `R acumulado en cero tiene signo NEUTRAL`() {
        assertEquals(ResultSign.NEUTRAL, summary(totalResultInR = 0f).resultSign)
    }

    // HU-017 Escenario 3: un mes con operaciones pero 0% ganadas se formatea sin errores de
    // cálculo (sin división por cero -- Room garantiza COUNT(*) >= 1 por grupo).
    @Test
    fun `mes sin operaciones ganadas formatea 0 por ciento sin error`() {
        val text = MonthSummaryFormatting.summaryText(
            summary(operationCount = 5, winRatePercent = 0, totalResultInR = -3.2f)
        )
        assertEquals("5 op · 0% ganadas · -3.2R", text)
    }

    @Test
    fun `formatea el resumen tipico con porcentaje y R positivo`() {
        val text = MonthSummaryFormatting.summaryText(
            summary(operationCount = 3, winRatePercent = 67, totalResultInR = 4.5f)
        )
        assertEquals("3 op · 67% ganadas · 4.5R", text)
    }
}
