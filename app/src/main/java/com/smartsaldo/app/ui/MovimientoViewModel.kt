package com.smartsaldo.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.smartsaldo.app.db.AppDatabase
import com.smartsaldo.app.db.entities.Movimiento
import com.smartsaldo.app.db.repository.MovimientoRepository
import kotlinx.coroutines.launch
import androidx.lifecycle.*

class MovimientoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MovimientoRepository
    private val _usuarioId = MutableLiveData<Long>()

    val movimientos: LiveData<List<Movimiento>> = _usuarioId.switchMap {
        repository.obtenerMovimientos(it).asLiveData()
    }

    // ✅ LiveData solo de gastos
    val gastos: LiveData<List<Movimiento>> = _usuarioId.switchMap {
        repository.obtenerGastos(it).asLiveData()
    }

    private val _totales = MutableLiveData<Map<String, Float>>()
    val totales: LiveData<Map<String, Float>> = _totales

    init {
        val dao = AppDatabase.getDatabase(application).movimientoDao()
        repository = MovimientoRepository(dao)
    }

    fun cargarMovimientos(usuarioId: Long) {
        _usuarioId.value = usuarioId
    }

    fun insertarMovimiento(movimiento: Movimiento) {
        viewModelScope.launch {
            repository.insertarMovimiento(movimiento)
        }
    }

    fun eliminarMovimiento(movimiento: Movimiento) {
        viewModelScope.launch {
            repository.eliminarMovimiento(movimiento)
        }
    }

    fun cargarTotales(usuarioId: Long) {
        viewModelScope.launch {
            repository.obtenerMovimientos(usuarioId).collect { listaMovimientos: List<Movimiento> ->
                val totales = mutableMapOf(
                    "INGRESO" to 0f,
                    "GASTO_FIJO" to 0f,
                    "AHORRO" to 0f,
                    "GASTO" to 0f,
                    "SALDO" to 0f
                )

                listaMovimientos.forEach { mov: Movimiento ->
                    when (mov.tipo) {
                        "INGRESO" -> totales["INGRESO"] = totales["INGRESO"]!! + mov.monto
                        "GASTO_FIJO" -> totales["GASTO_FIJO"] = totales["GASTO_FIJO"]!! + mov.monto
                        "AHORRO" -> totales["AHORRO"] = totales["AHORRO"]!! + mov.monto
                        "GASTO" -> totales["GASTO"] = totales["GASTO"]!! + mov.monto
                    }
                }

                // ⚡ saldo disponible = ingresos - gastos fijos - ahorro - gastos
                totales["SALDO"] =
                    (totales["INGRESO"] ?: 0f) -
                            (totales["GASTO_FIJO"] ?: 0f) -
                            (totales["AHORRO"] ?: 0f) -
                            (totales["GASTO"] ?: 0f)

                _totales.postValue(totales)
            }
        }
    }
}
