package com.smartsaldo.app.db.repository

import androidx.lifecycle.LiveData
import com.smartsaldo.app.db.dao.GastoDao
import com.smartsaldo.app.db.entities.Gasto

class GastoRepository(private val gastoDao: GastoDao) {

    suspend fun insert(gasto: Gasto) {
        gastoDao.insert(gasto)
    }

    fun getGastosUsuario(usuarioId: Long): LiveData<List<Gasto>> {
        return gastoDao.getGastosUsuario(usuarioId)
    }

    fun getTotalGastos(usuarioId: Long): LiveData<Float?> {
        return gastoDao.getTotalGastos(usuarioId)
    }

    suspend fun delete(gasto: Gasto) {
        gastoDao.delete(gasto)
    }
}