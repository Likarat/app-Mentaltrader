package com.miguel.mentaltrader.feature.inicio

import com.miguel.mentaltrader.core.data.InicioFilterRepository
import com.miguel.mentaltrader.core.data.InicioFilterSnapshot
import com.miguel.mentaltrader.core.data.Operation
import com.miguel.mentaltrader.core.model.Direction
import com.miguel.mentaltrader.core.model.ResultType
import com.miguel.mentaltrader.testutil.FakeInMemoryPreferencesDataStore
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
import org.junit.Assert.assertFalse
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

    // HU-030 (sub-slice EP-004-d): InicioViewModel ahora requiere un InicioFilterRepository real --
    // se le da uno de verdad respaldado por un DataStore EN MEMORIA (no un archivo real en disco,
    // ver InicioFilterRepositoryTest aparte para la persistencia real). Mismo patrón que
    // HistorialViewModelTest.createViewModel (HU-024/EP-003).
    private fun createViewModel(
        filterRepository: InicioFilterRepository = InicioFilterRepository(FakeInMemoryPreferencesDataStore())
    ) = InicioViewModel(operationDao, filterRepository)

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
        assertEquals(esperado?.first, viewModel.filterState.value.dateFrom)
        assertEquals(esperado?.second, viewModel.filterState.value.dateTo)
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

    // ---- HU-031 (sub-slice EP-004-c): estado vacío -- totalOperationCount ----

    // HU-031 Escenario 1: `totalOperationCount` (pass-through de `OperationDao.countAll`, mismo
    // criterio que `HistorialViewModel.totalOperationCount`, HU-025/EP-003) es lo que distingue
    // "primera vez" (0) de "hay operaciones pero el periodo no arroja ninguna" -- ver
    // `InicioEmptyState.resolve`, testeado aparte en `InicioEmptyStateTest`.
    @Test
    fun `totalOperationCount es 0 cuando no hay ninguna operacion registrada`() = runTest {
        val viewModel = createViewModel()

        assertEquals(0, viewModel.totalOperationCount.first())
    }

    @Test
    fun `totalOperationCount refleja el conteo real tras insertar operaciones, sin importar el periodo seleccionado`() = runTest {
        operationDao.insert(operacion(dateTime = 1_000L, resultInR = 1f))
        operationDao.insert(operacion(dateTime = 2_000L, resultInR = -1f))

        val viewModel = createViewModel()
        viewModel.onSelectPeriodo(PeriodoInicioFiltro.DIA, hoy)

        // totalOperationCount NO se filtra por el periodo seleccionado (a diferencia de
        // metricsSummary.operationCount) -- es el conteo GLOBAL, mismo criterio que
        // `HistorialViewModel.totalOperationCount`.
        assertEquals(2, viewModel.totalOperationCount.first())
    }

    // ---- HU-029 (sub-slice EP-004-d): rango de fechas personalizado ----
    // Escenario 1 (mostrar el calendario al elegir "Personalizado") es una garantía ESTRUCTURAL de
    // InicioScreen (mismo criterio que HU-028 Escenario 3, tasks.md 2.4): el bloque de campos de
    // fecha solo se compone cuando `filterState.selectedPeriodo == PeriodoInicioFiltro.PERSONALIZADO`
    // -- verificado por lectura de código, confirmación visual real diferida al instrumentado.

    // HU-029 Escenario 2: confirmar un rango personalizado recalcula las métricas (aquí,
    // cumulativeSeries -- misma única transformación real del ViewModel que ya verifican los tests
    // de onSelectPeriodo de HU-028) limitadas a ese rango.
    @Test
    fun `onCustomDateRange recalcula la serie usando solo las operaciones dentro del rango confirmado (HU-029 Escenario 2)`() = runTest {
        val dentroDelRango = hoy.atStartOfDay(zone).toInstant().toEpochMilli() + 1_000
        val fueraDelRango = hoy.minusDays(200).atStartOfDay(zone).toInstant().toEpochMilli()
        operationDao.insert(operacion(dateTime = dentroDelRango, resultInR = 4f))
        operationDao.insert(operacion(dateTime = fueraDelRango, resultInR = 100f))

        val viewModel = createViewModel()
        viewModel.onSelectPeriodo(PeriodoInicioFiltro.PERSONALIZADO, hoy)
        viewModel.onCustomDateRange(
            from = hoy.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
            to = hoy.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        )

        val series = viewModel.cumulativeSeries.first()

        assertEquals(listOf(4f), series.map { it.cumulativeResultInR })
        assertEquals(PeriodoInicioFiltro.PERSONALIZADO, viewModel.filterState.value.selectedPeriodo)
        assertFalse(viewModel.customRangeError.value)
    }

    // HU-029 Escenario 3 (edge): fin anterior a inicio -- el sistema impide confirmar (no aplica el
    // rango) y marca el error para que la UI lo muestre.
    @Test
    fun `onCustomDateRange con fin anterior a inicio no aplica el rango y marca customRangeError (HU-029 Escenario 3)`() = runTest {
        val viewModel = createViewModel()
        viewModel.onSelectPeriodo(PeriodoInicioFiltro.SEMANA, hoy)
        val filterAntesDelIntento = viewModel.filterState.value

        viewModel.onCustomDateRange(from = 2_000L, to = 1_000L)

        assertTrue(viewModel.customRangeError.value)
        assertEquals(filterAntesDelIntento, viewModel.filterState.value)
    }

    @Test
    fun `onCustomDateRange con un rango valido tras un intento invalido limpia customRangeError`() = runTest {
        val viewModel = createViewModel()
        viewModel.onCustomDateRange(from = 2_000L, to = 1_000L)
        assertTrue(viewModel.customRangeError.value)

        viewModel.onCustomDateRange(from = 1_000L, to = 2_000L)

        assertFalse(viewModel.customRangeError.value)
        assertEquals(1_000L, viewModel.filterState.value.dateFrom)
        assertEquals(2_000L, viewModel.filterState.value.dateTo)
        assertEquals(PeriodoInicioFiltro.PERSONALIZADO, viewModel.filterState.value.selectedPeriodo)
    }

    // ---- HU-030 (sub-slice EP-004-d): persistencia del filtro de periodo entre sesiones ----

    @Test
    fun `onSelectPeriodo persiste el nuevo filterState en el repository`() = runTest {
        val repository = InicioFilterRepository(FakeInMemoryPreferencesDataStore())
        val viewModel = createViewModel(filterRepository = repository)

        viewModel.onSelectPeriodo(PeriodoInicioFiltro.SEMANA, hoy)

        assertEquals("SEMANA", repository.load().periodName)
    }

    @Test
    fun `onCustomDateRange valido persiste periodName y el rango en el repository`() = runTest {
        val repository = InicioFilterRepository(FakeInMemoryPreferencesDataStore())
        val viewModel = createViewModel(filterRepository = repository)

        viewModel.onSelectPeriodo(PeriodoInicioFiltro.PERSONALIZADO, hoy)
        viewModel.onCustomDateRange(from = 1_000L, to = 2_000L)

        val snapshot = repository.load()
        assertEquals("PERSONALIZADO", snapshot.periodName)
        assertEquals(1_000L, snapshot.customRangeStart)
        assertEquals(2_000L, snapshot.customRangeEnd)
    }

    @Test
    fun `onCustomDateRange invalido no persiste ningun cambio en el repository`() = runTest {
        val repository = InicioFilterRepository(FakeInMemoryPreferencesDataStore())
        repository.save(InicioFilterSnapshot(periodName = "SEMANA"))
        val viewModel = createViewModel(filterRepository = repository)

        viewModel.onCustomDateRange(from = 2_000L, to = 1_000L)

        assertEquals("SEMANA", repository.load().periodName)
    }

    // HU-030 Escenario 1: al construirse, el ViewModel restaura el último periodo predefinido
    // persistido -- wiring real entre InicioViewModel.init e InicioFilterRepository (no un fake del
    // repositorio: se usa la clase real con un DataStore en memoria pre-poblado).
    @Test
    fun `al construirse restaura un periodo predefinido persistido (HU-030 Escenario 1)`() = runTest {
        val repository = InicioFilterRepository(FakeInMemoryPreferencesDataStore())
        repository.save(InicioFilterSnapshot(periodName = "SEMANA"))

        val viewModel = createViewModel(filterRepository = repository)

        val esperado = PeriodoInicioFiltroRange.rangeFor(PeriodoInicioFiltro.SEMANA, LocalDate.now())
        assertEquals(PeriodoInicioFiltro.SEMANA, viewModel.filterState.value.selectedPeriodo)
        assertEquals(esperado?.first, viewModel.filterState.value.dateFrom)
        assertEquals(esperado?.second, viewModel.filterState.value.dateTo)
    }

    // HU-030 Escenario 2 (edge): un rango personalizado persistido se restaura con sus fechas
    // EXACTAS (no recalculado relativo a "hoy", a diferencia de un periodo predefinido).
    @Test
    fun `al construirse restaura un rango personalizado persistido con sus fechas exactas (HU-030 Escenario 2)`() = runTest {
        val repository = InicioFilterRepository(FakeInMemoryPreferencesDataStore())
        repository.save(InicioFilterSnapshot(periodName = "PERSONALIZADO", customRangeStart = 1_000L, customRangeEnd = 2_000L))

        val viewModel = createViewModel(filterRepository = repository)

        assertEquals(PeriodoInicioFiltro.PERSONALIZADO, viewModel.filterState.value.selectedPeriodo)
        assertEquals(1_000L, viewModel.filterState.value.dateFrom)
        assertEquals(2_000L, viewModel.filterState.value.dateTo)
    }

    // HU-030 Escenario 3 (edge): sin ningún filtro guardado previamente, el sistema aplica el
    // periodo por defecto razonable de Inicio -- "todas las operaciones" (ver KDoc de
    // InicioFilterState.fromSnapshot, design.md decisión #8), sin error ni excepción.
    @Test
    fun `al construirse sin ningun filtro guardado previamente aplica el periodo por defecto sin error (HU-030 Escenario 3)`() = runTest {
        val repository = InicioFilterRepository(FakeInMemoryPreferencesDataStore())

        val viewModel = createViewModel(filterRepository = repository)

        assertNull(viewModel.filterState.value.selectedPeriodo)
        assertNull(viewModel.filterState.value.dateFrom)
        assertNull(viewModel.filterState.value.dateTo)
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
