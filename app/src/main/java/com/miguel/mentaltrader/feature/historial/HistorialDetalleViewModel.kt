package com.miguel.mentaltrader.feature.historial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.miguel.mentaltrader.core.data.CatalogItemDao
import com.miguel.mentaltrader.core.data.Operation
import com.miguel.mentaltrader.core.data.OperationDao
import com.miguel.mentaltrader.core.data.OperationImage
import com.miguel.mentaltrader.core.data.OperationImageDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** HU-018: estado de la vista de detalle -- la operación, sus imágenes, y los nombres de
 * catálogo ya resueltos (para no repetir el join Activo/Emoción/Error en la UI). */
data class HistorialDetalleUiState(
    val operation: Operation? = null,
    val images: List<OperationImage> = emptyList(),
    val assetName: String? = null,
    val emotionBeforeName: String? = null,
    val emotionAfterName: String? = null,
    val errorName: String? = null,
    val isLoading: Boolean = true
)

class HistorialDetalleViewModel(
    operationId: Long,
    private val operationDao: OperationDao,
    private val operationImageDao: OperationImageDao,
    private val catalogItemDao: CatalogItemDao
) : ViewModel() {

    private val _state = MutableStateFlow(HistorialDetalleUiState())
    val state: StateFlow<HistorialDetalleUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val operation = operationDao.getById(operationId)
            if (operation == null) {
                _state.value = _state.value.copy(isLoading = false)
                return@launch
            }
            val images = operationImageDao.getByOperationId(operationId).first()
            _state.value = HistorialDetalleUiState(
                operation = operation,
                images = images,
                assetName = catalogItemDao.getById(operation.assetId)?.name,
                emotionBeforeName = catalogItemDao.getById(operation.emotionBeforeId)?.name,
                emotionAfterName = catalogItemDao.getById(operation.emotionAfterId)?.name,
                errorName = catalogItemDao.getById(operation.errorId)?.name,
                isLoading = false
            )
        }
    }

    class Factory(
        private val operationId: Long,
        private val operationDao: OperationDao,
        private val operationImageDao: OperationImageDao,
        private val catalogItemDao: CatalogItemDao
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HistorialDetalleViewModel(operationId, operationDao, operationImageDao, catalogItemDao) as T
        }
    }
}
