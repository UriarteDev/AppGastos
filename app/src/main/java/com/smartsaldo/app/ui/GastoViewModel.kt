package com.smartsaldo.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.smartsaldo.app.db.AppDatabase
import com.smartsaldo.app.db.entities.Gasto
import com.smartsaldo.app.db.repository.GastoRepository
import kotlinx.coroutines.launch

class GastoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GastoRepository
    val totalGastos: LiveData<Float?>

    private val usuarioId = 1L // ⚡ manejar usuario real si lo implementas

    init {
        val gastoDao = AppDatabase.getDatabase(application).gastoDao()
        repository = GastoRepository(gastoDao)
        totalGastos = repository.getTotalGastos(usuarioId)
    }

    fun insertarGasto(nombre: String, precio: Float) {
        viewModelScope.launch {
            val gasto = Gasto(
                nombre = nombre,
                precio = precio,
                usuarioId = usuarioId
            )
            repository.insert(gasto)
        }
    }

    fun obtenerGastos(): LiveData<List<Gasto>> {
        return repository.getGastosUsuario(usuarioId)
    }
}