package com.miguel.mentaltrader.feature.historial

import com.miguel.mentaltrader.core.data.CatalogItem
import com.miguel.mentaltrader.core.data.CatalogItemDao
import com.miguel.mentaltrader.core.data.Operation
import com.miguel.mentaltrader.core.data.OperationDao
import com.miguel.mentaltrader.core.data.OperationImage
import com.miguel.mentaltrader.core.data.OperationImageDao
import com.miguel.mentaltrader.core.model.CatalogType
import com.miguel.mentaltrader.core.model.Direction
import com.miguel.mentaltrader.core.model.ResultType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests unitarios de HistorialDetalleViewModel (tasks.md 2.5, sub-slice EP-003-b, HU-018): carga
 * la operación + sus imágenes + los nombres de catálogo resueltos (Activo/Emociones/Error).
 * Usa fakes locales de OperationDao/OperationImageDao/CatalogItemDao (sin Room), mismo estilo que
 * los fakes privados de OperationFormViewModelTest (esta rama todavía no tiene testutil/ compartido).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HistorialDetalleViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var operationDao: FakeOperationDao
    private lateinit var operationImageDao: FakeOperationImageDao
    private lateinit var catalogItemDao: FakeCatalogItemDao

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        operationDao = FakeOperationDao()
        operationImageDao = FakeOperationImageDao()
        catalogItemDao = FakeCatalogItemDao()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(operationId: Long) =
        HistorialDetalleViewModel(operationId, operationDao, operationImageDao, catalogItemDao)

    // HU-018 Escenario 1/2: carga la operación real con sus nombres de catálogo resueltos.
    @Test
    fun `carga la operacion con los nombres de catalogo resueltos`() = runTest {
        val assetId = catalogItemDao.seed(CatalogType.ASSET, "XAUUSD")
        val emotionBeforeId = catalogItemDao.seed(CatalogType.EMOTION, "Confianza")
        val emotionAfterId = catalogItemDao.seed(CatalogType.EMOTION, "Calma")
        val errorId = catalogItemDao.seed(CatalogType.ERROR, "Ninguno")
        val operationId = operationDao.insert(
            operacion(assetId, emotionBeforeId, emotionAfterId, errorId)
        )

        val viewModel = createViewModel(operationId)

        val state = viewModel.state.value
        assertEquals(operationId, state.operation?.id)
        assertEquals("XAUUSD", state.assetName)
        assertEquals("Confianza", state.emotionBeforeName)
        assertEquals("Calma", state.emotionAfterName)
        assertEquals("Ninguno", state.errorName)
        assertTrue(!state.isLoading)
    }

    // HU-018 Escenario 2: las imágenes de la operación se cargan junto con el resto.
    @Test
    fun `carga las imagenes adjuntas de la operacion`() = runTest {
        val assetId = catalogItemDao.seed(CatalogType.ASSET, "XAUUSD")
        val emotionId = catalogItemDao.seed(CatalogType.EMOTION, "Confianza")
        val errorId = catalogItemDao.seed(CatalogType.ERROR, "Ninguno")
        val operationId = operationDao.insert(operacion(assetId, emotionId, emotionId, errorId))
        operationImageDao.insert(OperationImage(operationId = operationId, filePath = "images/a.jpg", position = 0, createdAt = 0L))

        val viewModel = createViewModel(operationId)

        assertEquals(1, viewModel.state.value.images.size)
        assertEquals("images/a.jpg", viewModel.state.value.images.first().filePath)
    }

    // HU-018 Escenario 3: sin ninguna imagen adjunta, la lista de imágenes queda vacía (la UI
    // omite la sección sin dejar espacio vacío ni layout roto -- responsabilidad de la Screen).
    @Test
    fun `sin imagenes adjuntas la lista de imagenes queda vacia`() = runTest {
        val assetId = catalogItemDao.seed(CatalogType.ASSET, "XAUUSD")
        val emotionId = catalogItemDao.seed(CatalogType.EMOTION, "Confianza")
        val errorId = catalogItemDao.seed(CatalogType.ERROR, "Ninguno")
        val operationId = operationDao.insert(operacion(assetId, emotionId, emotionId, errorId))

        val viewModel = createViewModel(operationId)

        assertTrue(viewModel.state.value.images.isEmpty())
    }

    @Test
    fun `operacion inexistente deja el estado sin cargar sin crashear`() = runTest {
        val viewModel = createViewModel(operationId = 999L)

        assertNull(viewModel.state.value.operation)
        assertTrue(!viewModel.state.value.isLoading)
    }

    private fun operacion(assetId: Long, emotionBeforeId: Long, emotionAfterId: Long, errorId: Long) = Operation(
        dateTime = 0L,
        assetId = assetId,
        direction = Direction.BUY,
        quality = 8f,
        emotionBeforeId = emotionBeforeId,
        emotionAfterId = emotionAfterId,
        errorId = errorId,
        result = ResultType.WIN,
        entryDescription = "test",
        createdAt = 0L,
        updatedAt = 0L
    )

    private class FakeOperationDao : OperationDao {
        val inserted = mutableListOf<Operation>()
        private var nextId = 1L

        override suspend fun insert(operation: Operation): Long {
            val withId = operation.copy(id = nextId++)
            inserted += withId
            return withId.id
        }

        override fun getAllOrderedByDateDesc(): Flow<List<Operation>> =
            MutableStateFlow(inserted.sortedByDescending { it.dateTime })

        override fun countAll(): Flow<Int> =
            throw UnsupportedOperationException("No usado por HistorialDetalleViewModelTest (feature de estado vacío del listado, EP-003-f)")

        override fun pagingSourceOrderedByDateDesc(): androidx.paging.PagingSource<Int, Operation> =
            throw UnsupportedOperationException("No usado por HistorialDetalleViewModelTest")

        override suspend fun getById(id: Long): Operation? = inserted.find { it.id == id }

        override suspend fun update(operation: Operation) {
            val index = inserted.indexOfFirst { it.id == operation.id }
            if (index >= 0) inserted[index] = operation
        }

        override fun monthlySummaries(): Flow<List<com.miguel.mentaltrader.core.data.MonthSummary>> =
            throw UnsupportedOperationException("No usado por HistorialDetalleViewModelTest")

        override fun pagingSourceFiltered(
            assetId: Long?,
            result: com.miguel.mentaltrader.core.model.ResultType?,
            errorId: Long?,
            emotionBeforeId: Long?,
            emotionAfterId: Long?,
            dateFrom: Long?,
            dateTo: Long?,
            searchText: String?
        ): androidx.paging.PagingSource<Int, Operation> =
            throw UnsupportedOperationException("No usado por HistorialDetalleViewModelTest")

        override suspend fun deleteById(id: Long) {
            inserted.removeAll { it.id == id }
        }

        override suspend fun deleteAll() {
            inserted.clear()
        }

        override suspend fun countUsageOfCatalogItem(id: Long): Int =
            inserted.count {
                it.assetId == id || it.emotionBeforeId == id || it.emotionAfterId == id || it.errorId == id
            }

        override fun metricsSummary(): Flow<com.miguel.mentaltrader.core.data.OperationMetricsSummary> =
            throw UnsupportedOperationException("No usado por HistorialDetalleViewModelTest (EP-004, feature/inicio)")

        override fun resultInROrderedByDateAsc(): Flow<List<Float?>> =
            throw UnsupportedOperationException("No usado por HistorialDetalleViewModelTest (EP-004, feature/inicio)")

        override fun emotionRanking(): Flow<List<com.miguel.mentaltrader.core.data.CatalogRankingItem>> =
            throw UnsupportedOperationException("No usado por HistorialDetalleViewModelTest (EP-004, feature/inicio)")

        override fun errorRanking(): Flow<List<com.miguel.mentaltrader.core.data.CatalogRankingItem>> =
            throw UnsupportedOperationException("No usado por HistorialDetalleViewModelTest (EP-004, feature/inicio)")
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

        fun seed(type: CatalogType, name: String): Long {
            val id = nextId++
            items += CatalogItem(id = id, type = type, name = name, isDefault = true, createdAt = 0L, updatedAt = 0L)
            return id
        }

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

        override suspend fun update(item: CatalogItem) {
            val index = items.indexOfFirst { it.id == item.id }
            if (index >= 0) items[index] = item
        }

        override suspend fun delete(item: CatalogItem) {
            items.removeAll { it.id == item.id }
        }

        override suspend fun countByTypeAndNameIgnoreCaseExcludingId(type: CatalogType, name: String, excludeId: Long): Int =
            items.count {
                it.type == type && it.id != excludeId && it.name.trim().equals(name.trim(), ignoreCase = true)
            }
    }
}
