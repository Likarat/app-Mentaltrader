package com.miguel.mentaltrader.feature.historial

import com.miguel.mentaltrader.core.data.CatalogItem
import com.miguel.mentaltrader.core.data.CatalogItemDao
import com.miguel.mentaltrader.core.data.MonthSummary
import com.miguel.mentaltrader.core.data.Operation
import com.miguel.mentaltrader.core.data.OperationDao
import com.miguel.mentaltrader.core.data.OperationImage
import com.miguel.mentaltrader.core.data.OperationImageDao
import com.miguel.mentaltrader.core.model.CatalogType
import com.miguel.mentaltrader.core.model.Direction
import com.miguel.mentaltrader.core.model.ResultType
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests unitarios de HistorialViewModel (tasks.md 3.7, sub-slice EP-003-c, HU-021): el patrón de
 * "deshacer" (soft-delete SOLO en memoria vía `pendingDeleteIds` + `Job` cancelable con delay,
 * design.md decisión #4). Usa `StandardTestDispatcher` (NO `UnconfinedTestDispatcher`, como el
 * resto de los tests de esta rama) porque necesita control explícito del tiempo virtual sobre
 * `delay(UNDO_WINDOW_MS)`, pasando el mismo dispatcher a `runTest` para que su scheduler quede
 * sincronizado con el de `Dispatchers.Main` (`viewModelScope`). Fakes locales de los 3 DAOs, mismo
 * estilo que `OperationFormViewModelTest`/`HistorialDetalleViewModelTest` (sin `testutil/`
 * compartido en esta rama).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HistorialViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var operationDao: FakeOperationDao
    private lateinit var operationImageDao: FakeOperationImageDao
    private lateinit var catalogItemDao: FakeCatalogItemDao
    private val deletedFiles = mutableListOf<String>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        operationDao = FakeOperationDao()
        operationImageDao = FakeOperationImageDao()
        catalogItemDao = FakeCatalogItemDao()
        deletedFiles.clear()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = HistorialViewModel(
        operationDao,
        operationImageDao,
        catalogItemDao,
        deleteImageFiles = { filePath -> deletedFiles += filePath }
    )

    private fun operacion(id: Long = 0L) = Operation(
        id = id,
        dateTime = 0L,
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

    // HU-021 Escenario 1: eliminar retira la operación del listado de inmediato (soft-delete en
    // memoria), sin borrarla todavía de Room.
    @Test
    fun `eliminar una operacion la agrega de inmediato a pendingDeleteIds sin tocar Room todavia`() = runTest(dispatcher) {
        val operationId = operationDao.insert(operacion())
        val viewModel = createViewModel()

        viewModel.onRequestDelete(operationId)

        assertTrue(operationId in viewModel.pendingDeleteIds.value)
        assertNotNull(operationDao.getById(operationId)) // todavía existe en Room
    }

    // HU-021 Escenario 2: deshacer dentro del tiempo disponible cancela el borrado y restaura la
    // operación exactamente como estaba (nunca se tocó Room).
    @Test
    fun `deshacer dentro del tiempo disponible cancela el borrado y la operacion sigue intacta`() = runTest(dispatcher) {
        val operationId = operationDao.insert(operacion())
        val viewModel = createViewModel()

        viewModel.onRequestDelete(operationId)
        advanceTimeBy(HistorialViewModel.UNDO_WINDOW_MS / 2)
        viewModel.onUndoDelete(operationId)
        advanceTimeBy(HistorialViewModel.UNDO_WINDOW_MS) // deja correr de sobra el tiempo restante
        runCurrent()

        assertFalse(operationId in viewModel.pendingDeleteIds.value)
        assertNotNull(operationDao.getById(operationId))
        assertTrue(deletedFiles.isEmpty())
    }

    // HU-021 Escenario 3: al expirar la ventana sin deshacer, se elimina definitivamente la
    // operación y sus imágenes asociadas (filas + archivos en filesDir).
    @Test
    fun `expirar la ventana de deshacer elimina definitivamente la operacion y sus imagenes`() = runTest(dispatcher) {
        val operationId = operationDao.insert(operacion())
        operationImageDao.insert(OperationImage(operationId = operationId, filePath = "images/a.jpg", position = 0, createdAt = 0L))
        operationImageDao.insert(OperationImage(operationId = operationId, filePath = "images/b.jpg", position = 1, createdAt = 0L))
        val viewModel = createViewModel()

        viewModel.onRequestDelete(operationId)
        advanceTimeBy(HistorialViewModel.UNDO_WINDOW_MS + 100)
        runCurrent()

        assertNull(operationDao.getById(operationId))
        assertTrue(operationImageDao.getAll().none { it.operationId == operationId })
        assertEquals(setOf("images/a.jpg", "images/b.jpg"), deletedFiles.toSet())
        assertFalse(operationId in viewModel.pendingDeleteIds.value)
    }

    // Edge adicional: pedir eliminar el mismo id dos veces no duplica el Job ni el borrado.
    @Test
    fun `pedir eliminar el mismo id dos veces no duplica el borrado`() = runTest(dispatcher) {
        val operationId = operationDao.insert(operacion())
        val viewModel = createViewModel()

        viewModel.onRequestDelete(operationId)
        viewModel.onRequestDelete(operationId)
        advanceTimeBy(HistorialViewModel.UNDO_WINDOW_MS + 100)
        runCurrent()

        assertNull(operationDao.getById(operationId))
        assertEquals(1, operationDao.deleteByIdCallCount)
    }

    // HU-017 Escenario 2: colapsar/expandir un mes es estado de UI puro del ViewModel -- no toca
    // Room, y colapsar un mes no afecta el estado de otro.
    @Test
    fun `colapsar un mes lo agrega a collapsedMonths`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        val agosto = YearMonth.of(2026, 8)

        viewModel.onToggleMonth(agosto)

        assertTrue(agosto in viewModel.collapsedMonths.value)
    }

    @Test
    fun `expandir un mes ya colapsado lo quita de collapsedMonths`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        val agosto = YearMonth.of(2026, 8)

        viewModel.onToggleMonth(agosto)
        viewModel.onToggleMonth(agosto)

        assertFalse(agosto in viewModel.collapsedMonths.value)
    }

    @Test
    fun `colapsar un mes no afecta el estado de otro mes`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        val agosto = YearMonth.of(2026, 8)
        val julio = YearMonth.of(2026, 7)

        viewModel.onToggleMonth(agosto)

        assertTrue(agosto in viewModel.collapsedMonths.value)
        assertFalse(julio in viewModel.collapsedMonths.value)
    }

    // HU-022/HU-023 (sub-slice EP-003-e): el estado de filtro por defecto no tiene ningún
    // criterio activo (equivalente a "Limpiar filtros" ya aplicado, HU-022 Escenario 4).
    @Test
    fun `el filterState inicial esta vacio (sin ningun criterio activo)`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        assertTrue(viewModel.filterState.value.isEmpty)
    }

    // HU-022 Escenario 1: cada setter de filtro de etiqueta actualiza solo su propio campo.
    @Test
    fun `onFilterAsset actualiza solo el assetId del filtro`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.onFilterAsset(10L)

        assertEquals(10L, viewModel.filterState.value.assetId)
        assertNull(viewModel.filterState.value.result)
    }

    @Test
    fun `onFilterResult actualiza solo el resultado del filtro`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.onFilterResult(ResultType.LOSS)

        assertEquals(ResultType.LOSS, viewModel.filterState.value.result)
    }

    // HU-022 Escenario 3: combinar dos filtros de etiqueta a la vez deja ambos campos activos en
    // el mismo HistorialFilterState (la combinación AND real la aplica HistorialFilterMatcher /
    // la query de OperationDao sobre ese único estado, ver su propio test).
    @Test
    fun `combinar Activo y Resultado deja ambos activos a la vez en el mismo filterState`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.onFilterAsset(10L)
        viewModel.onFilterResult(ResultType.WIN)

        assertEquals(10L, viewModel.filterState.value.assetId)
        assertEquals(ResultType.WIN, viewModel.filterState.value.result)
    }

    // HU-022 Escenario 2: seleccionar un periodo predefinido resuelve dateFrom/dateTo reales vía
    // PeriodoFiltroRange (misma lógica pura ya testeada en PeriodoFiltroRangeTest).
    @Test
    fun `onSelectPeriodo resuelve dateFrom y dateTo reales para un periodo predefinido`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        val hoy = java.time.LocalDate.of(2026, 8, 15)

        viewModel.onSelectPeriodo(PeriodoFiltro.ULTIMA_SEMANA, hoy)

        val esperado = PeriodoFiltroRange.rangeFor(PeriodoFiltro.ULTIMA_SEMANA, hoy)
        assertEquals(esperado?.first, viewModel.filterState.value.dateFrom)
        assertEquals(esperado?.second, viewModel.filterState.value.dateTo)
        assertEquals(PeriodoFiltro.ULTIMA_SEMANA, viewModel.filterState.value.selectedPeriodo)
    }

    // Personalizado no resuelve rango propio -- espera el rango explícito del usuario.
    @Test
    fun `onSelectPeriodo Personalizado no fija dateFrom ni dateTo hasta que se elige el rango`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.onSelectPeriodo(PeriodoFiltro.PERSONALIZADO, java.time.LocalDate.of(2026, 8, 15))

        assertNull(viewModel.filterState.value.dateFrom)
        assertNull(viewModel.filterState.value.dateTo)
        assertEquals(PeriodoFiltro.PERSONALIZADO, viewModel.filterState.value.selectedPeriodo)

        viewModel.onCustomDateRange(from = 100L, to = 200L)

        assertEquals(100L, viewModel.filterState.value.dateFrom)
        assertEquals(200L, viewModel.filterState.value.dateTo)
    }

    // HU-023 Escenario 1/2: el texto de búsqueda es un criterio más del mismo filterState.
    @Test
    fun `onSearchTextChanged actualiza el texto de busqueda sin tocar los demas filtros`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.onFilterAsset(10L)
        viewModel.onSearchTextChanged("ruptura")

        assertEquals(10L, viewModel.filterState.value.assetId)
        assertEquals("ruptura", viewModel.filterState.value.searchText)
    }

    // HU-022 Escenario 4: "Limpiar filtros" restaura el estado por defecto (sin ningún criterio).
    @Test
    fun `onClearFilters restaura el filterState por defecto`() = runTest(dispatcher) {
        val viewModel = createViewModel()
        viewModel.onFilterAsset(10L)
        viewModel.onFilterResult(ResultType.WIN)
        viewModel.onSearchTextChanged("ruptura")
        viewModel.onSelectPeriodo(PeriodoFiltro.ESTE_MES, java.time.LocalDate.of(2026, 8, 15))

        viewModel.onClearFilters()

        assertTrue(viewModel.filterState.value.isEmpty)
    }

    // HU-025 (sub-slice EP-003-f): totalOperationCount refleja el conteo real de operaciones de
    // OperationDao.countAll -- HistorialScreen lo combina con items.itemCount y filterState.isEmpty
    // (vía HistorialEmptyState.resolve, testeado aparte en HistorialEmptyStateTest) para distinguir
    // "primera vez" de "sin resultados de filtro".
    @Test
    fun `totalOperationCount es 0 cuando no hay ninguna operacion registrada`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        assertEquals(0, viewModel.totalOperationCount.first())
    }

    @Test
    fun `totalOperationCount refleja el conteo real tras insertar operaciones`() = runTest(dispatcher) {
        operationDao.insert(operacion())
        operationDao.insert(operacion())
        val viewModel = createViewModel()

        assertEquals(2, viewModel.totalOperationCount.first())
    }

    private class FakeOperationDao : OperationDao {
        val inserted = mutableListOf<Operation>()
        var deleteByIdCallCount = 0
        private var nextId = 1L

        override suspend fun insert(operation: Operation): Long {
            val withId = operation.copy(id = nextId++)
            inserted += withId
            return withId.id
        }

        override fun getAllOrderedByDateDesc(): Flow<List<Operation>> =
            MutableStateFlow(inserted.sortedByDescending { it.dateTime })

        // HU-025: conteo real, reflejando el estado de `inserted` en el momento en que se colecta
        // (misma limitación que el resto de los fakes de este archivo -- no es un `Flow` reactivo a
        // futuros inserts, ver `getAllOrderedByDateDesc`; suficiente para los tests de wiring de
        // `HistorialViewModel.totalOperationCount`, que colectan tras insertar).
        override fun countAll(): Flow<Int> = MutableStateFlow(inserted.size)

        override fun pagingSourceOrderedByDateDesc(): androidx.paging.PagingSource<Int, Operation> =
            throw UnsupportedOperationException("No usado por HistorialViewModelTest (requiere Room real, ver tasks.md 1.8)")

        override suspend fun getById(id: Long): Operation? = inserted.find { it.id == id }

        override suspend fun update(operation: Operation) {
            val index = inserted.indexOfFirst { it.id == operation.id }
            if (index >= 0) inserted[index] = operation
        }

        override fun monthlySummaries(): Flow<List<MonthSummary>> =
            throw UnsupportedOperationException("No usado por estos tests (requiere Room real, cálculo SQL, ver tasks.md 4.5)")

        override fun pagingSourceFiltered(
            assetId: Long?,
            result: ResultType?,
            errorId: Long?,
            emotionBeforeId: Long?,
            emotionAfterId: Long?,
            dateFrom: Long?,
            dateTo: Long?,
            searchText: String?
        ): androidx.paging.PagingSource<Int, Operation> =
            throw UnsupportedOperationException("No usado por HistorialViewModelTest (requiere Room real, ver tasks.md 5.6; la lógica de filtro/búsqueda se testea aparte, sin Room, en HistorialFilterMatcherTest)")

        override suspend fun deleteById(id: Long) {
            deleteByIdCallCount++
            inserted.removeAll { it.id == id }
        }

        override suspend fun deleteAll() {
            inserted.clear()
        }
    }

    private class FakeOperationImageDao : OperationImageDao {
        private val images = mutableListOf<OperationImage>()
        private var nextId = 1L

        override suspend fun insert(image: OperationImage): Long {
            val id = nextId++
            images += image.copy(id = id)
            return id
        }

        override suspend fun insertAll(images: List<OperationImage>) {
            images.forEach { insert(it) }
        }

        override fun getByOperationId(operationId: Long): Flow<List<OperationImage>> =
            MutableStateFlow(images.filter { it.operationId == operationId }.sortedBy { it.position })

        override suspend fun getAll(): List<OperationImage> = images

        override suspend fun deleteByOperationId(operationId: Long) {
            images.removeAll { it.operationId == operationId }
        }

        override suspend fun deleteAll() {
            images.clear()
        }
    }

    private class FakeCatalogItemDao : CatalogItemDao {
        private val items = mutableListOf<CatalogItem>()
        private var nextId = 1L

        override suspend fun insert(item: CatalogItem): Long {
            val id = nextId++
            items += item.copy(id = id)
            return id
        }

        override suspend fun insertAll(items: List<CatalogItem>) {
            items.forEach { insert(it) }
        }

        override fun getByType(type: CatalogType): Flow<List<CatalogItem>> =
            MutableStateFlow(items.filter { it.type == type })

        override suspend fun countByType(type: CatalogType): Int = items.count { it.type == type }

        override suspend fun getById(id: Long): CatalogItem? = items.find { it.id == id }
    }
}
