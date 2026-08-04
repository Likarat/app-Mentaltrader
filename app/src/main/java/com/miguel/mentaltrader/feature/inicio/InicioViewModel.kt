package com.miguel.mentaltrader.feature.inicio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.miguel.mentaltrader.core.data.CatalogRankingItem
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

/**
 * HU-026/HU-027 (sub-slice EP-004-a) + HU-028 (sub-slice EP-004-b): pantalla de Inicio -- resumen
 * numérico, gráfica de línea de R acumulado, gráfica de barras de distribución de resultados,
 * ranking de emociones/errores más frecuentes, y el selector de periodo predefinido que recalcula
 * las 4 queries anteriores sobre un rango de fechas real (en vez del conjunto completo de
 * operaciones que EP-004-a usaba como alcance provisional, design.md decisión #2).
 *
 * [metricsSummary]/[emotionRanking]/[errorRanking]/[cumulativeSeries] se reconstruyen vía
 * `flatMapLatest` sobre [filterState] cada vez que cambia (HU-028 Escenario 2: recalcula desde
 * cero, sin arrastrar datos del periodo anterior -- `flatMapLatest` cancela la colección anterior
 * antes de suscribirse a la nueva). Con [InicioFilterState] en su valor por defecto (ningún
 * periodo seleccionado, `dateFrom`/`dateTo` ambos `null`) el comportamiento es idéntico al de
 * EP-004-a (todas las operaciones). La única transformación real que vive en este ViewModel (más
 * allá de pasar `dateFrom`/`dateTo` al DAO) es [cumulativeSeries] (post-procesamiento en Kotlin
 * puro vía [ResultInRCumulativeSeries], ver su KDoc).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InicioViewModel(private val operationDao: OperationDao) : ViewModel() {

    private val _filterState = MutableStateFlow(InicioFilterState())
    /** HU-028: periodo de Inicio actualmente seleccionado (`null` == sin selección, todas las
     * operaciones). Sin persistencia todavía (HU-030/EP-004-d). */
    val filterState: StateFlow<InicioFilterState> = _filterState.asStateFlow()

    /** HU-028 Escenario 1: selecciona un periodo predefinido, resolviendo `dateFrom`/`dateTo`
     * reales vía [PeriodoInicioFiltroRange.rangeFor] (lógica pura ya testeada aparte en
     * `PeriodoInicioFiltroRangeTest`). [periodo] `null` quita la selección (vuelve a "todas las
     * operaciones", mismo criterio que `HistorialViewModel.onSelectPeriodo(null)`). [today]
     * explícito (no `LocalDate.now()` directo) para que la resolución sea determinista/testeable,
     * igual que `HistorialViewModel.onSelectPeriodo`. */
    fun onSelectPeriodo(periodo: PeriodoInicioFiltro?, today: LocalDate = LocalDate.now()) {
        _filterState.value = if (periodo == null) {
            InicioFilterState()
        } else {
            val range = PeriodoInicioFiltroRange.rangeFor(periodo, today)
            InicioFilterState(selectedPeriodo = periodo, dateFrom = range.first, dateTo = range.second)
        }
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

    /** HU-027 Escenario 1/3/4 / HU-028 Escenario 1: ranking de emociones más frecuentes
     * (cualquiera de sus 2 roles, ver KDoc de [OperationDao.emotionRanking]) dentro del periodo
     * seleccionado. Lista vacía si no hay operaciones en ese rango, sin error. */
    val emotionRanking: Flow<List<CatalogRankingItem>> = filterState.flatMapLatest { filter ->
        operationDao.emotionRanking(filter.dateFrom, filter.dateTo)
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

    class Factory(private val operationDao: OperationDao) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return InicioViewModel(operationDao) as T
        }
    }
}
