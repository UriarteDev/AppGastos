package com.example.myapplication.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.myapplication.db.AppDatabase
import com.example.myapplication.db.entities.Movimiento
import com.example.myapplication.db.repository.MovimientoRepository
import kotlinx.coroutines.launch
import androidx.lifecycle.*

class MovimientoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MovimientoRepository

    private val _usuarioId = MutableLiveData<Long>()
    val movimientos: LiveData<List<Movimiento>> = _usuarioId.switchMap {
        repository.obtenerMovimientos(it)
    }

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
}

