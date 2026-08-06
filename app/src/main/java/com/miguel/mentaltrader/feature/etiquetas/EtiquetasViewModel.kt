package com.miguel.mentaltrader.feature.etiquetas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.miguel.mentaltrader.core.data.CatalogAddResult
import com.miguel.mentaltrader.core.data.CatalogDeleteResult
import com.miguel.mentaltrader.core.data.CatalogItem
import com.miguel.mentaltrader.core.data.CatalogItemDao
import com.miguel.mentaltrader.core.data.CatalogRepository
import com.miguel.mentaltrader.core.data.CatalogUpdateResult
import com.miguel.mentaltrader.core.data.OperationImageDao
import com.miguel.mentaltrader.core.data.OperationUsageSummary
import com.miguel.mentaltrader.core.image.DiskSpaceCalculator
import com.miguel.mentaltrader.core.image.ImageProcessor
import com.miguel.mentaltrader.core.model.CatalogType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/** Fix: fila de [EtiquetasUiState.pendingDeleteUsagePreview] -- [OperationUsageSummary] ya
 * resuelto a un nombre de activo mostrable (el DAO solo conoce `assetId`, ver KDoc de
 * [OperationUsageSummary]). */
data class UsagePreviewEntry(val dateTime: Long, val assetName: String)

data class EtiquetasUiState(
    val selectedType: CatalogType = CatalogType.ASSET,
    val addFieldValue: String = "",
    val addError: String? = null,
    val editingItem: CatalogItem? = null,
    val editFieldValue: String = "",
    val editError: String? = null,
    val pendingDeleteItem: CatalogItem? = null,
    /** HU-012: cantidad de operaciones que usan [pendingDeleteItem], para mostrar la advertencia
     * con conteo en el diálogo de confirmación. Null mientras se está consultando o si no hay
     * ninguna eliminación pendiente. Fix: cuando es > 0, `EtiquetasScreen` ya no ofrece "Eliminar"
     * -- muestra directamente el detalle de [pendingDeleteUsagePreview] (el borrado está
     * bloqueado, ver `CatalogRepository.deleteItem`). */
    val pendingDeleteUsageCount: Int? = null,
    /** Fix: vista previa (fecha + activo) de las operaciones que usan [pendingDeleteItem] cuando
     * [pendingDeleteUsageCount] es mayor a 0 -- vacía mientras no hace falta o mientras se está
     * consultando. */
    val pendingDeleteUsagePreview: List<UsagePreviewEntry> = emptyList(),
    val blockedDeleteMessage: String? = null,
    /** HU-014: espacio total aproximado (bytes) ocupado por las imágenes + miniaturas ya
     * guardadas. 0 por defecto (Escenario 2: sin imágenes todavía), se recalcula una vez al
     * abrir la pestaña (no en tiempo real). */
    val diskSpaceBytes: Long = 0L
) {
    companion object {
        /** Formatea a KB (sin decimales) o MB (1 decimal) para presentación -- cálculo
         * aproximado, no exacto al byte (spec de HU-014). Locale.US fijo para no depender del
         * separador decimal regional del dispositivo (mismo cuidado que el parseo de HU-004). */
        fun formatDiskSpaceKbMb(bytes: Long): String {
            val kb = bytes / 1024.0
            return if (kb < 1024) {
                String.format(Locale.US, "%.0f KB", kb)
            } else {
                String.format(Locale.US, "%.1f MB", kb / 1024.0)
            }
        }
    }
}

