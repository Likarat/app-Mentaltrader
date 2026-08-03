package com.miguel.mentaltrader.feature.historial

import com.miguel.mentaltrader.core.data.Operation
import com.miguel.mentaltrader.core.model.Direction
import com.miguel.mentaltrader.core.model.ResultType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * HU-022 (filtrar por etiqueta/fecha) + HU-023 (buscar por palabra clave), sub-slice EP-003-e,
 * tasks.md 5.5: lógica PURA de la combinación AND de filtros + búsqueda -- sin Room ni Paging,
 * mismo patrón ya usado para [HistorialSeparators]/[HistorialGroupCollapse] (extraer la decisión a
 * una función pura testeable en JVM, sin la dependencia `androidx.paging:paging-testing` que no
 * está en stack-allowlist.json). [HistorialFilterMatcher.matches] especifica de forma inequívoca
 * el comportamiento esperado; [com.miguel.mentaltrader.core.data.OperationDao.pagingSourceFiltered]
 * implementa la misma semántica directamente en SQL (design.md decisión #5, patrón
 * `(:param IS NULL OR columna = :param)`, sin `RawQuery`) porque el filtrado debe ocurrir del lado
 * de la query para que la paginación siga sin cargar el histórico completo a memoria (HU-016) --
 * la equivalencia entre esta función y la query SQL real queda pendiente de un test instrumentado
 * real (tasks.md 5.6, diferido a la pasada final única sin dispositivo conectado en esta sesión).
 */
class HistorialFilterMatcherTest {

    private fun operacion(
        assetId: Long = 1L,
        result: ResultType = ResultType.WIN,
        errorId: Long = 1L,
        emotionBeforeId: Long = 1L,
        emotionAfterId: Long = 1L,
        dateTime: Long = 0L,
        entryDescription: String = "entrada normal",
        emotionBeforeReason: String? = null,
        emotionAfterReason: String? = null,
        errorReason: String? = null
    ) = Operation(
        id = 1L,
        dateTime = dateTime,
        assetId = assetId,
        direction = Direction.BUY,
        quality = 8f,
        emotionBeforeId = emotionBeforeId,
        emotionBeforeReason = emotionBeforeReason,
        emotionAfterId = emotionAfterId,
        emotionAfterReason = emotionAfterReason,
        errorId = errorId,
        errorReason = errorReason,
        result = result,
        entryDescription = entryDescription,
        createdAt = 0L,
        updatedAt = 0L
    )

    // ---- HU-022 Escenario 1: filtrar por una etiqueta (Activo, Resultado, Error, Emoción) ----

    @Test
    fun `filtro de Activo deja pasar solo operaciones de ese activo`() {
        val deXauusd = operacion(assetId = 10L)
        val deEurusd = operacion(assetId = 20L)
        val filtro = HistorialFilterState(assetId = 10L)

        assertTrue(HistorialFilterMatcher.matches(deXauusd, OperationCatalogNames(), filtro))
        assertFalse(HistorialFilterMatcher.matches(deEurusd, OperationCatalogNames(), filtro))
    }

    @Test
    fun `filtro de Resultado deja pasar solo operaciones con ese resultado`() {
        val ganada = operacion(result = ResultType.WIN)
        val perdida = operacion(result = ResultType.LOSS)
        val filtro = HistorialFilterState(result = ResultType.WIN)

        assertTrue(HistorialFilterMatcher.matches(ganada, OperationCatalogNames(), filtro))
        assertFalse(HistorialFilterMatcher.matches(perdida, OperationCatalogNames(), filtro))
    }

    @Test
    fun `filtro de Error deja pasar solo operaciones con ese error`() {
        val conError = operacion(errorId = 5L)
        val sinEseError = operacion(errorId = 6L)
        val filtro = HistorialFilterState(errorId = 5L)

        assertTrue(HistorialFilterMatcher.matches(conError, OperationCatalogNames(), filtro))
        assertFalse(HistorialFilterMatcher.matches(sinEseError, OperationCatalogNames(), filtro))
    }

    @Test
    fun `filtro de Emocion antes y de Emocion despues son independientes entre si`() {
        val op = operacion(emotionBeforeId = 2L, emotionAfterId = 3L)
        val filtroAntesCorrecto = HistorialFilterState(emotionBeforeId = 2L)
        val filtroDespuesIncorrecto = HistorialFilterState(emotionAfterId = 99L)

        assertTrue(HistorialFilterMatcher.matches(op, OperationCatalogNames(), filtroAntesCorrecto))
        assertFalse(HistorialFilterMatcher.matches(op, OperationCatalogNames(), filtroDespuesIncorrecto))
    }

    // ---- HU-022 Escenario 2: filtrar por fecha o periodo ----

    @Test
    fun `filtro de rango de fecha deja pasar solo operaciones dentro del rango`() {
        val dentro = operacion(dateTime = 1_000L)
        val antes = operacion(dateTime = 500L)
        val despues = operacion(dateTime = 2_000L)
        val filtro = HistorialFilterState(dateFrom = 900L, dateTo = 1_500L)

        assertTrue(HistorialFilterMatcher.matches(dentro, OperationCatalogNames(), filtro))
        assertFalse(HistorialFilterMatcher.matches(antes, OperationCatalogNames(), filtro))
        assertFalse(HistorialFilterMatcher.matches(despues, OperationCatalogNames(), filtro))
    }

