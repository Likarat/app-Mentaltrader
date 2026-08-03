package com.miguel.mentaltrader.feature.inicio

import com.miguel.mentaltrader.core.data.Operation
import com.miguel.mentaltrader.core.model.Direction
import com.miguel.mentaltrader.core.model.ResultType
import com.miguel.mentaltrader.testutil.FakeOperationDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests unitarios de InicioViewModel (tasks.md, sub-slice EP-004-a, HU-026): la única
 * transformación real que vive en el ViewModel (el resto son pass-through directos de
 * `OperationDao`, mismo criterio que `HistorialViewModel.monthSummaries`/`assets`/etc., ver su
 * KDoc) es `cumulativeSeries` (`ResultInRCumulativeSeries.build` sobre
 * `OperationDao.resultInROrderedByDateAsc`) -- eso es lo que se testea aquí, con el
 * `FakeOperationDao` compartido de `testutil` (su implementación real de
 * `resultInROrderedByDateAsc`, ver KDoc de esa clase). `metricsSummary`/`emotionRanking`/
 * `errorRanking` NO se ejercitan en estos tests (requieren Room real, agregación SQL, mismo
 * criterio ya establecido para `HistorialViewModel.monthSummaries` -- diferido al instrumentado).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InicioViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var operationDao: FakeOperationDao

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        operationDao = FakeOperationDao()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = InicioViewModel(operationDao)

    @Test
    fun `sin operaciones la serie acumulada queda vacia`() = runTest {
        val viewModel = createViewModel()

        val series = viewModel.cumulativeSeries.first()

        assertTrue(series.isEmpty())
    }

    @Test
    fun `la serie acumulada refleja el R de cada operacion en orden cronologico`() = runTest {
        operationDao.insert(operacion(dateTime = 3_000L, resultInR = 2f))
        operationDao.insert(operacion(dateTime = 1_000L, resultInR = 1f))
        operationDao.insert(operacion(dateTime = 2_000L, resultInR = -0.5f))

        val viewModel = createViewModel()
        val series = viewModel.cumulativeSeries.first()

        // Orden cronológico ascendente (1_000 -> 2_000 -> 3_000), no el orden de inserción.
        assertEquals(listOf(1f, 0.5f, 2.5f), series.map { it.cumulativeResultInR })
    }

    @Test
    fun `una operacion sin R cargado no rompe la serie acumulada`() = runTest {
        operationDao.insert(operacion(dateTime = 1_000L, resultInR = 1f))
        operationDao.insert(operacion(dateTime = 2_000L, resultInR = null))

        val viewModel = createViewModel()
        val series = viewModel.cumulativeSeries.first()

        assertEquals(listOf(1f, 1f), series.map { it.cumulativeResultInR })
    }

    private fun operacion(dateTime: Long, resultInR: Float?) = Operation(
        dateTime = dateTime,
        assetId = 1L,
        direction = Direction.BUY,
        quality = 8f,
        emotionBeforeId = 1L,
        emotionAfterId = 1L,
        errorId = 1L,
        result = ResultType.WIN,
        resultInR = resultInR,
        entryDescription = "test",
        createdAt = 0L,
        updatedAt = 0L
    )
}
