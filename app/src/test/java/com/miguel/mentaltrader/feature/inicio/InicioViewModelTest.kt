package com.miguel.mentaltrader.feature.inicio

import com.miguel.mentaltrader.core.data.Operation
import com.miguel.mentaltrader.core.model.Direction
import com.miguel.mentaltrader.core.model.ResultType
import com.miguel.mentaltrader.testutil.FakeOperationDao
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
 * Tests unitarios de InicioViewModel (tasks.md, sub-slice EP-004-a HU-026 + sub-slice EP-004-b
 * HU-028): la única transformación real que vive en el ViewModel (el resto son pass-through
 * directos de `OperationDao`, mismo criterio que `HistorialViewModel.monthSummaries`/`assets`/etc.,
 * ver su KDoc) es `cumulativeSeries` (`ResultInRCumulativeSeries.build` sobre
 * `OperationDao.resultInROrderedByDateAsc`) -- eso es lo que se testea aquí, con el
 * `FakeOperationDao` compartido de `testutil` (su implementación real de
 * `resultInROrderedByDateAsc`, ver KDoc de esa clase, ahora también filtrada por `dateFrom`/
 * `dateTo`). `metricsSummary`/`emotionRanking`/`errorRanking` NO se ejercitan en estos tests
 * (requieren Room real, agregación SQL, mismo criterio ya establecido para
 * `HistorialViewModel.monthSummaries` -- diferido al instrumentado).
 *
 * HU-028: los tests de `onSelectPeriodo`/recálculo/aislamiento entre periodos sucesivos verifican
 * el filtrado real de `cumulativeSeries` (a través de `resultInROrderedByDateAsc`, la única query
 * de las 4 nuevas de EP-004 con implementación real en el fake) -- `metricsSummary`/
 * `emotionRanking`/`errorRanking` reciben el mismo `dateFrom`/`dateTo` vía el mismo `filterState`
 * (ver KDoc de `InicioViewModel`), su cableado real queda verificado por lectura de código y
 * diferido al instrumentado, mismo criterio que EP-004-a.
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

    // ---- HU-028 (sub-slice EP-004-b): selector de periodo predefinido ----

    // `InicioViewModel.onSelectPeriodo` resuelve el rango con `ZoneId.systemDefault()` (mismo
    // default que `PeriodoInicioFiltroRange.rangeFor`, sin zona explícita) -- los timestamps de
    // prueba deben construirse con la MISMA zona para que la comparación sea real, no una
    // coincidencia del huso horario de la máquina que corre el test.
    private val zone = ZoneId.systemDefault()
    private val hoy = LocalDate.of(2026, 8, 15)

    @Test
    fun `sin periodo seleccionado el filterState queda por defecto (todas las operaciones)`() = runTest {
        val viewModel = createViewModel()

        assertNull(viewModel.filterState.value.selectedPeriodo)
        assertNull(viewModel.filterState.value.dateFrom)
        assertNull(viewModel.filterState.value.dateTo)
    }

    @Test
    fun `onSelectPeriodo resuelve dateFrom y dateTo reales para un periodo predefinido`() = runTest {
        val viewModel = createViewModel()

        viewModel.onSelectPeriodo(PeriodoInicioFiltro.MES, hoy)

        val esperado = PeriodoInicioFiltroRange.rangeFor(PeriodoInicioFiltro.MES, hoy)
        assertEquals(PeriodoInicioFiltro.MES, viewModel.filterState.value.selectedPeriodo)
        assertEquals(esperado.first, viewModel.filterState.value.dateFrom)
        assertEquals(esperado.second, viewModel.filterState.value.dateTo)
    }

    @Test
    fun `seleccionar un periodo recalcula la serie usando solo las operaciones dentro del rango`() = runTest {
        val dentro = hoy.atStartOfDay(zone).toInstant().toEpochMilli() + 1_000
        val fueraDelDia = hoy.minusDays(10).atStartOfDay(zone).toInstant().toEpochMilli()
        operationDao.insert(operacion(dateTime = dentro, resultInR = 2f))
        operationDao.insert(operacion(dateTime = fueraDelDia, resultInR = 100f))

        val viewModel = createViewModel()
        viewModel.onSelectPeriodo(PeriodoInicioFiltro.DIA, hoy)

        val series = viewModel.cumulativeSeries.first()

        assertEquals(listOf(2f), series.map { it.cumulativeResultInR })
    }

    @Test
    fun `cambiar de un periodo a otro recalcula desde cero, sin arrastrar datos del periodo anterior`() = runTest {
        val dentroDelDia = hoy.atStartOfDay(zone).toInstant().toEpochMilli() + 1_000
        val soloDentroDe3Meses = hoy.minusDays(60).atStartOfDay(zone).toInstant().toEpochMilli()
        operationDao.insert(operacion(dateTime = dentroDelDia, resultInR = 1f))
        operationDao.insert(operacion(dateTime = soloDentroDe3Meses, resultInR = 5f))

        val viewModel = createViewModel()

        viewModel.onSelectPeriodo(PeriodoInicioFiltro.DIA, hoy)
        val seriesDia = viewModel.cumulativeSeries.first()
        assertEquals(listOf(1f), seriesDia.map { it.cumulativeResultInR })

        viewModel.onSelectPeriodo(PeriodoInicioFiltro.ULTIMOS_3_MESES, hoy)
        val seriesTresMeses = viewModel.cumulativeSeries.first()
        // Refleja el rango nuevo completo (ambas operaciones), no arrastra ningún estado del
        // periodo anterior (DIA).
        assertEquals(listOf(5f, 6f), seriesTresMeses.map { it.cumulativeResultInR })
    }

    @Test
    fun `deseleccionar el periodo (null) vuelve a mostrar todas las operaciones`() = runTest {
        val fueraDeCualquierPeriodoPredefinido = hoy.minusDays(200).atStartOfDay(zone).toInstant().toEpochMilli()
        operationDao.insert(operacion(dateTime = fueraDeCualquierPeriodoPredefinido, resultInR = 3f))

        val viewModel = createViewModel()
        viewModel.onSelectPeriodo(PeriodoInicioFiltro.DIA, hoy)
        assertTrue(viewModel.cumulativeSeries.first().isEmpty())

        viewModel.onSelectPeriodo(null, hoy)
        val series = viewModel.cumulativeSeries.first()

        assertEquals(listOf(3f), series.map { it.cumulativeResultInR })
        assertNull(viewModel.filterState.value.selectedPeriodo)
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
