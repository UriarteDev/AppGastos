package com.smartsaldo.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.smartsaldo.app.db.AppDatabase
import com.smartsaldo.app.db.entities.Finanzas
import com.smartsaldo.app.db.repository.FinanzasRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinanzasRepository
    val ultimaFinanza: LiveData<Finanzas?>
    private var usuarioId: Long = 0L

    init {
        val dao = AppDatabase.getDatabase(application).finanzasDao()
        repository = FinanzasRepository(dao)
        ultimaFinanza = repository.ultimaFinanza

        // obtenemos el usuario por defecto en background
        viewModelScope.launch {
            usuarioId = withContext(Dispatchers.IO) {
                val userDao = AppDatabase.getDatabase(getApplication()).usuarioDao()
                userDao.getOrCreateDefault().id
            }
        }
    }

    fun guardarFinanza(saldo: Float, gastoFijo: Float, ahorro: Float) {
        viewModelScope.launch {
            if (usuarioId != 0L) {
                repository.insert(
                    Finanzas(
                        saldo = saldo,
                        gastoFijo = gastoFijo,
                        ahorro = ahorro,
                        usuarioId = usuarioId
                    )
                )
            }
        }
    }
}