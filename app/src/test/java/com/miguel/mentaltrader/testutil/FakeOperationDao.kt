package com.miguel.mentaltrader.testutil

import androidx.paging.PagingSource
import com.miguel.mentaltrader.core.data.CatalogRankingItem
import com.miguel.mentaltrader.core.data.MonthSummary
import com.miguel.mentaltrader.core.data.Operation
import com.miguel.mentaltrader.core.data.OperationDao
import com.miguel.mentaltrader.core.data.OperationMetricsSummary
import com.miguel.mentaltrader.core.model.ResultType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Fake de OperationDao compartido por los tests unitarios JVM (sin Room). [getAllOrderedByDateDesc]
 * deriva de un único [MutableStateFlow] de respaldo, mismo patrón que [FakeCatalogItemDao].
 * [pagingSourceOrderedByDateDesc]/[monthlySummaries]/[pagingSourceFiltered]/[metricsSummary]/
 * [emotionRanking]/[errorRanking] (EP-003 Historial, EP-004 Inicio) requieren Room real
 * (PagingSource/SQL agregado) y no se ejercitan contra este fake -- lanzan
 * `UnsupportedOperationException`, mismo criterio que los fakes locales de `feature/historial`.
 * [resultInROrderedByDateAsc] SÍ tiene una implementación real (proyección simple sobre la misma
 * lista en memoria, sin agregación): `InicioViewModel` la transforma en Kotlin puro
 * (`ResultInRCumulativeSeries`), y esa transformación es justamente lo que se testea contra este
 * fake (mismo criterio que otros campos "pass-through" de este archivo).
 */
class FakeOperationDao : OperationDao {
    private val allOperations = MutableStateFlow<List<Operation>>(emptyList())
    private var nextId = 1L

    val inserted: List<Operation> get() = allOperations.value

    /** HU-020: operaciones actualizadas vía [update] (no [insert]), en orden de llamada. */
    val updated = mutableListOf<Operation>()

    override suspend fun insert(operation: Operation): Long {
        val withId = operation.copy(id = nextId++)
        allOperations.value += withId
        return withId.id
    }

    override fun getAllOrderedByDateDesc(): Flow<List<Operation>> =
        allOperations.map { list -> list.sortedByDescending { it.dateTime } }

    override fun pagingSourceOrderedByDateDesc(): PagingSource<Int, Operation> =
        throw UnsupportedOperationException("Requiere Room real (PagingSource), no ejercitado contra este fake")

    override fun countAll(): Flow<Int> = allOperations.map { it.size }

    override suspend fun getById(id: Long): Operation? = allOperations.value.find { it.id == id }

    override suspend fun update(operation: Operation) {
        updated += operation
        allOperations.value = allOperations.value.map { if (it.id == operation.id) operation else it }
    }

    override suspend fun deleteById(id: Long) {
        allOperations.value = allOperations.value.filterNot { it.id == id }
    }

    override fun monthlySummaries(): Flow<List<MonthSummary>> =
        throw UnsupportedOperationException("Requiere Room real (cálculo agregado en SQL), no ejercitado contra este fake")

    override fun pagingSourceFiltered(
        assetId: Long?,
        result: ResultType?,
        errorId: Long?,
        emotionBeforeId: Long?,
        emotionAfterId: Long?,
        dateFrom: Long?,
        dateTo: Long?,
        searchText: String?
    ): PagingSource<Int, Operation> =
        throw UnsupportedOperationException("Requiere Room real (PagingSource filtrado), no ejercitado contra este fake")

    override suspend fun deleteAll() {
        allOperations.value = emptyList()
    }

    override suspend fun countUsageOfCatalogItem(id: Long): Int =
        allOperations.value.count {
            it.assetId == id || it.emotionBeforeId == id || it.emotionAfterId == id || it.errorId == id
        }

    override fun metricsSummary(): Flow<OperationMetricsSummary> =
        throw UnsupportedOperationException("Requiere Room real (agregación SQL), no ejercitado contra este fake")

    override fun resultInROrderedByDateAsc(): Flow<List<Float?>> =
        allOperations.map { list -> list.sortedBy { it.dateTime }.map { it.resultInR } }

    override fun emotionRanking(): Flow<List<CatalogRankingItem>> =
        throw UnsupportedOperationException("Requiere Room real (JOIN/GROUP BY agregado), no ejercitado contra este fake")

    override fun errorRanking(): Flow<List<CatalogRankingItem>> =
        throw UnsupportedOperationException("Requiere Room real (JOIN/GROUP BY agregado), no ejercitado contra este fake")
}
