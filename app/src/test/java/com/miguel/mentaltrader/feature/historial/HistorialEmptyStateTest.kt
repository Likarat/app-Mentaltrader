package com.miguel.mentaltrader.feature.historial

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests unitarios de [HistorialEmptyState] (tasks.md 6.4, sub-slice EP-003-f, HU-025): lógica PURA
 * (sin Room/Paging/Compose) que decide cuál de los DOS estados vacíos mostrar -- o ninguno --, sin
 * depender de `LazyPagingItems` real (no testeable en JVM sin `paging-testing`, mismo criterio ya
 * aplicado en `HistorialSeparators`/`HistorialGroupCollapse`/`HistorialFilterMatcher`).
 */
class HistorialEmptyStateTest {

    // HU-025 Escenario 1 (happy path): nunca hubo ninguna operación registrada -- invita a crear
    // la primera, sin importar si por algún motivo un filtro quedó activo (edge, ver test de abajo).
    @Test
    fun `sin ninguna operacion registrada nunca resuelve PRIMERA_VEZ`() {
        assertEquals(
            HistorialEmptyState.PRIMERA_VEZ,
            HistorialEmptyState.resolve(totalOperationCount = 0, itemCount = 0, filterActive = false)
        )
    }

    // HU-025 Escenario 2: hay operaciones registradas, pero el filtro/búsqueda activo (HU-022/023)
    // no arroja ninguna coincidencia -- mensaje DISTINTO del de "primera vez".
    @Test
    fun `con operaciones registradas pero el filtro activo sin coincidencias resuelve SIN_RESULTADOS_FILTRO`() {
        assertEquals(
            HistorialEmptyState.SIN_RESULTADOS_FILTRO,
            HistorialEmptyState.resolve(totalOperationCount = 5, itemCount = 0, filterActive = true)
        )
    }

    // Con items para mostrar, ningún estado vacío aplica.
    @Test
    fun `con items para mostrar no resuelve ningun estado vacio`() {
        assertNull(HistorialEmptyState.resolve(totalOperationCount = 5, itemCount = 3, filterActive = true))
        assertNull(HistorialEmptyState.resolve(totalOperationCount = 5, itemCount = 3, filterActive = false))
    }

    // Edge real encontrado durante el diseño de este sub-slice (no un escenario explícito de
    // HU-025): sin filtro activo, un listado vacío con operaciones ya registradas solo puede
    // deberse a la ventana de "Deshacer" de HU-021 (pendingDeleteIds oculta la última fila
    // pendiente de borrado de forma optimista en la UI, sin tocar Room todavía) -- un estado
    // transitorio, no el caso real de "sin resultados de filtro" que HU-025 pide comunicar. Se
    // decide NO mostrar ningún mensaje en ese caso (en vez de uno potencialmente engañoso), ya que
    // no hay ningún filtro que invitar a limpiar.
    @Test
    fun `sin filtro activo un listado vacio con operaciones registradas no resuelve ningun estado vacio (edge de pendingDeleteIds)`() {
        assertNull(HistorialEmptyState.resolve(totalOperationCount = 5, itemCount = 0, filterActive = false))
    }

    // Edge de precedencia: si nunca hubo operaciones, PRIMERA_VEZ tiene prioridad aun si por algún
    // motivo quedó un filtro activo (en la práctica no debería poder ocurrir un filtro real sin
    // datos que filtrar, pero la precedencia queda definida sin ambigüedad).
    @Test
    fun `sin ninguna operacion registrada nunca PRIMERA_VEZ tiene precedencia aunque haya un filtro activo`() {
        assertEquals(
            HistorialEmptyState.PRIMERA_VEZ,
            HistorialEmptyState.resolve(totalOperationCount = 0, itemCount = 0, filterActive = true)
        )
    }
}
