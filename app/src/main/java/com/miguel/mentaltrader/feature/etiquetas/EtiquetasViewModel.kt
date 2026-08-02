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
import com.miguel.mentaltrader.core.model.CatalogType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EtiquetasUiState(
    val selectedType: CatalogType = CatalogType.ASSET,
    val addFieldValue: String = "",
    val addError: String? = null,
    val editingItem: CatalogItem? = null,
    val editFieldValue: String = "",
    val editError: String? = null,
    val pendingDeleteItem: CatalogItem? = null,
    val blockedDeleteMessage: String? = null
)

class EtiquetasViewModel(
    private val catalogItemDao: CatalogItemDao,
    private val catalogRepository: CatalogRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EtiquetasUiState())
    val state: StateFlow<EtiquetasUiState> = _state.asStateFlow()

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
        _state.value = _state.value.copy(pendingDeleteItem = item)
    }

    fun onCancelDelete() {
        _state.value = _state.value.copy(pendingDeleteItem = null)
    }

    fun onConfirmDelete() {
        val item = _state.value.pendingDeleteItem ?: return
        viewModelScope.launch {
            when (val result = catalogRepository.deleteItem(item)) {
                CatalogDeleteResult.Deleted ->
                    _state.value = _state.value.copy(pendingDeleteItem = null)
                is CatalogDeleteResult.Blocked ->
                    _state.value = _state.value.copy(pendingDeleteItem = null, blockedDeleteMessage = result.reason)
            }
        }
    }

    fun onDismissBlockedDeleteMessage() {
        _state.value = _state.value.copy(blockedDeleteMessage = null)
    }

    class Factory(
        private val catalogItemDao: CatalogItemDao,
        private val catalogRepository: CatalogRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EtiquetasViewModel(catalogItemDao, catalogRepository) as T
        }
    }
}
