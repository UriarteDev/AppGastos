package com.smartsaldo.app.db.repository

import com.smartsaldo.app.db.dao.MovimientoDao
import com.smartsaldo.app.db.entities.Movimiento
import kotlinx.coroutines.flow.Flow

class MovimientoRepository(private val dao: MovimientoDao) {

    suspend fun insertarMovimiento(movimiento: Movimiento) {
        dao.insertarMovimiento(movimiento)
    }

    fun obtenerMovimientos(usuarioId: Long): Flow<List<Movimiento>> {
        return dao.obtenerMovimientos(usuarioId)
    }

    fun obtenerGastos(usuarioId: Long): Flow<List<Movimiento>> {
        return dao.obtenerGastos(usuarioId)
    }

    suspend fun eliminarMovimiento(movimiento: Movimiento) {
        dao.eliminarMovimiento(movimiento)
    }
}
