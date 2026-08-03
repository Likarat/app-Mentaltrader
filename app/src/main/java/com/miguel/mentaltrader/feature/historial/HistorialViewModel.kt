package com.miguel.mentaltrader.feature.historial

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import androidx.paging.insertSeparators
import androidx.paging.map
import com.miguel.mentaltrader.core.data.CatalogItemDao
import com.miguel.mentaltrader.core.data.MonthSummary
import com.miguel.mentaltrader.core.data.Operation
import com.miguel.mentaltrader.core.data.OperationDao
import com.miguel.mentaltrader.core.data.OperationImageDao
import com.miguel.mentaltrader.core.image.ImageProcessor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.time.YearMonth

/**
 * HU-015/HU-016: listado de Historial agrupado por mes/día y paginado (Paging 3) sobre
 * [OperationDao.pagingSourceOrderedByDateDesc], sin cargar el histórico completo a memoria. Los
 * separadores de agrupación los decide [HistorialSeparators] (lógica pura, testeada aparte).
 *
 * HU-021: también gestiona el soft-delete temporal en memoria (design.md decisión #4):
 * eliminar no toca Room de inmediato, solo agrega el id a [pendingDeleteIds] y programa el
 * borrado físico real tras [UNDO_WINDOW_MS] salvo que se cancele con [onUndoDelete].
 */
class HistorialViewModel(
    private val operationDao: OperationDao,
    private val operationImageDao: OperationImageDao,
    private val catalogItemDao: CatalogItemDao,
    /** Borra los archivos (imagen completa + miniatura cacheada) de una imagen ya eliminada de
     * Room. Abstraído como lambda (en vez de recibir un `Context` de Android directamente) para
     * poder testear en JVM puro la lógica de expiración del "deshacer" con un fake, igual que el
     * resto de los DAOs de este ViewModel -- el borrado real vía `File(appContext.filesDir, ...)`
     * se cablea en [Factory], mismo patrón de limpieza que `DataResetService`. */
    private val deleteImageFiles: (filePath: String) -> Unit = {}
) : ViewModel() {

    private val _pendingDeleteIds = MutableStateFlow<Set<Long>>(emptySet())
    /** HU-021: ids retirados del listado (aún NO borrados de Room) mientras su ventana de
     * "Deshacer" sigue activa. */
    val pendingDeleteIds: StateFlow<Set<Long>> = _pendingDeleteIds.asStateFlow()

    private val pendingDeleteJobs = mutableMapOf<Long, Job>()

    private val _deleteRequested = MutableSharedFlow<Long>(replay = 0, extraBufferCapacity = 1)
    /** HU-021: evento de una única vez por cada eliminación solicitada, para que la UI muestre el
     * snackbar de "Deshacer" (con buffer de 1 para no perder el evento si la pantalla de listado
     * no estaba compuesta en el instante exacto, p.ej. si "Eliminar" se disparó desde el detalle
     * y recién después se vuelve al listado). */
    val deleteRequested: SharedFlow<Long> = _deleteRequested.asSharedFlow()

    /** HU-017: resumen mensual (conteo, % ganadas, R acumulado), calculado en SQL vía
     * [OperationDao.monthlySummaries] -- query aparte de la paginación, no derivada de ella
     * (design.md decisión #2). Envuelta en `flow { emitAll(...) }` para diferir la llamada real
     * al DAO hasta que alguien colecte (mismo motivo que el `pagingSourceFactory` de [historial]
     * es una lambda perezosa): evita invocar `monthlySummaries()` en la sola construcción del
     * ViewModel, algo que rompería cualquier test que no la use pero sí construya el ViewModel. */
    val monthSummaries: Flow<List<MonthSummary>> = flow {
        emitAll(operationDao.monthlySummaries())
    }

    private val _collapsedMonths = MutableStateFlow<Set<YearMonth>>(emptySet())
    /** HU-017 Escenario 2: meses actualmente colapsados -- estado de UI puro (no afecta ninguna
     * query, ver [monthSummaries] y [historial]). */
    val collapsedMonths: StateFlow<Set<YearMonth>> = _collapsedMonths.asStateFlow()

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
            .map { pagingData: PagingData<HistorialListItem> ->
                // HU-021: retira del listado, de forma optimista, las filas cuya operación está
                // pendiente de borrado. Riesgo documentado en design.md (puede dejar un
                // GroupHeader huérfano si era la única operación de ese día/mes) -- aceptado,
                // pendiente de verificación instrumentada real.
                pagingData.filter { item ->
                    item !is HistorialListItem.OperationRow || item.operation.id !in pendingDeleteIds.value
                }
            }
            .cachedIn(viewModelScope)

    fun imagesOf(operationId: Long) = operationImageDao.getByOperationId(operationId)

    suspend fun assetNameOf(assetId: Long): String? = catalogItemDao.getById(assetId)?.name

    /** HU-021 Escenario 1: retira la operación del listado (soft-delete en memoria) y programa
     * su borrado físico real (fila + imágenes + archivos) para dentro de [UNDO_WINDOW_MS],
     * salvo que se cancele con [onUndoDelete] antes de que expire. */
    fun onRequestDelete(operationId: Long) {
        if (operationId in _pendingDeleteIds.value) return
        _pendingDeleteIds.value = _pendingDeleteIds.value + operationId
        pendingDeleteJobs[operationId] = viewModelScope.launch {
            delay(UNDO_WINDOW_MS)
            val images = operationImageDao.getByOperationId(operationId).first()
            images.forEach { deleteImageFiles(it.filePath) }
            operationImageDao.deleteByOperationId(operationId)
            operationDao.deleteById(operationId)
            pendingDeleteJobs.remove(operationId)
            _pendingDeleteIds.value = _pendingDeleteIds.value - operationId
        }
        _deleteRequested.tryEmit(operationId)
    }

    /** HU-021 Escenario 2: cancela el borrado físico programado y restaura la operación en el
     * listado -- nunca se tocó Room, así que "restaurar exactamente como estaba" es trivial. */
    fun onUndoDelete(operationId: Long) {
        pendingDeleteJobs.remove(operationId)?.cancel()
        _pendingDeleteIds.value = _pendingDeleteIds.value - operationId
    }

    /** HU-017 Escenario 2: alterna colapsar/expandir el grupo de [month], sin afectar el estado
     * de ningún otro mes. Estado de UI puro (no relanza ninguna query, ver design.md decisión
     * #2). */
    fun onToggleMonth(month: YearMonth) {
        _collapsedMonths.value = if (month in _collapsedMonths.value) {
            _collapsedMonths.value - month
        } else {
            _collapsedMonths.value + month
        }
    }

    companion object {
        const val PAGE_SIZE = 30

        /** Duración de la ventana de "Deshacer" tras eliminar (HU-021 Escenario 2/3). */
        const val UNDO_WINDOW_MS = 5_000L
    }

    class Factory(
        private val operationDao: OperationDao,
        private val operationImageDao: OperationImageDao,
        private val catalogItemDao: CatalogItemDao,
        private val appContext: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HistorialViewModel(
                operationDao,
                operationImageDao,
                catalogItemDao,
                deleteImageFiles = { filePath ->
                    File(appContext.filesDir, filePath).delete()
                    File(appContext.filesDir, ImageProcessor.thumbnailPathFor(filePath)).delete()
                }
            ) as T
        }
    }
}
