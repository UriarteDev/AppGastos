package com.example.myapplication.db.repository

import com.example.myapplication.db.dao.MovimientoDao
import com.example.myapplication.db.entities.Movimiento

class MovimientoRepository(private val dao: MovimientoDao) {

    fun obtenerMovimientos(usuarioId: Long) = dao.obtenerPorUsuario(usuarioId)

    suspend fun insertarMovimiento(movimiento: Movimiento) {
        dao.insertar(movimiento)
    }
}
