package com.miguel.mentaltrader.feature.ajustes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.miguel.mentaltrader.core.data.DataResetService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AjustesUiState(
    val isDeleting: Boolean = false,
    val deleted: Boolean = false
)

class AjustesViewModel(private val dataResetService: DataResetService) : ViewModel() {

    private val _state = MutableStateFlow(AjustesUiState())
    val state: StateFlow<AjustesUiState> = _state.asStateFlow()

    fun deleteAllOperationsAndImages() {
        _state.value = _state.value.copy(isDeleting = true)
        viewModelScope.launch {
            dataResetService.deleteAllOperationsAndImages()
            _state.value = AjustesUiState(isDeleting = false, deleted = true)
        }
    }

    class Factory(private val dataResetService: DataResetService) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AjustesViewModel(dataResetService) as T
        }
    }
}