    // ---- HU-022 Escenario 3: combinar múltiples filtros de etiqueta con lógica AND ----

    @Test
    fun `combinar Activo y Resultado exige que ambos coincidan a la vez (AND)`() {
        val cumpleAmbos = operacion(assetId = 10L, result = ResultType.WIN)
        val soloActivo = operacion(assetId = 10L, result = ResultType.LOSS)
        val soloResultado = operacion(assetId = 20L, result = ResultType.WIN)
        val filtro = HistorialFilterState(assetId = 10L, result = ResultType.WIN)

        assertTrue(HistorialFilterMatcher.matches(cumpleAmbos, OperationCatalogNames(), filtro))
        assertFalse(HistorialFilterMatcher.matches(soloActivo, OperationCatalogNames(), filtro))
        assertFalse(HistorialFilterMatcher.matches(soloResultado, OperationCatalogNames(), filtro))
    }

    // ---- HU-022 Escenario 4: limpiar todos los filtros ----

    @Test
    fun `el estado de filtro vacio (limpiar filtros) deja pasar cualquier operacion`() {
        val cualquiera = operacion(assetId = 999L, result = ResultType.LOSS, dateTime = 123L)

        assertTrue(HistorialFilterState().isEmpty)
        assertTrue(HistorialFilterMatcher.matches(cualquiera, OperationCatalogNames(), HistorialFilterState()))
    }

    @Test
    fun `un filtro con al menos un campo activo no esta vacio`() {
        assertFalse(HistorialFilterState(assetId = 1L).isEmpty)
        assertFalse(HistorialFilterState(searchText = "ruptura").isEmpty)
    }

    // ---- HU-023 Escenario 1: buscar por palabra clave (texto libre o nombre de catálogo) ----

    @Test
    fun `busqueda de texto coincide con los 4 campos de texto libre de la operacion`() {
        val filtro = HistorialFilterState(searchText = "ruptura")

        assertTrue(HistorialFilterMatcher.matches(operacion(entryDescription = "ruptura de resistencia"), OperationCatalogNames(), filtro))
        assertTrue(HistorialFilterMatcher.matches(operacion(entryDescription = "otra cosa", emotionBeforeReason = "vi una ruptura clara"), OperationCatalogNames(), filtro))
        assertTrue(HistorialFilterMatcher.matches(operacion(entryDescription = "otra cosa", emotionAfterReason = "me arrepenti de la ruptura"), OperationCatalogNames(), filtro))
        assertTrue(HistorialFilterMatcher.matches(operacion(entryDescription = "otra cosa", errorReason = "entre antes de la ruptura confirmada"), OperationCatalogNames(), filtro))
        assertFalse(HistorialFilterMatcher.matches(operacion(entryDescription = "sin coincidencia"), OperationCatalogNames(), filtro))
    }

    @Test
    fun `busqueda de texto tambien coincide con el nombre de catalogo seleccionado en la operacion`() {
        val filtro = HistorialFilterState(searchText = "XAUUSD")
        val nombres = OperationCatalogNames(assetName = "XAUUSD")

        assertTrue(HistorialFilterMatcher.matches(operacion(entryDescription = "no menciona el activo"), nombres, filtro))
        assertFalse(HistorialFilterMatcher.matches(operacion(entryDescription = "no menciona el activo"), OperationCatalogNames(assetName = "EURUSD"), filtro))
    }

    @Test
    fun `busqueda de texto es parcial e insensible a mayusculas`() {
        val filtro = HistorialFilterState(searchText = "RUPTURA")

        assertTrue(HistorialFilterMatcher.matches(operacion(entryDescription = "una ruptura de estructura"), OperationCatalogNames(), filtro))
    }

    // ---- HU-023 Escenario 2: combinar búsqueda de texto con un filtro de etiqueta ya activo ----

    @Test
    fun `busqueda combinada con filtro de Activo exige ambos a la vez (AND)`() {
        val xauusdConRuptura = operacion(assetId = 10L, entryDescription = "ruptura de maximos")
        val xauusdSinRuptura = operacion(assetId = 10L, entryDescription = "entrada normal")
        val eurusdConRuptura = operacion(assetId = 20L, entryDescription = "ruptura de maximos")
        val filtro = HistorialFilterState(assetId = 10L, searchText = "ruptura")

        assertTrue(HistorialFilterMatcher.matches(xauusdConRuptura, OperationCatalogNames(), filtro))
        assertFalse(HistorialFilterMatcher.matches(xauusdSinRuptura, OperationCatalogNames(), filtro))
        assertFalse(HistorialFilterMatcher.matches(eurusdConRuptura, OperationCatalogNames(), filtro))
    }

    // ---- HU-023 Escenario 3: búsqueda sin coincidencias ----

    @Test
    fun `busqueda sin coincidencias en ningun campo ni catalogo no lanza error y no coincide`() {
        val filtro = HistorialFilterState(searchText = "palabra-inexistente-xyz")
        val op = operacion(entryDescription = "algo distinto")

        assertFalse(HistorialFilterMatcher.matches(op, OperationCatalogNames(assetName = "XAUUSD", errorName = "Ninguno"), filtro))
    }

    @Test
    fun `texto de busqueda en blanco equivale a no buscar nada`() {
        val filtro = HistorialFilterState(searchText = "   ")

        assertTrue(HistorialFilterMatcher.matches(operacion(), OperationCatalogNames(), filtro))
    }
}
