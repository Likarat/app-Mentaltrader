package com.miguel.mentaltrader.feature.historial

import com.miguel.mentaltrader.core.data.Operation
import com.miguel.mentaltrader.core.model.Direction
import com.miguel.mentaltrader.core.model.ResultType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * HU-017 Escenario 2 (tasks.md 4.3/4.4, sub-slice EP-003-d): lógica pura de colapsar/expandir un
 * grupo de mes -- sin Room ni Compose, para que funcione de forma correcta e independiente de la
 * virtualización de LazyColumn (un item puede componerse aislado, sin haber "visto" antes su
 * encabezado de mes; por eso [HistorialListItem.monthOf] se calcula por item, no por posición).
 */
class HistorialGroupCollapseTest {

    private val agosto = YearMonth.of(2026, 8)
    private val julio = YearMonth.of(2026, 7)

    private fun operacionEn(date: LocalDate) = Operation(
        id = 1L,
        dateTime = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        assetId = 1L,
        direction = Direction.BUY,
        quality = 8f,
        emotionBeforeId = 1L,
        emotionAfterId = 1L,
        errorId = 1L,
        result = ResultType.WIN,
        entryDescription = "test",
        createdAt = 0L,
        updatedAt = 0L
    )

    // El mes de un OperationRow se calcula a partir de su dateTime real.
    @Test
    fun `monthOf de una operacion es el mes real de su fecha`() {
        val row = HistorialListItem.OperationRow(operacionEn(LocalDate.of(2026, 8, 15)))
        assertTrue(row.monthOf() == agosto)
    }

    // El mes de un encabezado de día (month=null) se calcula a partir de su propio día.
    @Test
    fun `monthOf de un encabezado de dia es el mes de ese dia`() {
        val header = HistorialListItem.GroupHeader(month = null, day = LocalDate.of(2026, 8, 3))
        assertTrue(header.monthOf() == agosto)
    }

    // HU-017 Escenario 2: el encabezado de mes SIEMPRE es visible, incluso colapsado (para poder
    // volver a expandirlo tocándolo).
    @Test
    fun `el encabezado de mes sigue visible aunque su mes este colapsado`() {
        val header = HistorialListItem.GroupHeader(month = agosto, day = LocalDate.of(2026, 8, 15))
        assertTrue(HistorialGroupCollapse.isVisible(header, collapsedMonths = setOf(agosto)))
    }

    // HU-017 Escenario 2: un encabezado de día dentro de un mes colapsado se oculta.
    @Test
    fun `un encabezado de dia de un mes colapsado se oculta`() {
        val header = HistorialListItem.GroupHeader(month = null, day = LocalDate.of(2026, 8, 20))
        assertFalse(HistorialGroupCollapse.isVisible(header, collapsedMonths = setOf(agosto)))
    }

    // HU-017 Escenario 2: una operación de un mes colapsado se oculta.
    @Test
    fun `una operacion de un mes colapsado se oculta`() {
        val row = HistorialListItem.OperationRow(operacionEn(LocalDate.of(2026, 8, 15)))
        assertFalse(HistorialGroupCollapse.isVisible(row, collapsedMonths = setOf(agosto)))
    }

    // HU-017 Escenario 2: colapsar un mes NO afecta la visibilidad de otro mes.
    @Test
    fun `colapsar un mes no afecta la visibilidad de operaciones de otro mes`() {
        val rowJulio = HistorialListItem.OperationRow(operacionEn(LocalDate.of(2026, 7, 10)))
        assertTrue(HistorialGroupCollapse.isVisible(rowJulio, collapsedMonths = setOf(agosto)))
        assertFalse(julio in setOf(agosto))
    }

    // Sin ningún mes colapsado, todo es visible (comportamiento por defecto, expandido).
    @Test
    fun `sin meses colapsados todo es visible`() {
        val row = HistorialListItem.OperationRow(operacionEn(LocalDate.of(2026, 8, 15)))
        val header = HistorialListItem.GroupHeader(month = null, day = LocalDate.of(2026, 8, 20))
        assertTrue(HistorialGroupCollapse.isVisible(row, collapsedMonths = emptySet()))
        assertTrue(HistorialGroupCollapse.isVisible(header, collapsedMonths = emptySet()))
    }
}
