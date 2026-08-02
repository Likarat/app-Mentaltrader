package com.miguel.mentaltrader.feature.historial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import com.miguel.mentaltrader.core.data.CatalogItemDao
import com.miguel.mentaltrader.core.data.Operation
import com.miguel.mentaltrader.core.data.OperationDao
import com.miguel.mentaltrader.core.data.OperationImageDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * HU-015/HU-016: listado de Historial agrupado por mes/día y paginado (Paging 3) sobre
 * [OperationDao.pagingSourceOrderedByDateDesc], sin cargar el histórico completo a memoria. Los
 * separadores de agrupación los decide [HistorialSeparators] (lógica pura, testeada aparte).
 */
class HistorialViewModel(
    operationDao: OperationDao,
    private val operationImageDao: OperationImageDao,
    private val catalogItemDao: CatalogItemDao
) : ViewModel() {

    val historial: Flow<PagingData<HistorialListItem>> =
        Pager(config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false)) {
            operationDao.pagingSourceOrderedByDateDesc()
        }.flow
            .map { pagingData: PagingData<Operation> ->
                pagingData.insertSeparators<Operation, Any> { before, after ->
                    HistorialSeparators.between(before, after)
                }
            }
            .map { pagingData: PagingData<Any> ->
                pagingData.map { item ->
                    if (item is Operation) HistorialListItem.OperationRow(item) else item as HistorialListItem
                }
            }
            .cachedIn(viewModelScope)

    fun imagesOf(operationId: Long) = operationImageDao.getByOperationId(operationId)

    suspend fun assetNameOf(assetId: Long): String? = catalogItemDao.getById(assetId)?.name

    companion object {
        const val PAGE_SIZE = 30
    }

    class Factory(
        private val operationDao: OperationDao,
        private val operationImageDao: OperationImageDao,
        private val catalogItemDao: CatalogItemDao
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HistorialViewModel(operationDao, operationImageDao, catalogItemDao) as T
        }
    }
}
