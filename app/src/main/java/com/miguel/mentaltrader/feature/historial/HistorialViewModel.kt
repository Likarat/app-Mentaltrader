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
import com.miguel.mentaltrader.core.data.CatalogItem
import com.miguel.mentaltrader.core.data.CatalogItemDao
import com.miguel.mentaltrader.core.data.HistorialFilterRepository
import com.miguel.mentaltrader.core.data.MonthSummary
import com.miguel.mentaltrader.core.data.Operation
import com.miguel.mentaltrader.core.data.OperationDao
import com.miguel.mentaltrader.core.data.OperationImageDao
import com.miguel.mentaltrader.core.image.ImageProcessor
import com.miguel.mentaltrader.core.model.CatalogType
import com.miguel.mentaltrader.core.model.ResultType
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

/**
 * HU-015/HU-016: listado de Historial agrupado por mes/día y paginado (Paging 3) sobre
 * [OperationDao.pagingSourceOrderedByDateDesc], sin cargar el histórico completo a memoria. Los
 * separadores de agrupación los decide [HistorialSeparators] (lógica pura, testeada aparte).
 *
 * HU-021: también gestiona el soft-delete temporal en memoria (design.md decisión #4):
 * eliminar no toca Room de inmediato, solo agrega el id a [pendingDeleteIds] y programa el
 * borrado físico real tras [UNDO_WINDOW_MS] salvo que se cancele con [onUndoDelete].
 *
 * HU-022/HU-023: también gestiona [filterState] (etiqueta + fecha/periodo + búsqueda de texto,
 * combinados con AND) -- cada cambio reconstruye el `Pager` con
 * [OperationDao.pagingSourceFiltered] vía `flatMapLatest` (design.md decisión #5: el filtrado
 * corre en SQL, no en memoria, para que la paginación siga sin cargar el histórico completo).
 *
 * HU-025: también expone [totalOperationCount], para que `HistorialScreen` distinga "primera vez"
 * de "sin resultados de filtro" cuando el listado paginado queda vacío (ver [HistorialEmptyState]).
 *
 * HU-024: [filterState] se restaura desde [HistorialFilterRepository] al construirse (Escenario
 * 1/2/3, ver `init`) y se persiste de nuevo tras cada uno de los 9 setters de filtro/búsqueda
 * (vía [persistFilterState]).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HistorialViewModel(
    private val operationDao: OperationDao,
    private val operationImageDao: OperationImageDao,
    private val catalogItemDao: CatalogItemDao,
    /** HU-024: persiste/restaura el último [HistorialFilterState] aplicado (DataStore Preferences,
     * design.md decisión #6). */
    private val filterRepository: HistorialFilterRepository,
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

    /** HU-025: total real de operaciones, sin importar el filtro activo -- HistorialScreen lo
     * combina con `items.itemCount` y `filterState.isEmpty` (vía [HistorialEmptyState.resolve])
     * para distinguir "primera vez" (Escenario 1) de "sin resultados de filtro" (Escenario 2). */
    val totalOperationCount: Flow<Int> = operationDao.countAll()

    private val _collapsedMonths = MutableStateFlow<Set<YearMonth>>(emptySet())
    /** HU-017 Escenario 2: meses actualmente colapsados -- estado de UI puro (no afecta ninguna
     * query, ver [monthSummaries] y [historial]). */
    val collapsedMonths: StateFlow<Set<YearMonth>> = _collapsedMonths.asStateFlow()

    private val _filterState = MutableStateFlow(HistorialFilterState())
    /** HU-022/HU-023: filtro + búsqueda actualmente activos (todos los campos nulos == "Limpiar
     * filtros" ya aplicado, HU-022 Escenario 4). */
    val filterState: StateFlow<HistorialFilterState> = _filterState.asStateFlow()

    init {
        // HU-024 Escenario 1/2/3: restaura el último filtro persistido (o el estado por defecto
        // `HistorialFilterState()` si nunca se guardó ninguno) apenas se construye el ViewModel --
        // `filterRepository.load()` es `suspend` (lee DataStore real), así que corre en su propia
        // corrutina de `viewModelScope`, sin bloquear la construcción. Riesgo aceptado y
        // documentado (ventana angosta, no verificada aún de forma instrumentada): si algún setter
        // de filtro se llamara en la fracción de segundo entre construir el ViewModel y que esta
        // restauración termine de leer disco, esta asignación lo pisaría al completarse -- en la
        // práctica el usuario no puede tocar un filtro antes de que la pantalla siquiera termine de
        // componerse, pero queda anotado igual que otros riesgos ya documentados en este archivo
        // (p.ej. el GroupHeader huérfano de HU-021).
        viewModelScope.launch {
            _filterState.value = HistorialFilterState.fromSnapshot(filterRepository.load())
        }
    }

    /** HU-024: persiste el [HistorialFilterState] COMPLETO vigente en ese instante -- se llama al
     * final de cada uno de los 9 setters de filtro/búsqueda (HU-022/HU-023), en una corrutina
     * propia de `viewModelScope` para no bloquear al llamador (evento de UI/recomposición). */
    private fun persistFilterState() {
        val snapshot = _filterState.value.toSnapshot()
        viewModelScope.launch { filterRepository.save(snapshot) }
    }

    /** HU-022 Escenario 1: opciones reales para los selectores de filtro por etiqueta (mismo
     * catálogo de EP-002 que ya usa `OperationFormViewModel` para el formulario). */
    val assets: Flow<List<CatalogItem>> = catalogItemDao.getByType(CatalogType.ASSET)
    val emotions: Flow<List<CatalogItem>> = catalogItemDao.getByType(CatalogType.EMOTION)
    val errorsCatalog: Flow<List<CatalogItem>> = catalogItemDao.getByType(CatalogType.ERROR)

    val historial: Flow<PagingData<HistorialListItem>> =
        filterState
            .flatMapLatest { filter ->
                Pager(config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false)) {
                    operationDao.pagingSourceFiltered(
                        assetId = filter.assetId,
                        result = filter.result,
                        errorId = filter.errorId,
                        emotionBeforeId = filter.emotionBeforeId,
                        emotionAfterId = filter.emotionAfterId,
                        dateFrom = filter.dateFrom,
                        dateTo = filter.dateTo,
                        // HU-023: los comodines `%` van del lado del llamador (design.md
                        // decisión #5), no de la query -- así `pagingSourceFiltered` sigue siendo
                        // una @Query de Room sin lógica condicional embebida en el parámetro.
                        searchText = filter.searchText?.trim()?.takeIf { it.isNotEmpty() }?.let { "%$it%" }
                    )
                }.flow
            }
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

    // ---- HU-022/HU-023: filtro + búsqueda (todos combinados con AND, ver HistorialFilterMatcher) ----

    /** HU-022 Escenario 1: filtrar por Activo (`null` quita ese criterio). */
    fun onFilterAsset(assetId: Long?) {
        _filterState.value = _filterState.value.copy(assetId = assetId)
        persistFilterState()
    }

    /** HU-022 Escenario 1: filtrar por Resultado (`null` quita ese criterio). */
    fun onFilterResult(result: ResultType?) {
        _filterState.value = _filterState.value.copy(result = result)
        persistFilterState()
    }

    /** HU-022 Escenario 1: filtrar por Error (`null` quita ese criterio). */
    fun onFilterError(errorId: Long?) {
        _filterState.value = _filterState.value.copy(errorId = errorId)
        persistFilterState()
    }

    /** HU-022 Escenario 1: filtrar por Emoción antes de operar (`null` quita ese criterio). */
    fun onFilterEmotionBefore(emotionId: Long?) {
        _filterState.value = _filterState.value.copy(emotionBeforeId = emotionId)
        persistFilterState()
    }

    /** HU-022 Escenario 1: filtrar por Emoción después de operar (`null` quita ese criterio). */
    fun onFilterEmotionAfter(emotionId: Long?) {
        _filterState.value = _filterState.value.copy(emotionAfterId = emotionId)
        persistFilterState()
    }

    /** HU-022 Escenario 2: selecciona un periodo predefinido (resuelve `dateFrom`/`dateTo` reales
     * vía [PeriodoFiltroRange], lógica pura ya testeada aparte) o [PeriodoFiltro.PERSONALIZADO]
     * (no fija rango propio -- espera [onCustomDateRange]). `null` quita el filtro de fecha. */
    fun onSelectPeriodo(periodo: PeriodoFiltro?, today: LocalDate = LocalDate.now()) {
        _filterState.value = when {
            periodo == null -> _filterState.value.copy(selectedPeriodo = null, dateFrom = null, dateTo = null)
            periodo == PeriodoFiltro.PERSONALIZADO -> _filterState.value.copy(selectedPeriodo = periodo)
            else -> {
                val range = PeriodoFiltroRange.rangeFor(periodo, today)
                _filterState.value.copy(selectedPeriodo = periodo, dateFrom = range?.first, dateTo = range?.second)
            }
        }
        persistFilterState()
    }

    /** HU-022 Escenario 2: rango de fecha personalizado elegido por el usuario (selector de fecha
     * real en HistorialScreen, tras elegir [PeriodoFiltro.PERSONALIZADO]). */
    fun onCustomDateRange(from: Long?, to: Long?) {
        _filterState.value = _filterState.value.copy(selectedPeriodo = PeriodoFiltro.PERSONALIZADO, dateFrom = from, dateTo = to)
        persistFilterState()
    }

    /** HU-023 Escenario 1/2: texto de búsqueda libre, combinado con AND junto a los demás
     * filtros ya activos (ver [HistorialFilterMatcher] para la semántica exacta). */
    fun onSearchTextChanged(text: String?) {
        _filterState.value = _filterState.value.copy(searchText = text)
        persistFilterState()
    }

    /** HU-022 Escenario 4: "Limpiar filtros" -- remueve TODOS los criterios (etiqueta, fecha,
     * búsqueda) de una sola vez y vuelve al listado completo agrupado por mes. También persiste
     * ese estado "vacío" (HU-024): si el usuario limpia los filtros y cierra la app, reabrirla
     * debe mostrar Historial sin ningún filtro activo, no el filtro que tenía antes de limpiarlo. */
    fun onClearFilters() {
        _filterState.value = HistorialFilterState()
        persistFilterState()
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
        private val filterRepository: HistorialFilterRepository,
        private val appContext: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HistorialViewModel(
                operationDao,
                operationImageDao,
                catalogItemDao,
                filterRepository,
                deleteImageFiles = { filePath ->
                    File(appContext.filesDir, filePath).delete()
                    File(appContext.filesDir, ImageProcessor.thumbnailPathFor(filePath)).delete()
                }
            ) as T
        }
    }
}
