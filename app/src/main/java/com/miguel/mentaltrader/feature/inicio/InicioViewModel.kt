package com.miguel.mentaltrader.feature.inicio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.miguel.mentaltrader.core.data.CatalogRankingItem
import com.miguel.mentaltrader.core.data.InicioFilterRepository
import com.miguel.mentaltrader.core.data.OperationDao
import com.miguel.mentaltrader.core.data.OperationMetricsSummary
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * HU-026/HU-027 (sub-slice EP-004-a) + HU-028 (sub-slice EP-004-b) + HU-029/HU-030 (sub-slice
 * EP-004-d): pantalla de Inicio -- resumen numérico, gráfica de línea de R acumulado, gráfica de
 * barras de distribución de resultados, ranking de emociones/errores más frecuentes, y el selector
 * de periodo (predefinido + personalizado, persistido entre sesiones) que recalcula las 4 queries
 * anteriores sobre un rango de fechas real (en vez del conjunto completo de operaciones que
 * EP-004-a usaba como alcance provisional, design.md decisión #2).
 *
 * [metricsSummary]/[emotionRanking]/[errorRanking]/[cumulativeSeries] se reconstruyen vía
 * `flatMapLatest` sobre [filterState] cada vez que cambia (HU-028 Escenario 2: recalcula desde
 * cero, sin arrastrar datos del periodo anterior -- `flatMapLatest` cancela la colección anterior
 * antes de suscribirse a la nueva). Con [InicioFilterState] en su valor por defecto (ningún
 * periodo seleccionado, `dateFrom`/`dateTo` ambos `null`) el comportamiento es idéntico al de
 * EP-004-a (todas las operaciones). La única transformación real que vive en este ViewModel (más
 * allá de pasar `dateFrom`/`dateTo` al DAO) es [cumulativeSeries] (post-procesamiento en Kotlin
 * puro vía [ResultInRCumulativeSeries], ver su KDoc).
 *
 * HU-030: [filterState] se restaura desde [filterRepository] al construirse (Escenario 1/2/3, ver
 * `init`) y se persiste de nuevo tras cada cambio de periodo/rango personalizado (vía
 * [persistFilterState]), mismo patrón mecánico que `HistorialViewModel`/HU-024.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InicioViewModel(
    private val operationDao: OperationDao,
    /** HU-030: persiste/restaura el último [InicioFilterState] aplicado (DataStore Preferences,
     * design.md decisión #8, archivo/claves propias de Inicio). */
    private val filterRepository: InicioFilterRepository
) : ViewModel() {

    private val _filterState = MutableStateFlow(InicioFilterState())
    /** HU-028/HU-029/HU-030: periodo de Inicio actualmente seleccionado (`null` == sin selección,
     * todas las operaciones). */
    val filterState: StateFlow<InicioFilterState> = _filterState.asStateFlow()

    private val _customRangeError = MutableStateFlow(false)
    /** HU-029 Escenario 3: `true` mientras el último intento de confirmar un rango personalizado
     * tenía la fecha de fin anterior a la de inicio -- `InicioScreen` muestra el mensaje de error y
     * NO se aplicó ningún cambio de filtro. Se limpia (`false`) apenas se confirma un rango válido
     * (o cualquier otro cambio de periodo). */
    val customRangeError: StateFlow<Boolean> = _customRangeError.asStateFlow()

    init {
        // HU-030 Escenario 1/2/3: restaura el último filtro persistido (o el periodo por defecto
        // razonable -- "todas las operaciones", ver InicioFilterState.fromSnapshot -- si nunca se
        // guardó ninguno) apenas se construye el ViewModel, mismo criterio/riesgo aceptado que
        // HistorialViewModel.init (HU-024): `filterRepository.load()` es `suspend`, corre en su
        // propia corrutina de `viewModelScope`, sin bloquear la construcción.
        viewModelScope.launch {
            _filterState.value = InicioFilterState.fromSnapshot(filterRepository.load())
        }
    }

    /** HU-030: persiste el [InicioFilterState] COMPLETO vigente en ese instante -- se llama al
     * final de [onSelectPeriodo]/[onCustomDateRange], en una corrutina propia de `viewModelScope`
     * para no bloquear al llamador (evento de UI/recomposición), mismo patrón que
     * `HistorialViewModel.persistFilterState`. */
    private fun persistFilterState() {
        val snapshot = _filterState.value.toSnapshot()
        viewModelScope.launch { filterRepository.save(snapshot) }
    }

    /** HU-028 Escenario 1: selecciona un periodo predefinido, resolviendo `dateFrom`/`dateTo`
     * reales vía [PeriodoInicioFiltroRange.rangeFor] (lógica pura ya testeada aparte en
     * `PeriodoInicioFiltroRangeTest`). [periodo] `null` quita la selección (vuelve a "todas las
     * operaciones", mismo criterio que `HistorialViewModel.onSelectPeriodo(null)`).
     * [PeriodoInicioFiltro.PERSONALIZADO] (HU-029) no resuelve rango propio aquí -- preserva
     * `dateFrom`/`dateTo` vigentes (si los había) y espera [onCustomDateRange], mismo criterio que
     * `HistorialViewModel.onSelectPeriodo`. [today] explícito (no `LocalDate.now()` directo) para
     * que la resolución sea determinista/testeable. Persiste el nuevo filtro (HU-030). */
    fun onSelectPeriodo(periodo: PeriodoInicioFiltro?, today: LocalDate = LocalDate.now()) {
        _filterState.value = when {
            periodo == null -> InicioFilterState()
            periodo == PeriodoInicioFiltro.PERSONALIZADO -> _filterState.value.copy(selectedPeriodo = periodo)
            else -> {
                val range = PeriodoInicioFiltroRange.rangeFor(periodo, today)
                InicioFilterState(selectedPeriodo = periodo, dateFrom = range?.first, dateTo = range?.second)
            }
        }
        persistFilterState()
    }

    /** HU-029 Escenario 2/3: rango de fecha personalizado elegido por el usuario (campos de fecha
     * reales en `InicioScreen`, tras elegir [PeriodoInicioFiltro.PERSONALIZADO]) -- mismo criterio
     * que `HistorialViewModel.onCustomDateRange`. Escenario 3 (edge): si ambas fechas son válidas
     * pero [to] es anterior a [from], el sistema NO aplica el rango (mantiene el filtro vigente
     * hasta ese momento) y marca [customRangeError] para que la UI muestre el mensaje de rango
     * inválido -- no se persiste ningún cambio en ese caso. */
    fun onCustomDateRange(from: Long?, to: Long?) {
        if (from != null && to != null && to < from) {
            _customRangeError.value = true
            return
        }
        _customRangeError.value = false
        _filterState.value = _filterState.value.copy(selectedPeriodo = PeriodoInicioFiltro.PERSONALIZADO, dateFrom = from, dateTo = to)
        persistFilterState()
    }

    /** HU-026 Escenario 1 / HU-028 Escenario 1: tarjetas de resumen numérico -- calculado completo
     * en SQL ([OperationDao.metricsSummary]), incluye el cálculo seguro de cero operaciones
     * (Escenario 4) y el recálculo real al cambiar de periodo. */
    val metricsSummary: Flow<OperationMetricsSummary> = filterState.flatMapLatest { filter ->
        operationDao.metricsSummary(filter.dateFrom, filter.dateTo)
    }

    /** HU-026 Escenario 2 / HU-028 Escenario 1: puntos punto-a-punto del R acumulado, listos para
     * [RAcumuladoLineChart], recalculados sobre el periodo seleccionado. */
    val cumulativeSeries: Flow<List<ResultInRCumulativeSeries.Point>> = filterState.flatMapLatest { filter ->
        operationDao.resultInROrderedByDateAsc(filter.dateFrom, filter.dateTo).map { values -> ResultInRCumulativeSeries.build(values) }
    }

    /** Fix: ranking de emociones "antes" más frecuentes dentro del periodo seleccionado -- separado
     * del de "después" (antes combinaban ambos roles en un solo ranking, ver KDoc de
     * [OperationDao.emotionBeforeRanking]). Lista vacía si no hay operaciones en ese rango, sin
     * error. */
    val emotionBeforeRanking: Flow<List<CatalogRankingItem>> = filterState.flatMapLatest { filter ->
        operationDao.emotionBeforeRanking(filter.dateFrom, filter.dateTo)
    }

    /** Fix: ranking de emociones "después" más frecuentes dentro del periodo seleccionado, misma
     * semántica que [emotionBeforeRanking] pero sobre [OperationDao.emotionAfterRanking]. */
    val emotionAfterRanking: Flow<List<CatalogRankingItem>> = filterState.flatMapLatest { filter ->
        operationDao.emotionAfterRanking(filter.dateFrom, filter.dateTo)
    }

    /** HU-027 Escenario 2/3/4 / HU-028 Escenario 1: ranking de errores más frecuentes dentro del
     * periodo seleccionado. */
    val errorRanking: Flow<List<CatalogRankingItem>> = filterState.flatMapLatest { filter ->
        operationDao.errorRanking(filter.dateFrom, filter.dateTo)
    }

    /** HU-031 (sub-slice EP-004-c): conteo GLOBAL de operaciones registradas, SIN filtrar por
     * periodo (a diferencia de `metricsSummary.operationCount`) -- mismo pass-through directo de
     * `OperationDao.countAll` que `HistorialViewModel.totalOperationCount` (HU-025/EP-003). Junto
     * con `metricsSummary.operationCount` (conteo dentro del periodo), es lo que
     * `InicioEmptyState.resolve` usa para distinguir "primera vez" de "sin datos para el periodo
     * seleccionado". */
    val totalOperationCount: Flow<Int> = operationDao.countAll()

    class Factory(
        private val operationDao: OperationDao,
        private val filterRepository: InicioFilterRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return InicioViewModel(operationDao, filterRepository) as T
        }
    }
}