class EtiquetasViewModel(
    private val catalogItemDao: CatalogItemDao,
    private val catalogRepository: CatalogRepository,
    private val operationImageDao: OperationImageDao,
    private val filesDir: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _state = MutableStateFlow(EtiquetasUiState())
    val state: StateFlow<EtiquetasUiState> = _state.asStateFlow()

    init {
        // HU-014: se calcula una sola vez al abrir la pestaña (no en tiempo real), reutilizando
        // las rutas ya guardadas por ImageProcessor (imagen + miniatura) sin duplicar esa lógica.
        viewModelScope.launch {
            val images = operationImageDao.getAll()
            val paths = images.flatMap { listOf(it.filePath, ImageProcessor.thumbnailPathFor(it.filePath)) }
            val bytes = withContext(ioDispatcher) { DiskSpaceCalculator.totalBytes(filesDir, paths) }
            _state.value = _state.value.copy(diskSpaceBytes = bytes)
        }
    }

    val assets: StateFlow<List<CatalogItem>> =
        catalogItemDao.getByType(CatalogType.ASSET).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val emotions: StateFlow<List<CatalogItem>> =
        catalogItemDao.getByType(CatalogType.EMOTION).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val errors: StateFlow<List<CatalogItem>> =
        catalogItemDao.getByType(CatalogType.ERROR).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun onTypeSelected(type: CatalogType) {
        _state.value = _state.value.copy(selectedType = type, addFieldValue = "", addError = null)
    }

    fun onAddFieldChange(value: String) {
        _state.value = _state.value.copy(addFieldValue = value, addError = null)
    }

    fun onConfirmAdd() {
        val type = _state.value.selectedType
        val name = _state.value.addFieldValue
        viewModelScope.launch {
            when (catalogRepository.addItem(type, name)) {
                is CatalogAddResult.Added -> _state.value = _state.value.copy(addFieldValue = "", addError = null)
                CatalogAddResult.Duplicate ->
                    _state.value = _state.value.copy(addError = CatalogRepository.DUPLICATE_MESSAGE)
                // Bug real reportado por el usuario (2026-08-04): el campo vacío creaba un
                // elemento de catálogo vacío, sin ningún aviso.
                CatalogAddResult.Blank ->
                    _state.value = _state.value.copy(addError = CatalogRepository.BLANK_MESSAGE)
            }
        }
    }

    fun onStartEdit(item: CatalogItem) {
        _state.value = _state.value.copy(editingItem = item, editFieldValue = item.name, editError = null)
    }

    fun onEditFieldChange(value: String) {
        _state.value = _state.value.copy(editFieldValue = value, editError = null)
    }

    fun onCancelEdit() {
        _state.value = _state.value.copy(editingItem = null, editFieldValue = "", editError = null)
    }

    fun onConfirmEdit() {
        val item = _state.value.editingItem ?: return
        val newName = _state.value.editFieldValue
        viewModelScope.launch {
            when (catalogRepository.updateItem(item, newName)) {
                CatalogUpdateResult.Updated ->
                    _state.value = _state.value.copy(editingItem = null, editFieldValue = "", editError = null)
                CatalogUpdateResult.Duplicate ->
                    _state.value = _state.value.copy(editError = CatalogRepository.DUPLICATE_MESSAGE)
            }
        }
    }

    fun onRequestDelete(item: CatalogItem) {
        _state.value = _state.value.copy(
            pendingDeleteItem = item,
            pendingDeleteUsageCount = null,
            pendingDeleteUsagePreview = emptyList()
        )
        // HU-012 Escenario 1: consulta el conteo de uso para enriquecer el diálogo de confirmación
        // ya existente (HU-011). Fix: cuando el conteo es > 0 también trae la vista previa (fecha +
        // activo) -- `EtiquetasScreen` la usa para mostrar directamente que el borrado está
        // bloqueado, en vez del diálogo de confirmar/cancelar.
        viewModelScope.launch {
            val count = catalogRepository.usageCountOf(item)
            val preview = if (count > 0) usagePreviewFor(item) else emptyList()
            if (_state.value.pendingDeleteItem?.id == item.id) {
                _state.value = _state.value.copy(pendingDeleteUsageCount = count, pendingDeleteUsagePreview = preview)
            }
        }
    }

    private suspend fun usagePreviewFor(item: CatalogItem): List<UsagePreviewEntry> {
        val summaries = catalogRepository.usagePreviewOf(item)
        val assetNames = assets.value.associate { it.id to it.name }
        return summaries.map { UsagePreviewEntry(dateTime = it.dateTime, assetName = assetNames[it.assetId] ?: "—") }
    }

    fun onCancelDelete() {
        _state.value = _state.value.copy(
            pendingDeleteItem = null,
            pendingDeleteUsageCount = null,
            pendingDeleteUsagePreview = emptyList()
        )
    }

    fun onConfirmDelete() {
        val item = _state.value.pendingDeleteItem ?: return
        viewModelScope.launch {
            when (val result = catalogRepository.deleteItem(item)) {
                CatalogDeleteResult.Deleted ->
                    _state.value = _state.value.copy(
                        pendingDeleteItem = null,
                        pendingDeleteUsageCount = null,
                        pendingDeleteUsagePreview = emptyList()
                    )
                is CatalogDeleteResult.Blocked ->
                    _state.value = _state.value.copy(
                        pendingDeleteItem = null,
                        pendingDeleteUsageCount = null,
                        pendingDeleteUsagePreview = emptyList(),
                        blockedDeleteMessage = result.reason
                    )
            }
        }
    }

    fun onDismissBlockedDeleteMessage() {
        _state.value = _state.value.copy(blockedDeleteMessage = null)
    }

    class Factory(
        private val catalogItemDao: CatalogItemDao,
        private val catalogRepository: CatalogRepository,
        private val operationImageDao: OperationImageDao,
        private val filesDir: File
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EtiquetasViewModel(catalogItemDao, catalogRepository, operationImageDao, filesDir) as T
        }
    }
}
