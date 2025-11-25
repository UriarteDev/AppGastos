package com.smartsaldo.app.ui.categorias

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsaldo.app.data.local.entities.Categoria
import com.smartsaldo.app.data.repository.CategoriaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoriaViewModel @Inject constructor(
    private val categoriaRepository: CategoriaRepository
) : ViewModel() {

    private val _usuarioId = MutableStateFlow<String?>(null)

    private val _uiState = MutableStateFlow(CategoriaUiState())
    val uiState: StateFlow<CategoriaUiState> = _uiState

    val categorias: StateFlow<List<Categoria>> = _usuarioId.flatMapLatest { usuarioId ->
        if (usuarioId != null) {
            categoriaRepository.getCategorias(usuarioId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setUsuarioId(usuarioId: String) {
        _usuarioId.value = usuarioId
    }

    fun crearCategoria(categoria: Categoria) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                categoriaRepository.insertarCategoria(categoria)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "categoria_creada"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "error_crear_categoria"
                )
            }
        }
    }

    fun actualizarCategoria(categoria: Categoria) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                categoriaRepository.actualizarCategoria(categoria)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "categoria_actualizada"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "error_actualizar_categoria"
                )
            }
        }
    }

    fun eliminarCategoria(categoria: Categoria) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                categoriaRepository.eliminarCategoria(categoria)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "categoria_eliminada"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "error_eliminar_categoria"
                )
            }
        }
    }

    fun limpiarMensaje() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }
}

data class CategoriaUiState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null
)