package com.example.myapplication.db.repository

import androidx.lifecycle.LiveData
import com.example.myapplication.db.dao.FinanzasDao
import com.example.myapplication.db.entities.Finanzas

class FinanzasRepository(private val dao: FinanzasDao) {
    val ultimaFinanza: LiveData<Finanzas?> = dao.getUltimaFinanza()

    suspend fun insert(finanzas: Finanzas) {
        dao.insert(finanzas)
    }

    suspend fun update(finanzas: Finanzas) {
        dao.update(finanzas)
    }

    suspend fun delete(finanzas: Finanzas) {
        dao.delete(finanzas)
    }

    data class TotalPorTipo(
        val tipo: String,
        val total: Float
    )

}
