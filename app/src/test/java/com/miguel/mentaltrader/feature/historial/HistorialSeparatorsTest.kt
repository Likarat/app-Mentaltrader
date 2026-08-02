package com.miguel.mentaltrader.feature.historial

import com.miguel.mentaltrader.core.data.Operation
import com.miguel.mentaltrader.core.model.Direction
import com.miguel.mentaltrader.core.model.ResultType
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests unitarios de HistorialSeparators (tasks.md 1.3/1.7, sub-slice EP-003-a, HU-015):
 * lógica PURA (sin Room/Paging) que decide qué separador de agrupación (mes/día) insertar entre
 * dos operaciones adyacentes ya ordenadas por fecha descendente. Se extrae como función pura
 * (en vez de resolverse dentro de un `PagingData.insertSeparators` real) para poder testearla en
 * JVM sin necesitar la dependencia `androidx.paging:paging-testing` (no está en
 * stack-allowlist.json) ni Room.
 */
class HistorialSeparatorsTest {

    private fun operacionEn(fechaHora: java.time.LocalDateTime, id: Long = 1L) = Operation(
        id = id,
        dateTime = fechaHora.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        assetId = 1L,
        direction = Direction.BUY,
        quality = 5f,
        emotionBeforeId = 1L,
        emotionAfterId = 1L,
        errorId = 1L,
        result = ResultType.WIN,
        entryDescription = "test",
        createdAt = 0L,
        updatedAt = 0L
    )

    // HU-015 Escenario 1: la primera operación del listado completo (before=null) siempre trae
    // encabezado de mes Y de día (es el inicio de la primera sección).
    @Test
    fun `la primera operacion del listado trae encabezado de mes y dia`() {
        val primera = operacionEn(java.time.LocalDateTime.of(2026, 7, 15, 10, 30))

        val separador = HistorialSeparators.between(before = null, after = primera)

        assertEquals(YearMonth.of(2026, 7), separador?.month)
        assertEquals(LocalDate.of(2026, 7, 15), separador?.day)
    }

    // HU-015 Escenario 2: dos operaciones del mismo día -> sin separador entre ellas.
    @Test
    fun `dos operaciones del mismo dia no llevan separador`() {
        val antes = operacionEn(java.time.LocalDateTime.of(2026, 7, 15, 18, 0))
        val despues = operacionEn(java.time.LocalDateTime.of(2026, 7, 15, 9, 0))

        val separador = HistorialSeparators.between(before = antes, after = despues)

        assertNull(separador)
    }

    // HU-015 Escenario 2: cambia el día pero sigue en el mismo mes -> solo encabezado de día.
    @Test
    fun `cambio de dia dentro del mismo mes solo trae encabezado de dia`() {
        val antes = operacionEn(java.time.LocalDateTime.of(2026, 7, 16, 8, 0))
        val despues = operacionEn(java.time.LocalDateTime.of(2026, 7, 15, 20, 0))

        val separador = HistorialSeparators.between(before = antes, after = despues)

        assertNull(separador?.month)
        assertEquals(LocalDate.of(2026, 7, 15), separador?.day)
    }

    // HU-015 Escenario 1: cambia el mes -> encabezado de mes Y de día (nueva sección completa).
    @Test
    fun `cambio de mes trae encabezado de mes y de dia`() {
        val antes = operacionEn(java.time.LocalDateTime.of(2026, 7, 1, 8, 0))
        val despues = operacionEn(java.time.LocalDateTime.of(2026, 6, 30, 20, 0))

        val separador = HistorialSeparators.between(before = antes, after = despues)

        assertEquals(YearMonth.of(2026, 6), separador?.month)
        assertEquals(LocalDate.of(2026, 6, 30), separador?.day)
    }

    @Test
    fun `sin operacion siguiente no hay separador`() {
        val antes = operacionEn(java.time.LocalDateTime.of(2026, 7, 1, 8, 0))

        val separador = HistorialSeparators.between(before = antes, after = null)

        assertNull(separador)
    }
}
