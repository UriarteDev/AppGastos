package com.smartsaldo.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsaldo.app.db.entities.*
import com.smartsaldo.app.db.repository.TransaccionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class TransaccionViewModel(
    private val transaccionRepository: TransaccionRepository,
    private val categoriaRepository: CategoriaRepository
) : ViewModel() {

    private val _usuarioId = MutableStateFlow<String?>(null)
    private val _filtroTexto = MutableStateFlow("")
    private val _categoriaSeleccionada = MutableStateFlow<Long?>(null)
    private val _mesSeleccionado = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH))
    private val _anoSeleccionado = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))

    // ✅ Estado de UI
    private val _uiState = MutableStateFlow(TransaccionUiState())
    val uiState: StateFlow<TransaccionUiState> = _uiState

    // ✅ Transacciones filtradas
    val transacciones: StateFlow<List<TransaccionConCategoria>> = combine(
        _usuarioId,
        _filtroTexto,
        _categoriaSeleccionada,
        _mesSeleccionado,
        _anoSeleccionado
    ) { usuarioId, filtro, categoria, mes, ano ->
        if (usuarioId == null) return@combine emptyList()

        when {
            filtro.isNotBlank() -> {
                transaccionRepository.buscarTransacciones(usuarioId, filtro)
            }
            categoria != null -> {
                transaccionRepository.getTransaccionesPorCategoria(usuarioId, categoria)
            }
            else -> {
                transaccionRepository.getTransaccionesDelMes(usuarioId, ano, mes)
            }
        }
    }.flatMapLatest { flow ->
        flow
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // ✅ Categorías disponibles
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

    // ✅ Estadísticas del mes actual
    val estadisticasDelMes: StateFlow<EstadisticaMensual?> = combine(
        _usuarioId,
        _mesSeleccionado,
        _anoSeleccionado
    ) { usuarioId, mes, ano ->
        if (usuarioId != null) {
            transaccionRepository.getTransaccionesDelMes(usuarioId, ano, mes)
                .map { transacciones ->
                    val ingresos = transacciones.filter { it.transaccion.tipo == TipoTransaccion.INGRESO }
                        .sumOf { it.transaccion.monto }
                    val gastos = transacciones.filter { it.transaccion.tipo == TipoTransaccion.GASTO }
                        .sumOf { it.transaccion.monto }

                    EstadisticaMensual(
                        mes = "${ano}-${mes + 1}",
                        totalIngresos = ingresos,
                        totalGastos = gastos,
                        totalTransacciones = transacciones.size
                    )
                }
        } else {
            flowOf(null)
        }
    }.flatMapLatest { flow ->
        flow
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun setUsuarioId(usuarioId: String) {
        _usuarioId.value = usuarioId
    }

    fun buscar(texto: String) {
        _filtroTexto.value = texto
        _categoriaSeleccionada.value = null // limpiar filtro de categoría
    }

    fun filtrarPorCategoria(categoriaId: Long?) {
        _categoriaSeleccionada.value = categoriaId
        _filtroTexto.value = "" // limpiar búsqueda de texto
    }

    fun cambiarMes(mes: Int, ano: Int) {
        _mesSeleccionado.value = mes
        _anoSeleccionado.value = ano
        _filtroTexto.value = "" // limpiar filtros
        _categoriaSeleccionada.value = null
    }

    fun agregarTransaccion(
        monto: Double,
        descripcion: String,
        notas: String?,
        fecha: Long,
        categoriaId: Long,
        tipo: TipoTransaccion
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                val usuarioId = _usuarioId.value ?: return@launch

                val transaccion = Transaccion(
                    monto = monto,
                    descripcion = descripcion,
                    notas = notas,
                    fecha = fecha,
                    categoriaId = categoriaId,
                    usuarioId = usuarioId,
                    tipo = tipo
                )

                transaccionRepository.insertarTransaccion(transaccion)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Transacción guardada ✅"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error al guardar transacción"
                )
            }
        }
    }

    fun editarTransaccion(
        id: Long,
        monto: Double,
        descripcion: String,
        notas: String?,
        fecha: Long,
        categoriaId: Long,
        tipo: TipoTransaccion
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                val usuarioId = _usuarioId.value ?: return@launch
                val transaccionActual = transaccionRepository.getTransaccionPorId(id) ?: return@launch

                val transaccionActualizada = transaccionActual.copy(
                    monto = monto,
                    descripcion = descripcion,
                    notas = notas,
                    fecha = fecha,
                    categoriaId = categoriaId,
                    tipo = tipo
                )

                transaccionRepository.actualizarTransaccion(transaccionActualizada)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Transacción actualizada ✅"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error al actualizar transacción"
                )
            }
        }
    }

    fun eliminarTransaccion(transaccion: Transaccion) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                transaccionRepository.eliminarTransaccion(transaccion)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Transacción eliminada ✅"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error al eliminar transacción"
                )
            }
        }
    }

    fun limpiarMensaje() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }
}

// ===== UI STATE =====
data class TransaccionUiState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null
)