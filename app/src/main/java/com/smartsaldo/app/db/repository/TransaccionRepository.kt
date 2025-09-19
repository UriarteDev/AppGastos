package com.smartsaldo.app.db.repository

import com.smartsaldo.app.db.dao.EstadisticaMensual
import com.smartsaldo.app.db.dao.TotalPorCategoria
import com.smartsaldo.app.db.dao.TransaccionDao
import com.smartsaldo.app.db.entities.*
import kotlinx.coroutines.flow.Flow
import java.util.*

class TransaccionRepository(private val dao: TransaccionDao) {

    fun getTransacciones(usuarioId: String): Flow<List<TransaccionConCategoria>> {
        return dao.getTransaccionesConCategoria(usuarioId)
    }

    fun getTransaccionesDelMes(usuarioId: String, year: Int, month: Int): Flow<List<TransaccionConCategoria>> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val inicioMes = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val finMes = calendar.timeInMillis

        return dao.getTransaccionesPorFecha(usuarioId, inicioMes, finMes)
    }

    fun buscarTransacciones(usuarioId: String, busqueda: String): Flow<List<TransaccionConCategoria>> {
        return dao.buscarTransacciones(usuarioId, busqueda)
    }

    fun getTransaccionesPorCategoria(usuarioId: String, categoriaId: Long): Flow<List<TransaccionConCategoria>> {
        return dao.getTransaccionesPorCategoria(usuarioId, categoriaId)
    }

    suspend fun insertarTransaccion(transaccion: Transaccion): Long {
        return dao.insertTransaccion(transaccion)
    }

    suspend fun actualizarTransaccion(transaccion: Transaccion) {
        val transaccionActualizada = transaccion.copy(updatedAt = System.currentTimeMillis())
        dao.updateTransaccion(transaccionActualizada)
    }

    suspend fun eliminarTransaccion(transaccion: Transaccion) {
        dao.deleteTransaccion(transaccion)
    }

    suspend fun getTransaccionPorId(id: Long): Transaccion? {
        return dao.getTransaccionPorId(id)
    }

    fun getEstadisticasMensuales(usuarioId: String): Flow<List<EstadisticaMensual>> {
        return dao.getEstadisticasMensuales(usuarioId)
    }

    fun getTotalPorCategoria(
        usuarioId: String,
        year: Int,
        month: Int,
        tipo: TipoTransaccion
    ): Flow<List<TotalPorCategoria>> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val inicioMes = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val finMes = calendar.timeInMillis

        return dao.getTotalPorCategoria(usuarioId, inicioMes, finMes, tipo)
    }
}