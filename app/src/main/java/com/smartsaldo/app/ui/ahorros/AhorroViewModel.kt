package com.smartsaldo.app.ui.ahorros

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsaldo.app.data.local.entities.Ahorro
import com.smartsaldo.app.data.local.entities.AporteAhorro
import com.smartsaldo.app.data.repository.AhorroRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AhorroViewModel @Inject constructor(
    private val ahorroRepository: AhorroRepository
) : ViewModel() {

    private val _usuarioId = MutableStateFlow<String?>(null)

    private val _uiState = MutableStateFlow(AhorroUiState())
    val uiState: StateFlow<AhorroUiState> = _uiState

    val ahorros: StateFlow<List<Ahorro>> = _usuarioId.flatMapLatest { usuarioId ->
        if (usuarioId != null) {
            ahorroRepository.getAhorros(usuarioId)
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

    fun crearAhorro(nombre: String, metaMonto: Double) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val usuarioId = _usuarioId.value ?: return@launch

                val ahorro = Ahorro(
                    nombre = nombre,
                    metaMonto = metaMonto,
                    usuarioId = usuarioId
                )

                ahorroRepository.insertarAhorro(ahorro)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Meta de ahorro creada ✅"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error al crear ahorro"
                )
            }
        }
    }

    fun agregarAporte(ahorroId: Long, monto: Double, nota: String?, usuarioId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                val aporte = AporteAhorro(
                    ahorroId = ahorroId,
                    monto = monto,
                    nota = nota,
                    usuarioId = usuarioId
                )

                ahorroRepository.agregarAporte(aporte)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Aporte registrado ✅"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error al agregar aporte"
                )
            }
        }
    }

    fun eliminarAhorro(ahorro: Ahorro) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                ahorroRepository.eliminarAhorro(ahorro)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Meta de ahorro eliminada"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error al eliminar"
                )
            }
        }
    }

    fun limpiarMensaje() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }
}

data class AhorroUiState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null
)