package com.miguel.mentaltrader.feature.inicio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.miguel.mentaltrader.core.data.CatalogRankingItem
import com.miguel.mentaltrader.core.data.OperationDao
import com.miguel.mentaltrader.core.data.OperationMetricsSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * HU-026/HU-027 (sub-slice EP-004-a): pantalla de Inicio -- resumen numérico, gráfica de línea de
 * R acumulado, gráfica de barras de distribución de resultados, y ranking de emociones/errores
 * más frecuentes. EP-004-a asume "el periodo ya seleccionado" es el conjunto completo de
 * operaciones (design.md decisión #1) -- el selector real de periodo (predefinido/personalizado,
 * persistido) llega con HU-028/HU-029/HU-030 en EP-004-b/d, sin tocar el contrato de estas
 * queries (mismo patrón evolutivo que `OperationDao.pagingSourceOrderedByDateDesc` ->
 * `pagingSourceFiltered` en EP-003).
 *
 * [metricsSummary]/[emotionRanking]/[errorRanking] son pass-through directos de [OperationDao]
 * (mismo criterio que `HistorialViewModel.monthSummaries`/`assets`/etc., envueltos en
 * `flow { emitAll(...) }` para diferir la llamada real al DAO hasta que alguien colecte). La única
 * transformación real que vive en este ViewModel es [cumulativeSeries] (post-procesamiento en
 * Kotlin puro vía [ResultInRCumulativeSeries], ver su KDoc).
 */
class InicioViewModel(private val operationDao: OperationDao) : ViewModel() {

    /** HU-026 Escenario 1: tarjetas de resumen numérico -- calculado completo en SQL
     * ([OperationDao.metricsSummary]), incluye el cálculo seguro de cero operaciones (Escenario
     * 4). */
    val metricsSummary: Flow<OperationMetricsSummary> = flow {
        emitAll(operationDao.metricsSummary())
    }

    /** HU-026 Escenario 2: puntos punto-a-punto del R acumulado, listos para
     * [RAcumuladoLineChart]. */
    val cumulativeSeries: Flow<List<ResultInRCumulativeSeries.Point>> =
        operationDao.resultInROrderedByDateAsc().map { values -> ResultInRCumulativeSeries.build(values) }

    /** HU-027 Escenario 1/3/4: ranking de emociones más frecuentes (cualquiera de sus 2 roles,
     * ver KDoc de [OperationDao.emotionRanking]). Lista vacía si no hay operaciones, sin error. */
    val emotionRanking: Flow<List<CatalogRankingItem>> = flow {
        emitAll(operationDao.emotionRanking())
    }

    /** HU-027 Escenario 2/3/4: ranking de errores más frecuentes. */
    val errorRanking: Flow<List<CatalogRankingItem>> = flow {
        emitAll(operationDao.errorRanking())
    }

    class Factory(private val operationDao: OperationDao) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return InicioViewModel(operationDao) as T
        }
    }
}
