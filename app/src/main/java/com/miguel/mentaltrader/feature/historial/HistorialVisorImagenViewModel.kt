package com.miguel.mentaltrader.feature.historial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.miguel.mentaltrader.core.data.OperationImage
import com.miguel.mentaltrader.core.data.OperationImageDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * HU-019: imágenes reales de una operación para el visor a pantalla completa. Reutiliza
 * [OperationImageDao.getByOperationId] -- la MISMA fuente real ya usada por
 * [HistorialDetalleViewModel] (que muestra la primera imagen en el detalle) y por la tarjeta
 * compacta del listado (HU-015 Escenario 3), ordenada por `position ASC`, para que el índice
 * inicial recibido desde `HistorialDetalleScreen` (siempre 0, la única imagen que hoy se muestra
 * ahí) sea consistente con lo que el usuario ya vio.
 */
class HistorialVisorImagenViewModel(
    operationId: Long,
    operationImageDao: OperationImageDao
) : ViewModel() {

    val images: StateFlow<List<OperationImage>> = operationImageDao.getByOperationId(operationId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    class Factory(
        private val operationId: Long,
        private val operationImageDao: OperationImageDao
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HistorialVisorImagenViewModel(operationId, operationImageDao) as T
        }
    }
}
